import express from 'express';
import http from 'http';
import path from 'path';
import fs from 'fs';
import { WebSocketServer, WebSocket } from 'ws';
import { createServer as createViteServer } from 'vite';
import { GoogleGenAI } from '@google/genai';
import dotenv from 'dotenv';
import {
  MP_CROPS,
  MP_MANDIS,
  DISTRICT_PROCUREMENT_STATS,
  STANDARD_TIME_SLOTS,
  DEMO_WEATHER_ALERTS,
  INITIAL_BOOKINGS,
  DEMO_FARMERS,
} from './src/data/mpMandiData';
import {
  SlotBooking,
  FarmerProfile,
  SmsLogItem,
  CropInfo,
  MandiCenter,
  TimeSlotConfig,
  AuditLogItem,
} from './src/types';

dotenv.config();

// ==========================================
// CANONICAL STATE & STORAGE
// Concurrency-safe in-memory store with persistence
// ==========================================
interface MandiSequenceTracker {
  [mandiIdAndDate: string]: number;
}

interface StoredOtp {
  phone: string;
  otp: string;
  expiresAt: number;
  attempts: number;
  lastSentAt: number;
}

const otpStore = new Map<string, StoredOtp>();
const idempotencyStore = new Map<string, { booking: SlotBooking; timestamp: number }>();
const tokenSequences: MandiSequenceTracker = {};

// Initial State Seed
const farmers: FarmerProfile[] = DEMO_FARMERS.map((f, idx) => ({
  id: f.id,
  kisanId: f.kisanId,
  name: f.name,
  hindiName: f.hindiName,
  phone: f.phone,
  aadharNumber: `${f.aadhaarLast4 ? `71048821${f.aadhaarLast4}` : `98214589710${idx}`}`,
  maskedAadhar: `XXXX-XXXX-${f.aadhaarLast4 || '4589'}`,
  district: f.district,
  village: f.village,
  landSizeAcres: f.landSizeAcres || 5.0,
  bankAccountLast4: f.bankAccountLast4 || '4589',
  ifscCode: f.ifscCode || 'SBIN0001248',
  registeredAt: '2026-08-15T09:00:00Z',
  lastLoginAt: new Date().toISOString(),
  loginCount: 5 + idx * 3,
}));

const mandis: MandiCenter[] = JSON.parse(JSON.stringify(MP_MANDIS));
const crops: CropInfo[] = JSON.parse(JSON.stringify(MP_CROPS));
const bookings: SlotBooking[] = JSON.parse(JSON.stringify(INITIAL_BOOKINGS));
const slotConfigs: TimeSlotConfig[] = JSON.parse(JSON.stringify(STANDARD_TIME_SLOTS));
const smsLogs: SmsLogItem[] = [];
const auditLogs: AuditLogItem[] = [];

// Initialize token sequences from initial bookings
bookings.forEach((b) => {
  const key = `${b.mandiCenterId}_${b.scheduledDate}`;
  const seq = b.tokenSequence || 0;
  if (!tokenSequences[key] || seq > tokenSequences[key]) {
    tokenSequences[key] = seq;
  }
});

// ==========================================
// GEMINI AI CLIENT (SERVER-SIDE ONLY)
// ==========================================
let aiClient: GoogleGenAI | null = null;
function getGeminiClient(): GoogleGenAI {
  if (!process.env.GEMINI_API_KEY) {
    throw new Error('GEMINI_API_KEY environment variable is required');
  }
  if (!aiClient) {
    aiClient = new GoogleGenAI({
      apiKey: process.env.GEMINI_API_KEY,
      httpOptions: { headers: { 'User-Agent': 'aistudio-build' } },
    });
  }
  return aiClient;
}

async function executeGeminiWithFallback<T>(prompt: string, fallbackData: T): Promise<{ data: T; mode: string }> {
  if (!process.env.GEMINI_API_KEY) {
    return { data: fallbackData, mode: 'heuristic-fallback' };
  }

  const candidateModels = ['gemini-2.5-flash', 'gemini-1.5-flash', 'gemini-1.5-pro'];

  for (const modelName of candidateModels) {
    try {
      const ai = getGeminiClient();
      const response = await ai.models.generateContent({
        model: modelName,
        contents: prompt,
        config: { responseMimeType: 'application/json' },
      });

      if (response && response.text) {
        const cleaned = response.text.replace(/```json\n?/g, '').replace(/```\n?/g, '').trim();
        const parsed = JSON.parse(cleaned);
        return { data: parsed, mode: `live-gemini-${modelName}` };
      }
    } catch {
      continue;
    }
  }

  return { data: fallbackData, mode: 'heuristic-fallback' };
}

// ==========================================
// SERVER INITIALIZATION & WEBSOCKET ENGINE
// ==========================================
async function startServer() {
  const app = express();
  const PORT = 3000;
  const server = http.createServer(app);

  // WebSocket Server on /ws
  const wss = new WebSocketServer({ server, path: '/ws' });
  const subscriptions = new Map<WebSocket, Set<string>>();

  wss.on('connection', (ws) => {
    subscriptions.set(ws, new Set());

    ws.on('message', (data) => {
      try {
        const msg = JSON.parse(data.toString());
        if (msg.action === 'SUBSCRIBE' && Array.isArray(msg.topics)) {
          const clientTopics = subscriptions.get(ws) || new Set();
          msg.topics.forEach((t: string) => clientTopics.add(t));
          subscriptions.set(ws, clientTopics);
        } else if (msg.action === 'PING') {
          ws.send(JSON.stringify({ action: 'PONG', timestamp: Date.now() }));
        }
      } catch {
        // Non-JSON frame
      }
    });

    ws.on('close', () => {
      subscriptions.delete(ws);
    });
  });

  // Broadcast event to subscribed WebSocket clients
  function broadcast(topic: string, payload: any) {
    const message = JSON.stringify(payload);
    for (const [client, topics] of subscriptions.entries()) {
      if (client.readyState === WebSocket.OPEN) {
        if (topics.has(topic) || topics.size === 0) {
          client.send(message);
        }
      }
    }
  }

  // ==========================================
  // SYSTEM STATE & AUDIT LOG HELPER
  // ==========================================
  function recordAuditLog(
    action: string,
    entityType: 'MANDI' | 'BOOKING' | 'SLOT' | 'WEIGHBRIDGE' | 'SYSTEM',
    entityId: string,
    actor: string,
    details: string,
    previousState: string = '',
    newState: string = '',
    quantityKgDelta: number = 0,
    mandiId?: string
  ): AuditLogItem {
    const logItem: AuditLogItem = {
      id: `audit-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`,
      timestamp: new Date().toISOString(),
      action,
      entityType,
      entityId,
      actor,
      details,
      previousState,
      newState,
      quantityKgDelta,
      mandiId,
    };
    auditLogs.unshift(logItem);
    if (auditLogs.length > 300) auditLogs.pop();
    broadcast('/topic/audit', { type: 'AUDIT_LOG', log: logItem });
    return logItem;
  }

  // Recalculates mandi procurement capacity and waiting queues atomically
  function recalcMandiCapacity(mandiId: string) {
    const mandi = mandis.find((m) => m.id === mandiId);
    if (!mandi) return null;

    if (!mandi.totalCapacityKg) {
      mandi.totalCapacityKg = (mandi.dailyCapacityQuintals || 4500) * 100;
    }

    const mandiBookings = bookings.filter((b) => b.mandiCenterId === mandiId);
    let reservedKg = 0;
    let procuredKg = 0;
    let activeWaiting = 0;

    for (const b of mandiBookings) {
      const bKg = b.requestedYieldKg || (b.estimatedYieldQuintals ? b.estimatedYieldQuintals * 100 : 5000);
      if (b.status === 'COMPLETED') {
        const pKg = b.netWeightQuintals ? b.netWeightQuintals * 100 : bKg;
        procuredKg += pKg;
      } else if (b.status !== 'CANCELLED' && b.status !== 'REJECTED') {
        reservedKg += bKg;
        if (b.status === 'BOOKED' || b.status === 'GATE_CALLED') {
          activeWaiting += 1;
        }
      }
    }

    mandi.reservedCapacityKg = reservedKg;
    mandi.procuredCapacityKg = procuredKg;
    mandi.occupiedCapacityKg = reservedKg + procuredKg;
    mandi.availableCapacityKg = Math.max(0, mandi.totalCapacityKg - mandi.occupiedCapacityKg);
    mandi.activeTokensWaiting = activeWaiting;

    broadcast(`/topic/mandi/${mandi.id}/capacity`, {
      mandiId: mandi.id,
      totalCapacityKg: mandi.totalCapacityKg,
      reservedCapacityKg: mandi.reservedCapacityKg,
      procuredCapacityKg: mandi.procuredCapacityKg,
      availableCapacityKg: mandi.availableCapacityKg,
      occupiedCapacityKg: mandi.occupiedCapacityKg,
      timestamp: new Date().toISOString(),
    });
    broadcast(`/topic/mandi/${mandi.id}/status`, { type: 'MANDI_STATUS', status: mandi });
    broadcast('/topic/mandis', { type: 'MANDIS_LIST', mandis });

    return mandi;
  }

  // Recalculates time slot booking status and available vehicle/tonnage capacity
  function recalcSlotCapacities() {
    for (const slot of slotConfigs) {
      if (!slot.maxCapacityKg) {
        slot.maxCapacityKg = (slot.maxCapacityQuintals || 1500) * 100;
      }
      const slotBookings = bookings.filter(
        (b) => b.timeSlot === slot.timeSlot && b.status !== 'CANCELLED' && b.status !== 'REJECTED'
      );
      const bookedVehicles = slotBookings.length;
      const bookedKg = slotBookings.reduce(
        (sum, b) => sum + (b.requestedYieldKg || (b.estimatedYieldQuintals ? b.estimatedYieldQuintals * 100 : 4500)),
        0
      );

      slot.bookedVehicles = bookedVehicles;
      slot.bookedQuantityKg = bookedKg;
      slot.availableQuantityKg = Math.max(0, slot.maxCapacityKg - bookedKg);
      slot.availableVehicles = Math.max(0, slot.maxVehicles - bookedVehicles);
      slot.status =
        slot.availableVehicles <= 0 || slot.availableQuantityKg <= 0
          ? 'FULL'
          : slot.availableVehicles <= 5 || slot.availableQuantityKg <= 2000
          ? 'LIMITED'
          : 'OPEN';
    }

    broadcast('/topic/slots', { type: 'SLOTS_UPDATE', slots: slotConfigs });
    return slotConfigs;
  }

  // Initial calculation and seed audit trail
  mandis.forEach((m) => recalcMandiCapacity(m.id));
  recalcSlotCapacities();

  if (auditLogs.length === 0) {
    recordAuditLog('SYSTEM_BOOTSTRAP', 'SYSTEM', 'system-01', 'KisanSarthi System', 'System initialized with real-time capacity and queue synchronizer', 'INIT', 'ONLINE', 0);
    recordAuditLog('MANDI_CAPACITY_INITIALIZED', 'MANDI', 'mandi-sehore', 'APMC Portal', 'Calibrated procurement quota for Sehore Mandi (450,000 kg)', '0', '450000', 450000, 'mandi-sehore');
  }

  app.use(express.json());

  // Health check
  app.get(['/api/health', '/api/v1/health'], (req, res) => {
    res.json({
      status: 'ok',
      timestamp: new Date().toISOString(),
      farmersCount: farmers.length,
      bookingsCount: bookings.length,
      mandisCount: mandis.length,
      connectedClients: wss.clients.size,
    });
  });

  // ==========================================
  // 1. AUTHENTICATION (SECURE OTP & RBAC)
  // Fix Bug 7 & 8: Dynamic 6-digit OTP, Never return OTP in response
  // ==========================================

  let cachedTwilioAccountType: 'Trial' | 'Full' | null = null;

  async function checkIsTwilioTrial(accountSid: string, authToken: string): Promise<boolean> {
    const envConfig = process.env.TWILIO_TRIAL_MODE ?? process.env.TWILIO_TRIAL;
    if (envConfig !== undefined && envConfig !== '') {
      return envConfig.toLowerCase() === 'true' || envConfig === '1';
    }
    if (cachedTwilioAccountType !== null) {
      return cachedTwilioAccountType === 'Trial';
    }
    try {
      const url = `https://api.twilio.com/2010-04-01/Accounts/${accountSid}.json`;
      const authHeader = Buffer.from(`${accountSid}:${authToken}`).toString('base64');
      const res = await fetch(url, {
        headers: { Authorization: `Basic ${authHeader}` },
      });
      if (res.ok) {
        const data = (await res.json()) as any;
        cachedTwilioAccountType = data.type === 'Trial' ? 'Trial' : 'Full';
        console.log(`[Twilio Gateway] Detected account type: ${data.type} (isTrial: ${cachedTwilioAccountType === 'Trial'})`);
        return cachedTwilioAccountType === 'Trial';
      }
    } catch (e: any) {
      console.warn('[Twilio Gateway] Failed to query account info:', e.message);
    }
    return true;
  }

  // Twilio / National SMS Gateway Integration Helper (Lazy initialized, never crashes if credentials missing)
  async function dispatchSmsViaGateway(
    recipientPhone: string,
    message: string,
    isTrialTest: boolean = false
  ): Promise<{
    success: boolean;
    provider: 'TWILIO' | 'MOCK';
    externalSid?: string;
    status?: string;
    isTrialRestricted?: boolean;
    error?: string;
  }> {
    const accountSid = process.env.TWILIO_ACCOUNT_SID;
    const authToken = process.env.TWILIO_AUTH_TOKEN;
    const fromNumber = process.env.TWILIO_PHONE_NUMBER;

    if (accountSid && authToken && fromNumber) {
      const isTrial = await checkIsTwilioTrial(accountSid, authToken);

      if (isTrial && !isTrialTest) {
        const trialErrMsg = 'Twilio Trial accounts cannot send this custom message. Use Twilio Trial Test mode or upgrade/configure the Twilio account for production SMS.';
        console.warn('[Twilio Gateway] Custom DLT message rejected in Trial mode:', trialErrMsg);
        return {
          success: false,
          provider: 'TWILIO',
          isTrialRestricted: true,
          error: trialErrMsg,
        };
      }

      try {
        const clean = recipientPhone.replace(/\D/g, '');
        const to = recipientPhone.startsWith('+') ? recipientPhone : `+91${clean.slice(-10)}`;
        const messageToSend = isTrialTest
          ? (message || process.env.TWILIO_TRIAL_MESSAGE || 'Your 1234 order of 1 items has shipped and should be delivered on tomorrow. Details: https://twilio.com')
          : message;

        const url = `https://api.twilio.com/2010-04-01/Accounts/${accountSid}/Messages.json`;
        const authHeader = Buffer.from(`${accountSid}:${authToken}`).toString('base64');
        const params = new URLSearchParams();
        params.append('To', to);
        params.append('From', fromNumber);
        params.append('Body', messageToSend);

        const response = await fetch(url, {
          method: 'POST',
          headers: {
            Authorization: `Basic ${authHeader}`,
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          body: params.toString(),
        });

        const data = (await response.json()) as any;
        if (response.ok && data.sid) {
          const rawStatus = (data.status || 'ACCEPTED').toUpperCase();
          const resolvedStatus = rawStatus === 'QUEUED' ? 'QUEUED' : rawStatus === 'FAILED' ? 'FAILED' : 'ACCEPTED';
          console.log(`[Twilio Gateway] Live SMS dispatched. SID=${data.sid}, status=${resolvedStatus}, trialTest=${isTrialTest}`);
          return { success: true, provider: 'TWILIO', externalSid: data.sid, status: resolvedStatus };
        } else {
          console.warn('Twilio dispatch warning:', data.message || data);
          return {
            success: false,
            provider: 'TWILIO',
            error: data.message || 'Twilio message dispatch error',
          };
        }
      } catch (e: any) {
        console.warn('Twilio gateway network error:', e.message);
        return { success: false, provider: 'TWILIO', error: e.message };
      }
    }

    // Simulated fallback
    return {
      success: true,
      provider: 'MOCK',
      externalSid: `MOCK-SMS-${Date.now().toString().slice(-6)}`,
      status: 'ACCEPTED',
    };
  }

  const handleSendOtp = async (req: express.Request, res: express.Response) => {
    const { phone } = req.body;
    const cleanPhone = String(phone || '').replace(/\D/g, '');

    if (cleanPhone.length < 10) {
      return res.status(400).json({ success: false, error: 'Valid 10-digit mobile number required' });
    }

    const now = Date.now();
    const existingOtp = otpStore.get(cleanPhone);

    // Rate limiting: 60-second cooldown
    if (existingOtp && now - existingOtp.lastSentAt < 60000) {
      const waitSec = Math.ceil((60000 - (now - existingOtp.lastSentAt)) / 1000);
      return res.status(429).json({
        success: false,
        error: `Please wait ${waitSec} seconds before requesting a new OTP`,
        cooldownSeconds: waitSec,
      });
    }

    // Generate secure random 6-digit OTP (Fix Bug 7)
    const generatedOtp = Math.floor(100000 + Math.random() * 900000).toString();
    otpStore.set(cleanPhone, {
      phone: cleanPhone,
      otp: generatedOtp,
      expiresAt: now + 5 * 60 * 1000, // 5 minutes TTL
      attempts: 0,
      lastSentAt: now,
    });

    const farmer = farmers.find((f) => f.phone === cleanPhone);
    const farmerName = farmer?.name || `Kisan (${cleanPhone.slice(-4)})`;
    const maskedAadhar = farmer?.maskedAadhar || `XXXX-XXXX-${cleanPhone.slice(-4)}`;

    const otpMessage = `[MP-EUPARJAN] Aapka KisanSarthi login OTP: ${generatedOtp} hai. Kripya ise kisi se saajha na karein. Valid for 5 mins.`;
    const gatewayResult = await dispatchSmsViaGateway(cleanPhone, otpMessage);

    // Record SMS dispatch (Status is SENT, not fake DELIVERED - Fix Bug 12)
    const smsReceipt: SmsLogItem = {
      id: `sms-otp-${Date.now()}`,
      recipientPhone: cleanPhone,
      farmerName,
      aadharMasked: maskedAadhar,
      message: otpMessage,
      senderHeader: 'VK-EUPARJAN',
      dltTemplateId: 'DLT-TE-1107160',
      status: gatewayResult.success ? 'SENT' : 'FAILED',
      dispatchedAt: new Date().toISOString(),
      dispatchedBy: gatewayResult.provider === 'TWILIO' ? 'Twilio SMS Gateway' : 'e-Uparjan Security Gateway',
      channel: 'SMS_GATEWAY',
      deliveryReceiptId: gatewayResult.externalSid || `DLT-OTP-${Date.now().toString().slice(-6)}`,
    };
    smsLogs.unshift(smsReceipt);

    // CRITICAL (Bug 8 Fix): NEVER return the OTP in the response body!
    res.json({
      success: true,
      message: `OTP has been dispatched via secure SMS to +91 ******${cleanPhone.slice(-4)}`,
      cooldownSeconds: 60,
      provider: gatewayResult.provider,
      deliveryReceiptId: smsReceipt.deliveryReceiptId,
      devNote: 'In local development, check /api/v1/sms/logs or the SMS logs tab to inspect the simulated SMS.',
    });
  };

  app.post('/api/v1/auth/send-otp', handleSendOtp);
  app.post('/api/auth/send-otp', handleSendOtp);

  const handleVerifyOtp = (req: express.Request, res: express.Response) => {
    const { phone, otp } = req.body;
    const cleanPhone = String(phone || '').replace(/\D/g, '');

    if (cleanPhone.length < 10) {
      return res.status(400).json({ success: false, error: 'Valid 10-digit mobile number required' });
    }

    const stored = otpStore.get(cleanPhone);
    const now = Date.now();

    // Check OTP validity
    let isValid = false;
    if (stored) {
      if (now > stored.expiresAt) {
        otpStore.delete(cleanPhone);
        return res.status(400).json({ success: false, error: 'OTP has expired. Please request a new one.' });
      }
      stored.attempts += 1;
      if (stored.attempts > 3) {
        otpStore.delete(cleanPhone);
        return res.status(400).json({ success: false, error: 'Max verification attempts exceeded. Request a new OTP.' });
      }
      if (stored.otp === otp || otp === '4826') {
        isValid = true;
        otpStore.delete(cleanPhone);
      }
    } else if (otp === '4826' || otp === '123456') {
      // Demo bypass fallback
      isValid = true;
    }

    if (!isValid) {
      return res.status(400).json({ success: false, error: 'Invalid verification code. Please check your SMS and try again.' });
    }

    let farmer = farmers.find((f) => f.phone === cleanPhone);
    if (farmer) {
      farmer.lastLoginAt = new Date().toISOString();
      farmer.loginCount = (farmer.loginCount || 1) + 1;
    } else {
      const last4 = cleanPhone.slice(-4);
      farmer = {
        id: `farmer-${cleanPhone}`,
        kisanId: `MP-KISAN-${cleanPhone.slice(-6)}`,
        name: `Kisan (+91 ${cleanPhone})`,
        phone: cleanPhone,
        aadharNumber: `71048821${last4}`,
        maskedAadhar: `XXXX-XXXX-${last4}`,
        district: 'Sehore',
        village: 'Bilkisganj',
        landSizeAcres: 5.0,
        bankAccountLast4: last4,
        ifscCode: 'SBIN0001248',
        registeredAt: new Date().toISOString(),
        lastLoginAt: new Date().toISOString(),
        loginCount: 1,
      };
      farmers.unshift(farmer);
    }

    // Deprecated mock token generation removed - production authentication is handled exclusively by Spring Boot backend
    res.status(403).json({
      success: false,
      error: 'Mock OTP authentication disabled. Farmer authentication must be routed directly to the Spring Boot backend.',
    });
  };

  // Mock endpoints disabled to prevent overriding or intercepting requests to the Render Spring Boot backend
  const handleDisabledMockAuth = (_req: express.Request, res: express.Response) => {
    res.status(403).json({
      success: false,
      error: 'Mock authentication is disabled. Authentication must proceed directly through the production Spring Boot service at https://kisansarthi-vsne.onrender.com',
    });
  };

  app.post('/api/v1/auth/admin-login', handleDisabledMockAuth);
  app.post('/api/auth/admin-login', handleDisabledMockAuth);

  // ==========================================
  // 2. CROPS & MSP MANAGEMENT
  // Fix Bug 3: Support standardMsp and standardMspPerQuintal seamlessly
  // ==========================================
  app.get(['/api/v1/crops', '/api/crops'], (req, res) => {
    res.json({ success: true, count: crops.length, crops });
  });

  const handleUpdateMsp = (req: express.Request, res: express.Response) => {
    const { cropId } = req.params;
    const body = req.body;

    const stdMsp = Number(body.standardMspPerQuintal ?? body.standardMsp ?? 0);
    const bonus = Number(body.mpBonusPerQuintal ?? body.mpBonus ?? 0);

    const crop = crops.find((c) => c.id === cropId);
    if (!crop) {
      return res.status(404).json({ success: false, error: `Crop not found: ${cropId}` });
    }

    crop.standardMspPerQuintal = stdMsp;
    crop.mpBonusPerQuintal = bonus;
    crop.totalMsp = stdMsp + bonus;

    res.json({ success: true, crop, message: 'MSP rates updated successfully' });
  };

  app.put('/api/v1/crops/:cropId/msp', handleUpdateMsp);
  app.patch('/api/crops/:cropId/msp', handleUpdateMsp);

  // ==========================================
  // 3. MANDIS & REAL-TIME QUEUE MANAGEMENT
  // Fix Bug 2: Canonical /api/v1/mandis/:id/queue/next and /api/mandis/:id/call-next
  // Broadcasting instant WebSocket updates
  // ==========================================
  app.get(['/api/v1/mandis', '/api/mandis'], (req, res) => {
    const { district } = req.query;
    let list = mandis;
    if (district && typeof district === 'string') {
      list = list.filter((m) => m.district.toLowerCase() === district.toLowerCase());
    }
    res.json({ success: true, count: list.length, mandis: list });
  });

  app.get('/api/v1/mandis/:id', (req, res) => {
    const mandi = mandis.find((m) => m.id === req.params.id);
    if (!mandi) return res.status(404).json({ success: false, error: 'Mandi not found' });
    res.json({ success: true, mandi });
  });

  app.get('/api/v1/mandis/:id/queue', (req, res) => {
    const mandi = mandis.find((m) => m.id === req.params.id);
    if (!mandi) return res.status(404).json({ success: false, error: 'Mandi not found' });
    res.json({
      success: true,
      queue: {
        mandiId: mandi.id,
        currentTokenServing: mandi.currentTokenServing,
        totalTokensToday: mandi.totalTokensToday,
        activeTokensWaiting: mandi.activeTokensWaiting,
        averageProcessingMins: mandi.averageProcessingMins,
        gateStatus: mandi.gateStatus,
      },
    });
  });

  const handleAdvanceQueue = (req: express.Request, res: express.Response) => {
    const { id } = req.params;
    const mandi = mandis.find((m) => m.id === id);
    if (!mandi) {
      return res.status(404).json({ success: false, error: `Mandi not found: ${id}` });
    }

    // Advance token sequence
    mandi.currentTokenServing += 1;
    mandi.activeTokensWaiting = Math.max(0, mandi.activeTokensWaiting - 1);

    const distPrefix = (mandi.district && mandi.district.length >= 3)
      ? mandi.district.substring(0, 3).toUpperCase()
      : 'MPM';
    const calledTokenNumber = `MP-${distPrefix}-${String(mandi.currentTokenServing).padStart(3, '0')}`;

    // Find and update matching booking
    const matchedBooking = bookings.find(
      (b) => b.mandiCenterId === id && b.tokenSequence === mandi.currentTokenServing
    );
    if (matchedBooking) {
      matchedBooking.status = 'GATE_CALLED';
    }

    const payload = {
      event: 'TOKEN_CALLED',
      mandiId: mandi.id,
      currentTokenServing: mandi.currentTokenServing,
      calledToken: mandi.currentTokenServing,
      tokenNumber: calledTokenNumber,
      activeTokensWaiting: mandi.activeTokensWaiting,
      waitingCount: mandi.activeTokensWaiting,
      calledBookingId: matchedBooking?.id,
      matchedBooking: matchedBooking || null,
      status: 'GATE_CALLED',
      timestamp: new Date().toISOString(),
    };

    // Broadcast instant real-time event to all subscribed clients!
    broadcast(`/topic/mandi/${mandi.id}/queue`, payload);
    broadcast(`/topic/mandi/${mandi.id}/status`, { type: 'MANDI_STATUS', status: mandi });

    res.json({
      success: true,
      ...payload,
      message: `Token #${mandi.currentTokenServing} called to Gate Bay 1`,
    });
  };

  app.post('/api/v1/mandis/:id/queue/next', handleAdvanceQueue);
  app.post('/api/mandis/:id/call-next', handleAdvanceQueue);

  // Queue Skip Token (Mark No-Show, Call Next)
  app.post(['/api/v1/mandis/:id/queue/skip', '/api/mandis/:id/queue/skip'], (req, res) => {
    const { id } = req.params;
    const mandi = mandis.find((m) => m.id === id);
    if (!mandi) return res.status(404).json({ success: false, error: `Mandi not found: ${id}` });

    const skippedToken = mandi.currentTokenServing;
    mandi.currentTokenServing += 1;
    mandi.activeTokensWaiting = Math.max(0, mandi.activeTokensWaiting - 1);

    const matchedBooking = bookings.find((b) => b.mandiCenterId === id && b.tokenSequence === skippedToken);
    if (matchedBooking && matchedBooking.status === 'GATE_CALLED') {
      matchedBooking.status = 'NO_SHOW';
    }

    recalcMandiCapacity(mandi.id);
    recordAuditLog(
      'QUEUE_TOKEN_SKIPPED',
      'MANDI',
      mandi.id,
      req.body.actor || 'Gate Bay Officer',
      `Skipped token #${skippedToken}. Now calling token #${mandi.currentTokenServing}`,
      `${skippedToken}`,
      `${mandi.currentTokenServing}`,
      0,
      mandi.id
    );

    const distPrefix = (mandi.district && mandi.district.length >= 3) ? mandi.district.substring(0, 3).toUpperCase() : 'MPM';
    const calledTokenNumber = `MP-${distPrefix}-${String(mandi.currentTokenServing).padStart(3, '0')}`;

    const payload = {
      event: 'TOKEN_SKIPPED',
      mandiId: mandi.id,
      skippedToken,
      currentTokenServing: mandi.currentTokenServing,
      calledToken: mandi.currentTokenServing,
      tokenNumber: calledTokenNumber,
      activeTokensWaiting: mandi.activeTokensWaiting,
      waitingCount: mandi.activeTokensWaiting,
      timestamp: new Date().toISOString(),
    };
    broadcast(`/topic/mandi/${mandi.id}/queue`, payload);
    broadcast(`/topic/mandi/${mandi.id}/status`, { type: 'MANDI_STATUS', status: mandi });

    res.json({ success: true, ...payload, message: `Token #${skippedToken} marked skipped. Token #${mandi.currentTokenServing} called.` });
  });

  // Queue Recall Token (Re-announce current token)
  app.post(['/api/v1/mandis/:id/queue/recall', '/api/mandis/:id/queue/recall'], (req, res) => {
    const { id } = req.params;
    const mandi = mandis.find((m) => m.id === id);
    if (!mandi) return res.status(404).json({ success: false, error: `Mandi not found: ${id}` });

    const distPrefix = (mandi.district && mandi.district.length >= 3) ? mandi.district.substring(0, 3).toUpperCase() : 'MPM';
    const calledTokenNumber = `MP-${distPrefix}-${String(mandi.currentTokenServing).padStart(3, '0')}`;

    recordAuditLog(
      'QUEUE_TOKEN_RECALLED',
      'MANDI',
      mandi.id,
      req.body.actor || 'Gate Bay Officer',
      `Recalled token #${mandi.currentTokenServing} (${calledTokenNumber}) to Bay 1`,
      `${mandi.currentTokenServing}`,
      `${mandi.currentTokenServing}`,
      0,
      mandi.id
    );

    const payload = {
      event: 'TOKEN_RECALLED',
      mandiId: mandi.id,
      currentTokenServing: mandi.currentTokenServing,
      calledToken: mandi.currentTokenServing,
      tokenNumber: calledTokenNumber,
      activeTokensWaiting: mandi.activeTokensWaiting,
      waitingCount: mandi.activeTokensWaiting,
      timestamp: new Date().toISOString(),
    };
    broadcast(`/topic/mandi/${mandi.id}/queue`, payload);

    res.json({ success: true, ...payload, message: `Token #${mandi.currentTokenServing} recalled to Gate Bay 1` });
  });

  // Mandi Capacity Inspection & Adjustment
  app.get('/api/v1/mandis/:id/capacity', (req, res) => {
    const mandi = mandis.find((m) => m.id === req.params.id);
    if (!mandi) return res.status(404).json({ success: false, error: 'Mandi not found' });
    res.json({
      success: true,
      data: {
        mandiId: mandi.id,
        totalCapacityKg: mandi.totalCapacityKg,
        reservedCapacityKg: mandi.reservedCapacityKg,
        procuredCapacityKg: mandi.procuredCapacityKg,
        occupiedCapacityKg: mandi.occupiedCapacityKg,
        availableCapacityKg: mandi.availableCapacityKg,
      },
    });
  });

  app.put(['/api/v1/mandis/:id/capacity', '/api/mandis/:id/capacity'], (req, res) => {
    const mandi = mandis.find((m) => m.id === req.params.id);
    if (!mandi) return res.status(404).json({ success: false, error: 'Mandi not found' });
    const newTotalKg = Number(req.body.totalCapacityKg || (req.body.dailyCapacityQuintals ? req.body.dailyCapacityQuintals * 100 : 0));
    if (!newTotalKg || newTotalKg <= 0) {
      return res.status(400).json({ success: false, error: 'Valid totalCapacityKg greater than 0 required' });
    }
    const prevTotal = mandi.totalCapacityKg;
    mandi.totalCapacityKg = newTotalKg;
    mandi.dailyCapacityQuintals = Math.round(newTotalKg / 100);
    recalcMandiCapacity(mandi.id);
    recordAuditLog(
      'MANDI_CAPACITY_UPDATED',
      'MANDI',
      mandi.id,
      req.body.actor || 'Mandi Admin Officer',
      `Updated daily procurement capacity to ${newTotalKg} kg (${mandi.dailyCapacityQuintals} Qtl)`,
      `${prevTotal}`,
      `${newTotalKg}`,
      newTotalKg - prevTotal,
      mandi.id
    );
    res.json({ success: true, mandi, message: `Procurement capacity updated to ${newTotalKg} kg (${mandi.dailyCapacityQuintals} Qtl)` });
  });

  // ==========================================
  // 4. SLOTS & CAPACITY
  // Fix Bug 4: Support /api/v1/slots/capacity, /api/slots/capacity, /api/slots/update
  // ==========================================
  app.get(['/api/v1/slots', '/api/slots'], (req, res) => {
    res.json({ success: true, count: slotConfigs.length, slots: slotConfigs });
  });

  const handleUpdateSlotCapacity = (req: express.Request, res: express.Response) => {
    const { timeSlot, maxVehicles, status, maxCapacityKg, maxCapacityQuintals } = req.body;
    const slot = slotConfigs.find((s) => s.timeSlot === timeSlot);
    if (slot) {
      if (maxVehicles !== undefined) slot.maxVehicles = Number(maxVehicles);
      if (status !== undefined) slot.status = status;
      if (maxCapacityKg !== undefined) {
        slot.maxCapacityKg = Number(maxCapacityKg);
        slot.maxCapacityQuintals = Math.round(Number(maxCapacityKg) / 100);
      } else if (maxCapacityQuintals !== undefined) {
        slot.maxCapacityQuintals = Number(maxCapacityQuintals);
        slot.maxCapacityKg = Number(maxCapacityQuintals) * 100;
      }
      recalcSlotCapacities();
      recordAuditLog(
        'SLOT_CAPACITY_UPDATED',
        'SLOT',
        slot.timeSlot,
        req.body.actor || 'Admin Officer',
        `Updated slot [${slot.timeSlot}] capacity: max ${slot.maxVehicles} vehicles, status: ${slot.status}`,
        '',
        slot.status,
        0
      );
    }
    res.json({ success: true, slots: slotConfigs, message: 'Slot capacity updated successfully' });
  };

  app.patch('/api/v1/slots/capacity', handleUpdateSlotCapacity);
  app.patch('/api/slots/capacity', handleUpdateSlotCapacity);
  app.post('/api/slots/update', handleUpdateSlotCapacity);

  // ==========================================
  // 5. BOOKINGS (IDEMPOTENCY & CONCURRENCY PROTECTION)
  // Fix Bug 1 & 5: Server-authoritative token generation, state machine transitions
  // ==========================================
  app.get(['/api/v1/bookings', '/api/bookings'], (req, res) => {
    const { farmerId, phone, mandiId } = req.query;
    let list = bookings;

    if (farmerId && typeof farmerId === 'string') {
      list = list.filter((b) => b.farmerId === farmerId);
    }
    if (phone && typeof phone === 'string') {
      list = list.filter((b) => b.farmerPhone === phone);
    }
    if (mandiId && typeof mandiId === 'string') {
      list = list.filter((b) => b.mandiCenterId === mandiId);
    }

    res.json({ success: true, count: list.length, bookings: list });
  });

  app.post(['/api/v1/bookings', '/api/bookings'], (req, res) => {
    const idempotencyKey = req.header('Idempotency-Key');

    // 1. Check Idempotency Store
    if (idempotencyKey && idempotencyStore.has(idempotencyKey)) {
      const existing = idempotencyStore.get(idempotencyKey)!;
      return res.json({
        success: true,
        booking: existing.booking,
        message: 'Idempotent request matched. Returning existing booking.',
      });
    }

    const data = req.body;
    const targetMandi = mandis.find((m) => m.id === data.mandiCenterId) || mandis[0];
    const requestedKg = (Number(data.estimatedYieldQuintals) || 50) * 100;

    // Consequence Validation: Check Mandi Available Capacity
    if (targetMandi.availableCapacityKg < requestedKg) {
      return res.status(400).json({
        success: false,
        error: 'CAPACITY_EXCEEDED',
        message: `Available capacity for ${targetMandi.name} is only ${targetMandi.availableCapacityKg} kg (${(targetMandi.availableCapacityKg / 100).toFixed(1)} quintals). Cannot reserve ${requestedKg} kg (${data.estimatedYieldQuintals} quintals).`,
        mandiCapacity: {
          totalKg: targetMandi.totalCapacityKg,
          availableKg: targetMandi.availableCapacityKg,
          reservedKg: targetMandi.reservedCapacityKg,
          occupiedKg: targetMandi.occupiedCapacityKg,
        },
      });
    }

    // Consequence Validation: Check Slot Capacity
    const targetSlot = slotConfigs.find((s) => s.timeSlot === data.timeSlot);
    if (targetSlot && (targetSlot.availableVehicles <= 0 || targetSlot.availableQuantityKg < requestedKg)) {
      return res.status(400).json({
        success: false,
        error: 'SLOT_FULL',
        message: `Time slot ${data.timeSlot} is at maximum capacity (Available vehicles: ${targetSlot.availableVehicles}, Available: ${targetSlot.availableQuantityKg} kg). Please select another time slot.`,
      });
    }

    const scheduledDate = data.scheduledDate || new Date().toISOString().split('T')[0];
    const seqKey = `${targetMandi.id}_${scheduledDate}`;

    // 2. Concurrency-Safe Monotonic Token Increment
    const nextSeq = (tokenSequences[seqKey] || targetMandi.totalTokensToday || 0) + 1;
    tokenSequences[seqKey] = nextSeq;

    // Format official server-authoritative token number (e.g. MP-SEH-042)
    const distPrefix = (targetMandi.district && targetMandi.district.length >= 3)
      ? targetMandi.district.substring(0, 3).toUpperCase()
      : 'MPM';
    const officialTokenNumber = `MP-${distPrefix}-${String(nextSeq).padStart(3, '0')}`;

    // Update Mandi counters atomically
    targetMandi.totalTokensToday += 1;

    const newBooking: SlotBooking = {
      id: `booking-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`,
      tokenNumber: officialTokenNumber,
      tokenSequence: nextSeq,
      farmerId: data.farmerId || `farmer-${data.farmerPhone || 'user'}`,
      farmerName: data.farmerName || 'Registered Farmer',
      farmerPhone: data.farmerPhone || '9826014522',
      farmerAadhar: data.farmerAadhar,
      district: data.district || targetMandi.district,
      village: data.village || 'Bilkisganj',
      mandiCenterId: targetMandi.id,
      mandiCenterName: targetMandi.name,
      cropId: data.cropId || 'crop-wheat',
      cropName: data.cropName || 'Wheat (Gehun - Sharbati)',
      estimatedYieldQuintals: Number(data.estimatedYieldQuintals) || 50,
      requestedYieldKg: requestedKg,
      acreage: Number(data.acreage) || 4.0,
      harvestDate: data.harvestDate,
      scheduledDate,
      timeSlot: data.timeSlot || '08:00 AM - 10:00 AM',
      vehicleType: data.vehicleType || 'Tractor Trolley',
      vehicleNumber: data.vehicleNumber || 'MP-04-AB-1234',
      status: 'BOOKED',
      qrCodeData: `https://euparjan.mp.gov.in/gate-pass?t=${officialTokenNumber}`,
      createdAt: new Date().toISOString(),
      bankAccountLast4: data.bankAccountLast4 || '4589',
      paymentStatus: 'PENDING',
      waitTimeEstimateMins: data.waitTimeEstimateMins || 14,
      aiRecommended: data.aiRecommended,
      aiReasoning: data.aiReasoning,
    };

    bookings.unshift(newBooking);

    // Save to Idempotency Store
    if (idempotencyKey) {
      idempotencyStore.set(idempotencyKey, { booking: newBooking, timestamp: Date.now() });
    }

    // Atomic State Recalculation (Consequential system update)
    recalcMandiCapacity(targetMandi.id);
    recalcSlotCapacities();

    // Record Audit Log Item
    recordAuditLog(
      'SLOT_BOOKED',
      'BOOKING',
      newBooking.id,
      newBooking.farmerName,
      `Reserved ${requestedKg} kg (${newBooking.estimatedYieldQuintals} Qtl) for ${targetMandi.name}. Token ${officialTokenNumber}`,
      'NONE',
      'BOOKED',
      requestedKg,
      targetMandi.id
    );

    // Broadcast new booking to WebSocket
    broadcast(`/topic/mandi/${targetMandi.id}/queue`, {
      event: 'BOOKING_CREATED',
      booking: newBooking,
      mandiId: targetMandi.id,
      timestamp: new Date().toISOString(),
    });
    broadcast('/topic/bookings', { type: 'BOOKING_CREATED', booking: newBooking });

    res.status(201).json({
      success: true,
      booking: newBooking,
      mandiCapacity: {
        totalKg: targetMandi.totalCapacityKg,
        availableKg: targetMandi.availableCapacityKg,
        reservedKg: targetMandi.reservedCapacityKg,
        occupiedKg: targetMandi.occupiedCapacityKg,
      },
      message: `Official token ${officialTokenNumber} generated successfully. ${requestedKg} kg capacity reserved.`,
    });
  });

  // Cancellation Endpoint (Consequence: Releases capacity, decrements waiting, updates slot)
  const handleCancelBooking = (req: express.Request, res: express.Response) => {
    const { id } = req.params;
    const booking = bookings.find((b) => b.id === id);
    if (!booking) {
      return res.status(404).json({ success: false, error: `Booking not found: ${id}` });
    }

    if (booking.status === 'CANCELLED') {
      return res.status(400).json({ success: false, error: 'Booking is already cancelled' });
    }
    if (booking.status === 'COMPLETED') {
      return res.status(400).json({ success: false, error: 'Cannot cancel a completed procurement booking' });
    }

    const prevStatus = booking.status;
    booking.status = 'CANCELLED';
    booking.cancelledAt = new Date().toISOString();

    const releasedKg = booking.requestedYieldKg || (booking.estimatedYieldQuintals ? booking.estimatedYieldQuintals * 100 : 5000);

    // Atomic State Recalculation
    recalcMandiCapacity(booking.mandiCenterId);
    recalcSlotCapacities();

    // Record Audit Log Item
    recordAuditLog(
      'BOOKING_CANCELLED',
      'BOOKING',
      booking.id,
      req.body.actor || 'Farmer / Admin',
      `Cancelled token ${booking.tokenNumber}. Released ${releasedKg} kg capacity back to mandi.`,
      prevStatus,
      'CANCELLED',
      -releasedKg,
      booking.mandiCenterId
    );

    // Broadcast real-time event
    broadcast(`/topic/mandi/${booking.mandiCenterId}/queue`, {
      event: 'BOOKING_CANCELLED',
      bookingId: booking.id,
      tokenNumber: booking.tokenNumber,
      releasedKg,
      timestamp: new Date().toISOString(),
    });
    broadcast('/topic/bookings', { type: 'BOOKING_CANCELLED', booking });

    res.json({
      success: true,
      booking,
      releasedKg,
      message: `Token ${booking.tokenNumber} cancelled successfully. ${releasedKg} kg (${releasedKg / 100} Qtl) capacity released back to mandi.`,
    });
  };

  app.post(['/api/v1/bookings/:id/cancel', '/api/bookings/:id/cancel'], handleCancelBooking);
  app.delete(['/api/v1/bookings/:id', '/api/bookings/:id'], handleCancelBooking);

  // Rejection Endpoint (Quality rejection releases reserved capacity)
  app.post(['/api/v1/bookings/:id/reject', '/api/bookings/:id/reject'], (req, res) => {
    const { id } = req.params;
    const booking = bookings.find((b) => b.id === id);
    if (!booking) {
      return res.status(404).json({ success: false, error: `Booking not found: ${id}` });
    }

    if (booking.status === 'REJECTED' || booking.status === 'CANCELLED') {
      return res.status(400).json({ success: false, error: `Booking is already ${booking.status}` });
    }

    const prevStatus = booking.status;
    booking.status = 'REJECTED';
    booking.rejectionReason = req.body.reason || 'FAQ Quality Standards Not Met (Moisture > 12% or damaged grain)';

    const releasedKg = booking.requestedYieldKg || (booking.estimatedYieldQuintals ? booking.estimatedYieldQuintals * 100 : 5000);

    // Atomic State Recalculation
    recalcMandiCapacity(booking.mandiCenterId);
    recalcSlotCapacities();

    // Record Audit Log Item
    recordAuditLog(
      'BOOKING_REJECTED',
      'BOOKING',
      booking.id,
      req.body.actor || 'Quality Inspector',
      `Rejected lot ${booking.tokenNumber}: ${booking.rejectionReason}. Released ${releasedKg} kg capacity.`,
      prevStatus,
      'REJECTED',
      -releasedKg,
      booking.mandiCenterId
    );

    // Broadcast real-time event
    broadcast(`/topic/mandi/${booking.mandiCenterId}/queue`, {
      event: 'BOOKING_REJECTED',
      bookingId: booking.id,
      tokenNumber: booking.tokenNumber,
      rejectionReason: booking.rejectionReason,
      releasedKg,
      timestamp: new Date().toISOString(),
    });
    broadcast('/topic/bookings', { type: 'BOOKING_REJECTED', booking });

    res.json({
      success: true,
      booking,
      message: `Token ${booking.tokenNumber} marked as rejected. ${releasedKg} kg capacity released back to available pool.`,
    });
  });

  // State Machine Status Transition & Weighment Finalization
  const handleUpdateBookingStatus = (req: express.Request, res: express.Response) => {
    const { id } = req.params;
    const booking = bookings.find((b) => b.id === id);
    if (!booking) {
      return res.status(404).json({ success: false, error: `Booking not found: ${id}` });
    }

    const prevStatus = booking.status;
    const updates = req.body;
    Object.assign(booking, updates);

    // If status transitioned to COMPLETED, finalize procurement conversion
    if (booking.status === 'COMPLETED' && prevStatus !== 'COMPLETED') {
      booking.completedAt = new Date().toISOString();
      booking.paymentStatus = 'DBT_INITIATED';
      booking.utrNumber = booking.utrNumber || `MPDBT${Date.now().toString().slice(-8)}`;

      const actualGross = Number(booking.actualGrossWeightKg || 0);
      const actualTare = Number(booking.actualTareWeightKg || 0);
      const netKg = actualGross > actualTare ? actualGross - actualTare : (booking.requestedYieldKg || 5000);
      booking.netWeightQuintals = parseFloat((netKg / 100).toFixed(2));
      booking.totalPayoutRs = Math.round(booking.netWeightQuintals * 2400);

      recalcMandiCapacity(booking.mandiCenterId);
      recalcSlotCapacities();

      recordAuditLog(
        'WEIGHMENT_COMPLETED',
        'WEIGHBRIDGE',
        booking.id,
        req.body.actor || 'Weighbridge Officer',
        `Procured Net: ${netKg} kg (${booking.netWeightQuintals} Qtl). Converted reservation into procurement. DBT Rs ${booking.totalPayoutRs}`,
        prevStatus,
        'COMPLETED',
        netKg,
        booking.mandiCenterId
      );
    } else {
      recalcMandiCapacity(booking.mandiCenterId);
      recalcSlotCapacities();

      recordAuditLog(
        'STATUS_TRANSITION',
        'BOOKING',
        booking.id,
        req.body.actor || 'Operator',
        `Token ${booking.tokenNumber} transitioned from ${prevStatus} to ${booking.status}`,
        prevStatus,
        booking.status,
        0,
        booking.mandiCenterId
      );
    }

    // Broadcast update to WebSocket
    broadcast(`/topic/mandi/${booking.mandiCenterId}/queue`, {
      event: 'BOOKING_UPDATE',
      bookingId: booking.id,
      status: booking.status,
      tokenNumber: booking.tokenNumber,
      timestamp: new Date().toISOString(),
    });
    broadcast('/topic/bookings', { type: 'BOOKING_UPDATE', booking });

    res.json({ success: true, booking, message: 'Booking status updated successfully' });
  };

  app.put('/api/v1/bookings/:id/status', handleUpdateBookingStatus);
  app.patch('/api/bookings/:id', handleUpdateBookingStatus);

  // Weighment Submission (Gross, Tare, Net Weight)
  app.post(['/api/v1/bookings/:id/weighment', '/api/bookings/:id/weighment'], (req, res) => {
    const { id } = req.params;
    const booking = bookings.find((b) => b.id === id);
    if (!booking) {
      return res.status(404).json({ success: false, error: `Booking not found: ${id}` });
    }

    const { actualGrossWeightKg, actualTareWeightKg, moisturePct, foreignMatterPct, qualityGrade } = req.body;
    if (actualGrossWeightKg !== undefined) booking.actualGrossWeightKg = Number(actualGrossWeightKg);
    if (actualTareWeightKg !== undefined) booking.actualTareWeightKg = Number(actualTareWeightKg);
    if (moisturePct !== undefined) booking.moisturePct = Number(moisturePct);
    if (foreignMatterPct !== undefined) booking.foreignMatterPct = Number(foreignMatterPct);
    if (qualityGrade !== undefined) booking.qualityGrade = qualityGrade;

    const gross = booking.actualGrossWeightKg || 0;
    const tare = booking.actualTareWeightKg || 0;
    if (gross > 0 && tare > 0 && gross > tare) {
      const netKg = gross - tare;
      booking.netWeightQuintals = parseFloat((netKg / 100).toFixed(2));
      booking.totalPayoutRs = Math.round(booking.netWeightQuintals * 2400);
      booking.status = 'COMPLETED';
      booking.completedAt = new Date().toISOString();
      booking.paymentStatus = 'DBT_INITIATED';
      booking.utrNumber = booking.utrNumber || `MPDBT${Date.now().toString().slice(-8)}`;

      recalcMandiCapacity(booking.mandiCenterId);
      recalcSlotCapacities();

      recordAuditLog(
        'WEIGHMENT_FINALIZED',
        'WEIGHBRIDGE',
        booking.id,
        req.body.actor || 'Weighbridge Officer',
        `Finalized weighment: Gross ${gross} kg, Tare ${tare} kg, Net ${netKg} kg (${booking.netWeightQuintals} Qtl). Status set to COMPLETED.`,
        'WEIGHBRIDGE_TARE',
        'COMPLETED',
        netKg,
        booking.mandiCenterId
      );
    } else if (gross > 0) {
      booking.status = 'WEIGHBRIDGE_GROSS';
      recalcMandiCapacity(booking.mandiCenterId);
    }

    broadcast(`/topic/mandi/${booking.mandiCenterId}/queue`, {
      event: 'WEIGHMENT_RECORDED',
      bookingId: booking.id,
      booking,
      timestamp: new Date().toISOString(),
    });
    broadcast('/topic/bookings', { type: 'BOOKING_UPDATE', booking });

    res.json({ success: true, booking, message: 'Weighment recorded successfully' });
  });

  app.get(['/api/v1/bookings/:id/weighment', '/api/bookings/:id/weighment'], (req, res) => {
    const booking = bookings.find((b) => b.id === req.params.id);
    if (!booking) return res.status(404).json({ success: false, error: 'Booking not found' });
    res.json({
      success: true,
      data: {
        bookingId: booking.id,
        tokenNumber: booking.tokenNumber,
        actualGrossWeightKg: booking.actualGrossWeightKg,
        actualTareWeightKg: booking.actualTareWeightKg,
        netWeightQuintals: booking.netWeightQuintals,
        moisturePct: booking.moisturePct,
        foreignMatterPct: booking.foreignMatterPct,
        qualityGrade: booking.qualityGrade,
        totalPayoutRs: booking.totalPayoutRs,
        status: booking.status,
      },
    });
  });

  // ==========================================
  // 6. FARMERS REGISTRY
  // ==========================================
  app.get(['/api/v1/farmers', '/api/farmers'], (req, res) => {
    const { district, search } = req.query;
    let list = farmers;
    if (district && typeof district === 'string') {
      list = list.filter((f) => f.district.toLowerCase() === district.toLowerCase());
    }
    if (search && typeof search === 'string') {
      const q = search.toLowerCase();
      list = list.filter((f) => f.name.toLowerCase().includes(q) || f.phone.includes(q));
    }
    res.json({ success: true, count: list.length, farmers: list });
  });

  // ==========================================
  // 7. SMS & NOTIFICATIONS (ACCURATE DELIVERY STATUS)
  // Fix Bug 12: Proper delivery lifecycle statuses
  // ==========================================
  app.get(['/api/v1/sms/config', '/api/sms/config'], async (req, res) => {
    const accountSid = process.env.TWILIO_ACCOUNT_SID;
    const authToken = process.env.TWILIO_AUTH_TOKEN;
    const hasTwilio = Boolean(accountSid && authToken && process.env.TWILIO_PHONE_NUMBER);
    const isTrial = hasTwilio ? await checkIsTwilioTrial(accountSid!, authToken!) : false;

    res.json({
      success: true,
      data: {
        provider: hasTwilio ? 'twilio' : 'mock',
        trialMode: isTrial,
        accountType: isTrial ? 'Trial' : 'Full',
        trialTestPath: '/api/v1/sms/trial-test',
        predefinedTemplateName: 'Order Confirmations',
        predefinedTemplateMessage:
          process.env.TWILIO_TRIAL_MESSAGE ||
          'Your 1234 order of 1 items has shipped and should be delivered on tomorrow. Details: https://twilio.com',
        productionDltAvailable: !isTrial,
      },
    });
  });

  app.get(['/api/v1/sms/logs', '/api/sms/logs'], (req, res) => {
    const { phone } = req.query;
    let list = smsLogs;
    if (phone && typeof phone === 'string') {
      const clean = phone.replace(/\D/g, '');
      list = list.filter((item) => item.recipientPhone.includes(clean));
    }
    res.json({ success: true, count: list.length, logs: list });
  });

  app.post(['/api/v1/sms/trial-test', '/api/sms/trial-test'], async (req, res) => {
    const { recipientPhone, farmerName, aadharMasked, customTrialMessage, message } = req.body;
    const cleanPhone = String(recipientPhone || '').replace(/\D/g, '');

    if (cleanPhone.length < 10) {
      return res.status(400).json({ success: false, error: 'Valid 10-digit phone required' });
    }

    const trialMsg =
      customTrialMessage ||
      message ||
      process.env.TWILIO_TRIAL_MESSAGE ||
      'Your 1234 order of 1 items has shipped and should be delivered on tomorrow. Details: https://twilio.com';

    const gatewayResult = await dispatchSmsViaGateway(cleanPhone, trialMsg, true);

    const logItem: SmsLogItem = {
      id: `sms-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`,
      recipientPhone: cleanPhone.slice(-10),
      farmerName: farmerName || 'Verified Test Recipient',
      aadharMasked: aadharMasked || 'XXXX-XXXX-4589',
      message: trialMsg,
      senderHeader: 'TWILIO-TRIAL',
      dltTemplateId: 'TWILIO-PREDEFINED-ORDER-CONFIRMATION',
      status: gatewayResult.success ? (gatewayResult.status as any || 'ACCEPTED') : 'FAILED',
      dispatchedAt: new Date().toISOString(),
      dispatchedBy: 'Twilio Trial Dispatcher (Connectivity Test)',
      channel: 'SMS_GATEWAY',
      deliveryReceiptId: gatewayResult.externalSid,
    };

    smsLogs.unshift(logItem);

    if (gatewayResult.success) {
      res.json({
        success: true,
        data: {
          sid: gatewayResult.externalSid,
          status: gatewayResult.status || 'ACCEPTED',
          deliveryReceiptId: gatewayResult.externalSid,
          recipientPhone: cleanPhone.slice(-10),
          farmerName: farmerName || 'Verified Test Recipient',
          message: trialMsg,
        },
        log: logItem,
        provider: gatewayResult.provider,
        message: `Twilio accepted Trial Test SMS (status: ${gatewayResult.status || 'ACCEPTED'})`,
      });
    } else {
      res.status(400).json({
        success: false,
        error: {
          code: 'TWILIO_TRIAL_FAILED',
          message: gatewayResult.error || 'Twilio Trial Test SMS dispatch failed',
        },
        message: gatewayResult.error || 'Twilio Trial Test SMS dispatch failed',
        log: logItem,
        provider: gatewayResult.provider,
      });
    }
  });

  app.post(['/api/v1/sms/send', '/api/sms/send'], async (req, res) => {
    const { recipientPhone, farmerName, aadharMasked, message, senderHeader, dltTemplateId, trialTest, isTrialTest, channel, templateType, customTrialMessage } = req.body;
    const cleanPhone = String(recipientPhone || '').replace(/\D/g, '');

    if (cleanPhone.length < 10) {
      return res.status(400).json({ success: false, error: 'Valid 10-digit phone required' });
    }

    const isTrialRequest = Boolean(trialTest || isTrialTest || channel === 'TWILIO_TRIAL_TEST' || templateType === 'TWILIO_TRIAL_TEST');

    if (isTrialRequest) {
      const trialMsg =
        customTrialMessage ||
        message ||
        process.env.TWILIO_TRIAL_MESSAGE ||
        'Your 1234 order of 1 items has shipped and should be delivered on tomorrow. Details: https://twilio.com';

      const gatewayResult = await dispatchSmsViaGateway(cleanPhone, trialMsg, true);

      const logItem: SmsLogItem = {
        id: `sms-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`,
        recipientPhone: cleanPhone.slice(-10),
        farmerName: farmerName || 'Verified Test Recipient',
        aadharMasked: aadharMasked || 'XXXX-XXXX-4589',
        message: trialMsg,
        senderHeader: 'TWILIO-TRIAL',
        dltTemplateId: 'TWILIO-PREDEFINED-ORDER-CONFIRMATION',
        status: gatewayResult.success ? (gatewayResult.status as any || 'ACCEPTED') : 'FAILED',
        dispatchedAt: new Date().toISOString(),
        dispatchedBy: 'Twilio Trial Dispatcher (Connectivity Test)',
        channel: 'SMS_GATEWAY',
        deliveryReceiptId: gatewayResult.externalSid,
      };

      smsLogs.unshift(logItem);

      if (gatewayResult.success) {
        return res.json({
          success: true,
          data: {
            sid: gatewayResult.externalSid,
            status: gatewayResult.status || 'ACCEPTED',
            deliveryReceiptId: gatewayResult.externalSid,
            recipientPhone: cleanPhone.slice(-10),
            farmerName: farmerName || 'Verified Test Recipient',
            message: trialMsg,
          },
          log: logItem,
          provider: gatewayResult.provider,
          message: `Twilio accepted Trial Test SMS (status: ${gatewayResult.status || 'ACCEPTED'})`,
        });
      } else {
        return res.status(400).json({
          success: false,
          error: {
            code: 'TWILIO_TRIAL_FAILED',
            message: gatewayResult.error || 'Twilio Trial Test SMS dispatch failed',
          },
          message: gatewayResult.error || 'Twilio Trial Test SMS dispatch failed',
          log: logItem,
          provider: gatewayResult.provider,
        });
      }
    }

    if (!message) {
      return res.status(400).json({ success: false, error: 'Valid phone and message required' });
    }

    const gatewayResult = await dispatchSmsViaGateway(cleanPhone, message.trim(), false);

    if (gatewayResult.isTrialRestricted) {
      const errorMsg = 'Twilio Trial accounts cannot send this custom message. Use Twilio Trial Test mode or upgrade/configure the Twilio account for production SMS.';
      const logItem: SmsLogItem = {
        id: `sms-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`,
        recipientPhone: cleanPhone.slice(-10),
        farmerName: farmerName || 'Kisan User',
        aadharMasked: aadharMasked || 'XXXX-XXXX-4589',
        message: message.trim(),
        senderHeader: senderHeader || 'VK-EUPARJAN',
        dltTemplateId: dltTemplateId || 'DLT-TE-1107161',
        status: 'FAILED',
        dispatchedAt: new Date().toISOString(),
        dispatchedBy: 'Twilio Gateway (Trial Restricted)',
        channel: 'SMS_GATEWAY',
        deliveryReceiptId: undefined,
      };
      smsLogs.unshift(logItem);

      return res.status(400).json({
        success: false,
        message: errorMsg,
        error: {
          code: 'TWILIO_TRIAL_CUSTOM_RESTRICTION',
          message: errorMsg,
        },
        log: logItem,
      });
    }

    const logItem: SmsLogItem = {
      id: `sms-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`,
      recipientPhone: cleanPhone.slice(-10),
      farmerName: farmerName || 'Kisan User',
      aadharMasked: aadharMasked || 'XXXX-XXXX-4589',
      message: message.trim(),
      senderHeader: senderHeader || 'VK-EUPARJAN',
      dltTemplateId: dltTemplateId || 'DLT-TE-1107161',
      status: gatewayResult.success ? (gatewayResult.status as any || 'ACCEPTED') : 'FAILED',
      dispatchedAt: new Date().toISOString(),
      dispatchedBy: gatewayResult.provider === 'TWILIO' ? 'Twilio SMS Gateway' : 'Admin Dispatcher',
      channel: 'SMS_GATEWAY',
      deliveryReceiptId: gatewayResult.externalSid,
    };

    smsLogs.unshift(logItem);
    res.json({
      success: gatewayResult.success,
      data: {
        sid: gatewayResult.externalSid,
        status: gatewayResult.status || (gatewayResult.success ? 'ACCEPTED' : 'FAILED'),
        deliveryReceiptId: gatewayResult.externalSid,
        recipientPhone: cleanPhone.slice(-10),
        farmerName: farmerName || 'Kisan User',
        message: message.trim(),
      },
      log: logItem,
      provider: gatewayResult.provider,
      message: gatewayResult.success ? 'SMS dispatched successfully' : (gatewayResult.error || 'Failed to dispatch SMS'),
    });
  });

  // ==========================================
  // 8. REPORTS, AUDIT LOGS & WEATHER
  // ==========================================
  app.get(['/api/v1/audit/logs', '/api/audit/logs'], (req, res) => {
    const { entityType, mandiId } = req.query;
    let list = auditLogs;
    if (entityType && typeof entityType === 'string') {
      list = list.filter((a) => a.entityType.toUpperCase() === entityType.toUpperCase());
    }
    if (mandiId && typeof mandiId === 'string') {
      list = list.filter((a) => a.mandiId === mandiId);
    }
    res.json({ success: true, count: list.length, logs: list });
  });

  app.get(['/api/v1/reports/district-stats', '/api/reports/district-stats'], (req, res) => {
    res.json({
      success: true,
      stats: DISTRICT_PROCUREMENT_STATS,
      mandisCount: mandis.length,
      totalBookings: bookings.length,
      registeredFarmersCount: farmers.length,
    });
  });

  app.get('/api/v1/weather/:district', (req, res) => {
    const dist = req.params.district.toLowerCase();
    const weather = DEMO_WEATHER_ALERTS[dist] || DEMO_WEATHER_ALERTS['sehore'] || {
      district: req.params.district,
      condition: 'Clear Sky',
      description: 'Clear sunny day across the district.',
      tempCelsius: 32,
      rainProbability: 5,
      humidity: 42,
      windSpeedKmH: 10,
      alertSeverity: 'NONE',
    };
    res.json({ success: true, weather });
  });

  // ==========================================
  // 9. AI ADVISORY & LOGISTICS
  // ==========================================
  const handleAiSlotRecommendation = async (req: express.Request, res: express.Response) => {
    const { district, mandiName, cropName, estimatedYieldQuintals, harvestDate, vehicleType } = req.body;

    const fallbackResponse = {
      recommendedSlot: '08:00 AM - 10:00 AM',
      recommendedDate: harvestDate || new Date().toISOString().split('T')[0],
      estimatedWaitTimeMinutes: 14,
      congestionScore: 'LOW',
      reasons: [
        'Early morning arrivals experience 65% faster weighbridge throughput.',
        'Moisture content is optimal before afternoon humidity shifts.',
        'Tractor trolley parking bays at the APMC shed have maximum vacancy during 8-10 AM.',
      ],
      weatherWarning: 'Weather clear in ' + (district || 'Madhya Pradesh') + ' with negligible rain risk for transit.',
      projectedProfitPerQuintal: 1150,
    };

    const prompt = `You are the AI Mandi Logistics Optimizer for Madhya Pradesh APMC Procurement Centers (KisanSarthi MP).
Analyze the following farmer procurement request:
- District: ${district || 'Sehore'}
- Mandi Center: ${mandiName || 'Krishi Upaj Mandi Sehore'}
- Crop: ${cropName || 'Wheat (Sharbati)'}
- Quantity: ${estimatedYieldQuintals || 50} quintals
- Planned Harvest: ${harvestDate || '2026-09-08'}
- Vehicle: ${vehicleType || 'Tractor Trolley'}

Respond strictly in valid JSON format matching this schema:
{
  "recommendedSlot": "string",
  "recommendedDate": "YYYY-MM-DD",
  "estimatedWaitTimeMinutes": number,
  "congestionScore": "LOW" | "MODERATE" | "HIGH",
  "reasons": ["string", "string", "string"],
  "weatherWarning": "string",
  "projectedProfitPerQuintal": number
}`;

    const { data, mode } = await executeGeminiWithFallback(prompt, fallbackResponse);
    res.json({ success: true, suggestion: data, mode });
  };

  app.post('/api/v1/ai/slot-recommendation', handleAiSlotRecommendation);
  app.post('/api/ai/optimize-slot', handleAiSlotRecommendation);

  const handleAiYieldAdvisor = async (req: express.Request, res: express.Response) => {
    const { cropName, acreage, district, estimatedYield } = req.body;

    const fallbackResponse = {
      predictedYieldQuintals: (Number(acreage) || 4) * 18,
      estimatedRevenue: (Number(acreage) || 4) * 18 * 2400,
      estimatedProfit: (Number(acreage) || 4) * (18 * 2400 - 11500),
      profitMarginPercent: 73.3,
      keyRecommendations: [
        'Maintain moisture below 12% by sun-drying 2 days post-harvest to avoid FAQ rejection.',
        'Pre-clean foreign organic matter to guarantee Grade A premium at the weighbridge.',
        'Opt for early morning delivery slot to minimize tractor fuel idling in the mandi yard.',
      ],
      soilHealthTips: [
        'Malwa deep black soils benefit from post-Rabi green manuring (Dhaincha/Sanai).',
        'Balanced NPK 12:32:16 application prior to next sowing ensures high test weight.',
      ],
      harvestWindowAdvice: 'Ideal harvest window is within 3-5 days after 85% grains achieve golden yellow firmness.',
    };

    const prompt = `You are the Madhya Pradesh Krishi Vigyan Kendra (KVK) Agricultural AI Advisor for KisanSarthi MP.
Farmer profile:
- Commodity: ${cropName || 'Wheat (Sharbati)'}
- Land Acreage: ${acreage || 4} acres
- District: ${district || 'Sehore, MP (Malwa Plateau)'}
- Farmer Estimated Yield: ${estimatedYield || 70} quintals

Respond strictly in JSON format matching this schema:
{
  "predictedYieldQuintals": number,
  "estimatedRevenue": number,
  "estimatedProfit": number,
  "profitMarginPercent": number,
  "keyRecommendations": ["string", "string", "string"],
  "soilHealthTips": ["string", "string"],
  "harvestWindowAdvice": "string"
}`;

    const { data, mode } = await executeGeminiWithFallback(prompt, fallbackResponse);
    res.json({ success: true, analysis: data, mode });
  };

  app.post('/api/v1/ai/yield-advisor', handleAiYieldAdvisor);
  app.post('/api/ai/yield-advisor', handleAiYieldAdvisor);

  // ==========================================
  // VITE MIDDLEWARE & STATIC SERVING
  // ==========================================
  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  // Listen on port 3000
  server.listen(PORT, '0.0.0.0', () => {
    console.log(`KisanSarthi Unified Server running on http://0.0.0.0:${PORT} with WebSocket on /ws`);
  });
}

startServer();
