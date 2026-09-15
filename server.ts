/**
 * ============================================================================
 * KISANSARTHI AI STUDIO DEMO / MOCK BACKEND SERVER
 * ============================================================================
 * NOTICE: This Node.js/Express server (`server.ts`) is a self-contained, in-memory
 * demo and mock implementation engineered specifically for rapid Google AI Studio
 * web deployment, container hosting, and local client preview.
 *
 * IT IS NOT THE CANONICAL BACKEND OR SOURCE OF TRUTH.
 *
 * The canonical production backend is implemented in Java / Spring Boot under the
 * `/backend` directory (Spring Boot 3, PostgreSQL, Flyway, Redis, Spring Security).
 *
 * For detailed API parity mapping, behavioral differences, and response shapes,
 * see `BACKEND_PARITY.md` in the project root.
 * ============================================================================
 */

import express from 'express';
import http from 'http';
import path from 'path';
import fs from 'fs';
import crypto from 'crypto';
import jwt from 'jsonwebtoken';
import { WebSocketServer, WebSocket } from 'ws';
import { createServer as createViteServer } from 'vite';
import { GoogleGenAI } from '@google/genai';
import dotenv from 'dotenv';
import {
  ALL_INDIA_CROPS,
  ALL_INDIA_MANDIS,
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

// Helper to mirror backend OtpService.java SHA-256 hash
function hashOtp(otp: string): string {
  return crypto.createHash('sha256').update(otp, 'utf8').digest('hex');
}

// Signed JWT token generator with real expiry, mirroring backend JwtTokenProvider.java
const JWT_SECRET = process.env.JWT_SECRET || 'kisansarthi-secure-jwt-signing-secret-key-32chars!';
const JWT_EXPIRATION = '1h'; // 1 hour expiration

function generateSignedAccessToken(payload: {
  userId: string;
  role: 'FARMER' | 'ADMIN';
  phone?: string;
  name?: string;
  mandiId?: string;
}): string {
  return jwt.sign(payload, JWT_SECRET, {
    expiresIn: JWT_EXPIRATION,
    subject: payload.userId,
  });
}

// ==========================================
// CANONICAL STATE & STORAGE
// Concurrency-safe in-memory store with persistence
// ==========================================
interface MandiSequenceTracker {
  [mandiIdAndDate: string]: number;
}

interface StoredOtp {
  phone: string;
  otpHash: string;
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

const mandis: MandiCenter[] = JSON.parse(JSON.stringify(ALL_INDIA_MANDIS));
const crops: CropInfo[] = JSON.parse(JSON.stringify(ALL_INDIA_CROPS));
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
  // Port 3000 is mandatory for the infrastructure nginx reverse proxy
  const PORT = 3000;
  const server = http.createServer(app);

  server.on('error', (err: { code?: string; message?: string }) => {
    console.error(`Server error on port ${PORT}:`, err?.message || err);
  });

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

    // Generate secure random 6-digit OTP (mirroring OtpService.java SecureRandom)
    const generatedOtp = crypto.randomInt(100000, 1000000).toString();
    otpStore.set(cleanPhone, {
      phone: cleanPhone,
      otpHash: hashOtp(generatedOtp),
      expiresAt: now + 5 * 60 * 1000, // 5 minutes TTL
      attempts: 0,
      lastSentAt: now,
    });

    const farmer = farmers.find((f) => f.phone === cleanPhone);
    const farmerName = farmer?.name || `Kisan (${cleanPhone.slice(-4)})`;
    const maskedAadhar = farmer?.maskedAadhar || `XXXX-XXXX-${cleanPhone.slice(-4)}`;

    const otpMessage = `[MP-EUPARJAN] Aapka KisanSarthi login OTP: ${generatedOtp} hai. Kripya ise kisi se saajha na karein. Valid for 5 mins.`;
    const receiptId = `DLT-OTP-${Date.now().toString().slice(-6)}`;

    // High-speed non-blocking asynchronous SMS dispatch
    dispatchSmsViaGateway(cleanPhone, otpMessage)
      .then((gatewayResult) => {
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
          deliveryReceiptId: gatewayResult.externalSid || receiptId,
        };
        smsLogs.unshift(smsReceipt);
      })
      .catch((err) => {
        console.warn('[SMS Dispatch Notice]:', err?.message || err);
      });

    // CRITICAL (Bug 8 Fix): NEVER return the OTP in the response body!
    res.json({
      success: true,
      message: `OTP has been dispatched via secure SMS to +91 ******${cleanPhone.slice(-4)}`,
      cooldownSeconds: 60,
      provider: 'SMS_GATEWAY',
      deliveryReceiptId: receiptId,
      devNote: 'In local development, check /api/v1/sms/logs or the SMS logs tab to inspect the simulated SMS.',
    });
  };

  app.post('/api/v1/auth/send-otp', handleSendOtp);
  app.post('/api/auth/send-otp', handleSendOtp);

  const handleVerifyOtp = (req: express.Request, res: express.Response) => {
    const { phone, otp } = req.body;
    const cleanPhone = String(phone || '').replace(/\D/g, '');
    const cleanOtp = String(otp || '').trim();

    if (cleanPhone.length < 10) {
      return res.status(400).json({ success: false, error: 'Valid 10-digit mobile number required' });
    }

    if (!cleanOtp) {
      return res.status(400).json({ success: false, error: 'OTP verification code is required' });
    }

    const stored = otpStore.get(cleanPhone);
    const now = Date.now();

    // Strict verification logic mirroring backend OtpService.java:
    // - Track attempts (MAX_ATTEMPTS = 3)
    // - Enforce 5-minute expiry (TTL)
    // - Check SHA-256 hash match against what was dispatched
    // - Unconditional bypass removed
    // - Optional sandbox bypass strictly gated by DEMO_MODE=true AND only when no real OTP was issued
    let isValid = false;
    if (stored) {
      if (now > stored.expiresAt) {
        otpStore.delete(cleanPhone);
        return res.status(400).json({
          success: false,
          code: 'OTP_EXPIRED',
          error: 'OTP has expired or does not exist. Please request a new one.',
        });
      }
      stored.attempts += 1;
      if (stored.attempts > 3) {
        otpStore.delete(cleanPhone);
        return res.status(400).json({
          success: false,
          code: 'MAX_ATTEMPTS_EXCEEDED',
          error: 'Maximum verification attempts exceeded. Please request a new OTP.',
        });
      }
      const inputHash = hashOtp(cleanOtp);
      if (inputHash === stored.otpHash) {
        isValid = true;
        otpStore.delete(cleanPhone);
      } else {
        // Never allow demo bypass when a real OTP was actually issued and doesn't match!
        return res.status(400).json({
          success: false,
          error: 'Invalid verification code. Please check your SMS and try again.',
        });
      }
    } else {
      // No active OTP was issued or it has expired
      const isDemoMode = process.env.DEMO_MODE === 'true';
      if (isDemoMode && (cleanOtp === '4826' || cleanOtp === '123456')) {
        isValid = true;
      } else {
        return res.status(400).json({
          success: false,
          code: 'OTP_EXPIRED',
          error: 'OTP has expired or does not exist. Please request a new one.',
        });
      }
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

    const accessToken = generateSignedAccessToken({
      userId: farmer.id,
      role: 'FARMER',
      phone: farmer.phone,
      name: farmer.name,
    });
    const farmerUserData = {
      id: farmer.id,
      name: farmer.name,
      phone: farmer.phone,
      aadharNumber: farmer.aadharNumber,
      maskedAadhar: farmer.maskedAadhar,
      district: farmer.district,
      village: farmer.village,
      role: 'FARMER' as const,
    };

    recordAuditLog(
      'FARMER_LOGIN_OTP',
      'SYSTEM',
      farmer.id,
      farmer.name,
      `Farmer authenticated via SMS OTP on mobile +91 ******${cleanPhone.slice(-4)}`,
      '',
      'ACTIVE',
      0
    );

    return res.json({
      success: true,
      data: {
        accessToken,
        user: farmerUserData,
      },
      farmer: farmerUserData,
      accessToken,
      token: accessToken,
      message: 'Farmer verified and authenticated successfully',
    });
  };

  app.post('/api/v1/auth/verify-otp', handleVerifyOtp);
  app.post('/api/auth/verify-otp', handleVerifyOtp);

  // Fast department admin authentication (Instant sub-10ms response)
  const handleAdminLogin = (req: express.Request, res: express.Response) => {
    const { username, password, mandiId } = req.body;
    const cleanUsername = String(username || '').trim();
    const cleanPassword = String(password || '').trim();

    // Verify authorized passcode
    const validPasscodes = [
      'Admin@India2026',
      'Admin@ENAM2026',
      'Admin@MPMandi2026',
      'Admin@2026',
      'admin',
      'admin123',
    ];
    if (!validPasscodes.includes(cleanPassword)) {
      return res.status(401).json({
        success: false,
        error: 'Invalid passcode! Authorized passcode: Admin@India2026 or Admin@MPMandi2026',
      });
    }

    const officerMandiId = mandiId || 'mandi-sehore';
    const targetMandi = mandis.find((m) => m.id === officerMandiId) || mandis[0];
    const adminUser = {
      id: `admin-${cleanUsername || 'officer'}`,
      name: `Officer (${cleanUsername || targetMandi.district + ' Mandi'})`,
      phone: targetMandi.phone || '1800-180-1551',
      state: targetMandi.state,
      district: targetMandi.district,
      role: 'ADMIN' as const,
      mandiId: targetMandi.id,
    };
    const accessToken = generateSignedAccessToken({
      userId: adminUser.id,
      role: 'ADMIN',
      phone: adminUser.phone,
      name: adminUser.name,
      mandiId: targetMandi.id,
    });

    recordAuditLog(
      'ADMIN_LOGIN',
      'SYSTEM',
      adminUser.id,
      adminUser.name,
      `Officer login authorized for mandi: ${targetMandi.name} (${targetMandi.state})`,
      '',
      'ACTIVE',
      0
    );

    return res.json({
      success: true,
      data: {
        accessToken,
        user: adminUser,
      },
      accessToken,
      token: accessToken,
      message: 'Admin officer authenticated successfully',
    });
  };

  app.post('/api/v1/auth/admin-login', handleAdminLogin);
  app.post('/api/auth/admin-login', handleAdminLogin);

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

  // Weighment Workflow (Start, Record, Complete)
  app.post(['/api/v1/bookings/:id/weighment/start', '/api/bookings/:id/weighment/start'], (req, res) => {
    const { id } = req.params;
    const booking = bookings.find((b) => b.id === id);
    if (!booking) return res.status(404).json({ success: false, error: `Booking not found: ${id}` });

    booking.status = 'WEIGHING';
    recordAuditLog(
      'WEIGHMENT_STARTED',
      'WEIGHBRIDGE',
      booking.id,
      req.query.operatorName || req.body?.actor || 'Weighbridge Operator',
      `Vehicle admitted to weighbridge bay: ${req.query.weighbridgeBay || 'Kanta Bay 1'}`,
      'GATE_ENTERED',
      'WEIGHING',
      0,
      booking.mandiCenterId
    );

    broadcast(`/topic/mandi/${booking.mandiCenterId}/queue`, {
      event: 'WEIGHMENT_STARTED',
      bookingId: booking.id,
      booking,
      timestamp: new Date().toISOString(),
    });
    broadcast('/topic/bookings', { type: 'BOOKING_UPDATE', booking });

    res.json({ success: true, booking, message: 'Weighment started successfully' });
  });

  app.post(['/api/v1/bookings/:id/weighment', '/api/bookings/:id/weighment'], (req, res) => {
    const { id } = req.params;
    const booking = bookings.find((b) => b.id === id);
    if (!booking) {
      return res.status(404).json({ success: false, error: `Booking not found: ${id}` });
    }

    const {
      actualGrossWeightKg,
      actualTareWeightKg,
      grossWeightQuintals,
      tareWeightQuintals,
      moisturePct,
      foreignMatterPct,
      qualityGrade,
    } = req.body;

    // Support both Kg and Quintals
    if (grossWeightQuintals !== undefined && grossWeightQuintals !== null) {
      booking.grossWeightQuintals = Number(grossWeightQuintals);
      booking.actualGrossWeightKg = Math.round(Number(grossWeightQuintals) * 100);
    } else if (actualGrossWeightKg !== undefined && actualGrossWeightKg !== null) {
      booking.actualGrossWeightKg = Number(actualGrossWeightKg);
      booking.grossWeightQuintals = parseFloat((Number(actualGrossWeightKg) / 100).toFixed(2));
    }

    if (tareWeightQuintals !== undefined && tareWeightQuintals !== null) {
      booking.tareWeightQuintals = Number(tareWeightQuintals);
      booking.actualTareWeightKg = Math.round(Number(tareWeightQuintals) * 100);
    } else if (actualTareWeightKg !== undefined && actualTareWeightKg !== null) {
      booking.actualTareWeightKg = Number(actualTareWeightKg);
      booking.tareWeightQuintals = parseFloat((Number(actualTareWeightKg) / 100).toFixed(2));
    }

    if (moisturePct !== undefined) booking.moisturePct = Number(moisturePct);
    if (foreignMatterPct !== undefined) booking.foreignMatterPct = Number(foreignMatterPct);
    if (qualityGrade !== undefined) booking.qualityGrade = qualityGrade;

    const grossKg = booking.actualGrossWeightKg || 0;
    const tareKg = booking.actualTareWeightKg || 0;

    if (grossKg > 0 && tareKg > 0 && grossKg > tareKg) {
      const netKg = grossKg - tareKg;
      booking.netWeightQuintals = parseFloat((netKg / 100).toFixed(2));
      booking.totalPayoutRs = Math.round(booking.netWeightQuintals * 2400);
      booking.status = 'WEIGHMENT_COMPLETED';
      booking.paymentStatus = 'PENDING';

      recalcMandiCapacity(booking.mandiCenterId);
      recalcSlotCapacities();

      recordAuditLog(
        'WEIGHMENT_COMPLETED',
        'WEIGHBRIDGE',
        booking.id,
        req.body.actor || 'Weighbridge Officer',
        `Completed weighment: Gross ${grossKg} kg, Tare ${tareKg} kg, Authoritative Net ${netKg} kg (${booking.netWeightQuintals} Qtl). Total payable: ₹${booking.totalPayoutRs}`,
        'WEIGHMENT_STAGE_1',
        'WEIGHMENT_COMPLETED',
        netKg,
        booking.mandiCenterId
      );
    } else if (grossKg > 0) {
      booking.status = 'WEIGHMENT_STAGE_1';
      recalcMandiCapacity(booking.mandiCenterId);
    }

    broadcast(`/topic/mandi/${booking.mandiCenterId}/queue`, {
      event: 'WEIGHMENT_RECORDED',
      bookingId: booking.id,
      booking,
      timestamp: new Date().toISOString(),
    });
    broadcast('/topic/bookings', { type: 'BOOKING_UPDATE', booking });

    res.json({
      success: true,
      booking,
      message: 'Weighment recorded successfully',
      data: {
        bookingId: booking.id,
        tokenNumber: booking.tokenNumber,
        actualGrossWeightKg: booking.actualGrossWeightKg,
        actualTareWeightKg: booking.actualTareWeightKg,
        grossWeightQuintals: booking.grossWeightQuintals,
        tareWeightQuintals: booking.tareWeightQuintals,
        netWeightQuintals: booking.netWeightQuintals,
        netWeightKg: (booking.actualGrossWeightKg || 0) - (booking.actualTareWeightKg || 0),
        moisturePct: booking.moisturePct,
        foreignMatterPct: booking.foreignMatterPct,
        qualityGrade: booking.qualityGrade,
        netPayableAmount: booking.totalPayoutRs,
        status: booking.status,
      },
    });
  });

  app.post(['/api/v1/bookings/:id/weighment/complete', '/api/bookings/:id/weighment/complete'], (req, res) => {
    const booking = bookings.find((b) => b.id === req.params.id);
    if (!booking) return res.status(404).json({ success: false, error: 'Booking not found' });
    booking.status = 'WEIGHMENT_COMPLETED';
    broadcast('/topic/bookings', { type: 'BOOKING_UPDATE', booking });
    res.json({ success: true, booking });
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
        grossWeightQuintals: booking.grossWeightQuintals,
        tareWeightQuintals: booking.tareWeightQuintals,
        netWeightQuintals: booking.netWeightQuintals,
        moisturePct: booking.moisturePct,
        foreignMatterPct: booking.foreignMatterPct,
        qualityGrade: booking.qualityGrade,
        totalPayoutRs: booking.totalPayoutRs,
        status: booking.status,
      },
    });
  });

  // Phase 3: Procurement Certification Endpoint
  app.post(['/api/v1/bookings/:id/procurement/complete', '/api/bookings/:id/procurement/complete'], (req, res) => {
    const { id } = req.params;
    const booking = bookings.find((b) => b.id === id);
    if (!booking) return res.status(404).json({ success: false, error: `Booking not found: ${id}` });

    booking.status = 'PROCUREMENT_COMPLETED';
    booking.paymentStatus = 'PENDING';
    recordAuditLog(
      'PROCUREMENT_CERTIFIED',
      'BOOKING',
      booking.id,
      (req.query.officerName as string) || req.body?.actor || 'APMC Procurement Officer',
      `Procurement certified for ${booking.tokenNumber}: Net ${booking.netWeightQuintals} Qtl, Total ₹${booking.totalPayoutRs}`,
      'WEIGHMENT_COMPLETED',
      'PROCUREMENT_COMPLETED',
      booking.netWeightQuintals || 0,
      booking.mandiCenterId
    );

    broadcast(`/topic/mandi/${booking.mandiCenterId}/queue`, {
      event: 'PROCUREMENT_COMPLETED',
      bookingId: booking.id,
      booking,
      timestamp: new Date().toISOString(),
    });
    broadcast('/topic/bookings', { type: 'BOOKING_UPDATE', booking });

    res.json({ success: true, booking, message: 'Procurement certified successfully' });
  });

  // Phase 3: Payment Initiation & Confirmation Endpoints
  app.post(['/api/v1/bookings/:id/payment', '/api/bookings/:id/payment'], (req, res) => {
    const { id } = req.params;
    const booking = bookings.find((b) => b.id === id);
    if (!booking) return res.status(404).json({ success: false, error: `Booking not found: ${id}` });

    const ref = `DBT-MP-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${Math.floor(1000 + Math.random() * 9000)}`;
    booking.status = 'PAYMENT_PROCESSING';
    booking.paymentStatus = 'DBT_INITIATED';
    booking.dbtReferenceNo = booking.dbtReferenceNo || ref;
    booking.utrNumber = booking.dbtReferenceNo;
    booking.bankAccountLast4 = booking.bankAccountLast4 || '4321';
    booking.bankIfsc = booking.bankIfsc || 'SBIN0001111';

    recordAuditLog(
      'PAYMENT_INITIATED',
      'BOOKING',
      booking.id,
      req.body?.actor || 'DBT Portal Officer',
      `DBT Payment initiated for Token ${booking.tokenNumber}: Ref ${booking.dbtReferenceNo}, Amount ₹${booking.totalPayoutRs} to A/C ending ${booking.bankAccountLast4}`,
      'PROCUREMENT_COMPLETED',
      'PAYMENT_PROCESSING',
      booking.totalPayoutRs || 0,
      booking.mandiCenterId
    );

    broadcast(`/topic/mandi/${booking.mandiCenterId}/queue`, {
      event: 'PAYMENT_INITIATED',
      bookingId: booking.id,
      booking,
      timestamp: new Date().toISOString(),
    });
    broadcast('/topic/bookings', { type: 'BOOKING_UPDATE', booking });

    res.json({
      success: true,
      booking,
      data: {
        bookingId: booking.id,
        dbtReferenceNo: booking.dbtReferenceNo,
        paymentStatus: 'PROCESSING',
        netPayableAmount: booking.totalPayoutRs,
        bankAccountLast4: booking.bankAccountLast4,
        ifscCode: booking.bankIfsc,
      },
    });
  });

  app.post(['/api/v1/bookings/:id/payment/confirm', '/api/bookings/:id/payment/confirm'], (req, res) => {
    const { id } = req.params;
    const booking = bookings.find((b) => b.id === id);
    if (!booking) return res.status(404).json({ success: false, error: `Booking not found: ${id}` });

    booking.status = 'COMPLETED';
    booking.paymentStatus = 'COMPLETED';
    booking.completedAt = new Date().toISOString();
    booking.paymentDate = new Date().toISOString();

    recordAuditLog(
      'PAYMENT_CREDITED',
      'BOOKING',
      booking.id,
      'PFMS / NPCI DBT Gateway',
      `DBT payout credited to beneficiary bank account. Ref: ${booking.dbtReferenceNo || booking.utrNumber}. Status COMPLETED.`,
      'PAYMENT_PROCESSING',
      'COMPLETED',
      booking.totalPayoutRs || 0,
      booking.mandiCenterId
    );

    broadcast(`/topic/mandi/${booking.mandiCenterId}/queue`, {
      event: 'PAYMENT_CREDITED',
      bookingId: booking.id,
      booking,
      timestamp: new Date().toISOString(),
    });
    broadcast('/topic/bookings', { type: 'BOOKING_UPDATE', booking });

    res.json({
      success: true,
      booking,
      data: {
        bookingId: booking.id,
        dbtReferenceNo: booking.dbtReferenceNo,
        paymentStatus: 'COMPLETED',
        netPayableAmount: booking.totalPayoutRs,
        creditedAt: booking.completedAt,
      },
    });
  });

  app.get(['/api/v1/bookings/:id/payment', '/api/bookings/:id/payment'], (req, res) => {
    const booking = bookings.find((b) => b.id === req.params.id);
    if (!booking) return res.status(404).json({ success: false, error: 'Booking not found' });
    res.json({
      success: true,
      data: {
        bookingId: booking.id,
        dbtReferenceNo: booking.dbtReferenceNo || booking.utrNumber,
        paymentStatus: booking.paymentStatus,
        netPayableAmount: booking.totalPayoutRs,
        bankAccountLast4: booking.bankAccountLast4 || '4321',
        ifscCode: booking.bankIfsc || 'SBIN0001111',
        creditedAt: booking.completedAt,
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

  // Dynamic Statewide Overview
  app.get(['/api/v1/reports/overview', '/api/reports/overview'], (req, res) => {
    const completedBookings = bookings.filter((b) =>
      ['PROCUREMENT_COMPLETED', 'PAYMENT_PROCESSING', 'COMPLETED'].includes(b.status)
    );

    const totalCertifiedQuantity = completedBookings.reduce((acc, b) => acc + (b.netWeightQuintals || b.estimatedYieldQuintals || 0), 0);
    const totalProcurementValue = completedBookings.reduce((acc, b) => acc + (b.totalPayoutRs || 0), 0);
    const totalDbtDisbursed = bookings.filter(b => b.paymentStatus === 'COMPLETED' || b.paymentStatus === 'CREDITED_TO_BANK').reduce((acc, b) => acc + (b.totalPayoutRs || 0), 0);
    const totalFarmersServed = new Set(completedBookings.map((b) => b.farmerPhone || b.farmerName)).size;
    const totalActiveMandis = mandis.filter((m) => m.gateStatus === 'OPEN').length || mandis.length;
    const totalWaitingFarmers = bookings.filter((b) =>
      ['GATE_CALLED', 'GATE_ENTERED', 'WEIGHING', 'WEIGHMENT_STAGE_1', 'QC_INSPECTION', 'WEIGHBRIDGE_GROSS'].includes(b.status)
    ).length;

    const statusBreakdown: Record<string, number> = {};
    bookings.forEach((b) => {
      statusBreakdown[b.status] = (statusBreakdown[b.status] || 0) + 1;
    });

    res.json({
      success: true,
      data: {
        totalBookings: bookings.length,
        totalCompletedProcurements: completedBookings.length,
        totalCertifiedQuantityQuintals: Math.round(totalCertifiedQuantity * 100) / 100,
        totalProcurementValueRs: Math.round(totalProcurementValue * 100) / 100,
        totalDbtDisbursedRs: Math.round(totalDbtDisbursed * 100) / 100,
        totalFarmersServed,
        totalActiveMandis,
        totalWaitingFarmers,
        statusBreakdown,
      },
    });
  });

  // Dynamic District Stats
  app.get(['/api/v1/reports/district-stats', '/api/reports/district-stats'], (req, res) => {
    // Dynamically calculate district metrics blending targets with live booking state
    const districtGroups: Record<string, typeof DISTRICT_PROCUREMENT_STATS[0]> = {};

    DISTRICT_PROCUREMENT_STATS.forEach((stat) => {
      districtGroups[stat.district.toLowerCase()] = { ...stat };
    });

    // Augment with real bookings
    bookings.forEach((b) => {
      const dist = (b.district || 'Sehore').toLowerCase();
      if (districtGroups[dist]) {
        const netQtl = b.netWeightQuintals || b.estimatedYieldQuintals || 0;
        if (['PROCUREMENT_COMPLETED', 'PAYMENT_PROCESSING', 'COMPLETED'].includes(b.status)) {
          districtGroups[dist].totalProcuredQuintals += netQtl;
          if (b.paymentStatus === 'COMPLETED' || b.paymentStatus === 'CREDITED_TO_BANK') {
            districtGroups[dist].dbtDisbursedCrores += (b.totalPayoutRs || 0) / 10000000.0;
          }
        }
      }
    });

    const dynamicStats = Object.values(districtGroups).map((stat) => {
      const achievedPct = stat.targetQuintals > 0 ? (stat.totalProcuredQuintals / stat.targetQuintals) * 100 : 0;
      return {
        ...stat,
        achievementPercentage: Math.round(achievedPct * 10) / 10,
        status: achievedPct > 85 ? 'HIGH_VOLUME' : 'NORMAL',
      };
    });

    res.json({
      success: true,
      stats: dynamicStats,
      data: {
        stats: dynamicStats,
        mandisCount: mandis.length,
        totalBookings: bookings.length,
        registeredFarmersCount: Math.max(farmers.length, 164300),
      },
      mandisCount: mandis.length,
      totalBookings: bookings.length,
      registeredFarmersCount: Math.max(farmers.length, 164300),
    });
  });

  // Mandi-level performance & real-time queues
  app.get(['/api/v1/reports/mandi-performance', '/api/reports/mandi-performance'], (req, res) => {
    const { district } = req.query;
    let targetMandis = mandis;
    if (district && typeof district === 'string') {
      targetMandis = targetMandis.filter((m) => m.district.toLowerCase() === district.toLowerCase());
    }

    const performance = targetMandis.map((m) => {
      const mBookings = bookings.filter((b) => b.mandiCenterId === m.id);
      const completed = mBookings.filter((b) =>
        ['PROCUREMENT_COMPLETED', 'PAYMENT_PROCESSING', 'COMPLETED'].includes(b.status)
      );
      const certifiedQtl = completed.reduce((acc, b) => acc + (b.netWeightQuintals || b.estimatedYieldQuintals || 0), 0);
      const totalDbt = mBookings.filter((b) => b.paymentStatus === 'COMPLETED' || b.paymentStatus === 'CREDITED_TO_BANK').reduce((acc, b) => acc + (b.totalPayoutRs || 0), 0);

      const queueLen = m.activeTokensWaiting || 0;
      const avgMins = m.averageProcessingMins || 15;
      const waitTime = queueLen * avgMins;
      const remainingCapacity = Math.max(0, m.dailyCapacityQuintals - certifiedQtl);

      let status = 'AVAILABLE';
      let bottleneckReason: string | undefined = undefined;

      if (m.gateStatus !== 'OPEN') {
        status = 'CLOSED';
        bottleneckReason = 'Mandi gate is closed';
      } else if (remainingCapacity <= 0) {
        status = 'FULL';
        bottleneckReason = 'Daily intake capacity saturated';
      } else if (queueLen >= 25 || waitTime >= 90) {
        status = 'HIGH_LOAD';
        bottleneckReason = `Congestion: ${queueLen} vehicles waiting (${waitTime} mins wait)`;
      } else if (queueLen >= 10 || waitTime >= 40) {
        status = 'BUSY';
      }

      return {
        mandiId: m.id,
        mandiName: m.name,
        hindiName: m.hindiName,
        district: m.district,
        totalBookings: mBookings.length,
        completedProcurements: completed.length,
        certifiedQuantityQuintals: Math.round(certifiedQtl * 100) / 100,
        remainingSlotCapacityQuintals: Math.round(remainingCapacity),
        averageProcessingMins: avgMins,
        estimatedWaitTimeMins: waitTime,
        currentQueueLength: queueLen,
        throughputPerHour: Math.round((completed.length / 8.0) * 10) / 10,
        totalDbtAmountRs: Math.round(totalDbt),
        status,
        bottleneckReason,
      };
    });

    res.json({ success: true, data: performance });
  });

  // Bottlenecks detection endpoint
  app.get(['/api/v1/reports/bottlenecks', '/api/reports/bottlenecks'], (req, res) => {
    const alerts: any[] = [];
    mandis.forEach((m) => {
      const queueLen = m.activeTokensWaiting || 0;
      const avgMins = m.averageProcessingMins || 15;
      const waitTime = queueLen * avgMins;

      if (queueLen >= 20) {
        alerts.push({
          mandiId: m.id,
          mandiName: m.name,
          district: m.district,
          severity: queueLen >= 30 ? 'CRITICAL' : 'MEDIUM',
          indicator: 'QUEUE_CONGESTION',
          metricDescription: `${queueLen} vehicles waiting in queue`,
          reason: 'Vehicle arrival volume exceeds single-weighbridge intake capacity.',
          waitingFarmers: queueLen,
          estimatedWaitMinutes: waitTime,
          remainingSlotCapacityQuintals: m.dailyCapacityQuintals,
        });
      }

      if (waitTime >= 60) {
        alerts.push({
          mandiId: m.id,
          mandiName: m.name,
          district: m.district,
          severity: waitTime >= 120 ? 'CRITICAL' : 'MEDIUM',
          indicator: 'HIGH_WAIT_TIME',
          metricDescription: `${waitTime} mins average turnaround`,
          reason: 'Estimated turnaround time breaches Citizen Charter SLA (60 minutes).',
          waitingFarmers: queueLen,
          estimatedWaitMinutes: waitTime,
          remainingSlotCapacityQuintals: m.dailyCapacityQuintals,
        });
      }
    });

    res.json({ success: true, data: alerts });
  });

  // Crop-wise procurement report
  app.get(['/api/v1/reports/crops', '/api/reports/crops'], (req, res) => {
    const cropReports = crops.map((crop) => {
      const cBookings = bookings.filter((b) => (b.cropName || '').toLowerCase().includes(crop.name.toLowerCase().split(' ')[0]));
      const completed = cBookings.filter((b) =>
        ['PROCUREMENT_COMPLETED', 'PAYMENT_PROCESSING', 'COMPLETED'].includes(b.status)
      );

      const netQtl = completed.reduce((acc, b) => acc + (b.netWeightQuintals || b.estimatedYieldQuintals || 0), 0);
      const totalVal = completed.reduce((acc, b) => acc + (b.totalPayoutRs || 0), 0);
      const dbt = cBookings.filter((b) => b.paymentStatus === 'COMPLETED' || b.paymentStatus === 'CREDITED_TO_BANK').reduce((acc, b) => acc + (b.totalPayoutRs || 0), 0);
      const farmersCount = new Set(completed.map((b) => b.farmerPhone || b.farmerName)).size;
      const avgQtl = farmersCount > 0 ? netQtl / farmersCount : 0;

      return {
        cropId: crop.id,
        cropName: crop.name,
        hindiName: crop.hindiName,
        season: crop.season,
        totalBookings: cBookings.length,
        completedProcurements: completed.length,
        certifiedQuantityQuintals: Math.round(netQtl * 100) / 100,
        totalProcurementValueRs: Math.round(totalVal),
        farmersServed: farmersCount,
        averageQuantityPerFarmerQuintals: Math.round(avgQtl * 100) / 100,
        totalDbtDisbursedRs: Math.round(dbt),
      };
    });

    res.json({ success: true, data: cropReports });
  });

  // Quality & Weighment Analytics
  app.get(['/api/v1/reports/quality-weighment', '/api/reports/quality-weighment'], (req, res) => {
    const completed = bookings.filter((b) =>
      ['PROCUREMENT_COMPLETED', 'PAYMENT_PROCESSING', 'COMPLETED'].includes(b.status)
    );

    const moistures = completed.map((b) => b.moisturePct || 11.2);
    const avgMoisture = moistures.length > 0 ? moistures.reduce((a, b) => a + b, 0) / moistures.length : 11.4;
    const aboveFaq = moistures.filter((m) => m > 12.0).length;

    const totalNet = completed.reduce((acc, b) => acc + (b.netWeightQuintals || b.estimatedYieldQuintals || 0), 0);
    const totalGross = totalNet * 1.35; // Standard tare factor
    const totalTare = totalGross - totalNet;

    res.json({
      success: true,
      data: {
        totalVehiclesWeighed: Math.max(completed.length, 142),
        totalGrossQuintals: Math.round(totalGross * 100) / 100,
        totalTareQuintals: Math.round(totalTare * 100) / 100,
        totalCertifiedNetQuintals: Math.round(totalNet * 100) / 100,
        averageNetQuintalsPerVehicle: completed.length > 0 ? Math.round((totalNet / completed.length) * 100) / 100 : 42.5,
        averageMoisturePct: Math.round(avgMoisture * 100) / 100,
        minMoisturePct: 9.8,
        maxMoisturePct: 13.8,
        samplesWithinFaqThreshold: Math.max(0, moistures.length - aboveFaq),
        samplesAboveFaqThreshold: aboveFaq,
        percentAboveFaqThreshold: moistures.length > 0 ? Math.round((aboveFaq / moistures.length) * 1000) / 10 : 4.2,
        averageForeignMatterPct: 0.65,
        totalDockageQuintals: Math.round(totalNet * 0.0065 * 100) / 100,
        dockagePercentage: 0.65,
      },
    });
  });

  // Payment & DBT Analytics
  app.get(['/api/v1/reports/payment-analytics', '/api/reports/payment-analytics'], (req, res) => {
    const paidBookings = bookings.filter((b) => b.paymentStatus === 'COMPLETED' || b.paymentStatus === 'CREDITED_TO_BANK');
    const pendingBookings = bookings.filter((b) => b.paymentStatus !== 'COMPLETED' && b.paymentStatus !== 'CREDITED_TO_BANK');

    const totalSettled = paidBookings.reduce((acc, b) => acc + (b.totalPayoutRs || 0), 0);
    const totalPending = pendingBookings.reduce((acc, b) => acc + (b.totalPayoutRs || 0), 0);

    const delayedPayments = pendingBookings
      .filter((b) => b.status === 'PROCUREMENT_COMPLETED' || b.status === 'PAYMENT_PROCESSING')
      .map((b) => ({
        bookingId: b.id,
        tokenNumber: b.tokenNumber,
        farmerReference: `${b.farmerName} (${b.farmerPhone})`,
        maskedAadhar: b.farmerAadhar || 'XXXX-XXXX-4589',
        mandiId: b.mandiCenterId,
        mandiName: b.mandiCenterName,
        netPayableAmount: b.totalPayoutRs || 85000,
        paymentStatus: b.paymentStatus,
        completedAt: b.completedAt || b.createdAt,
        delayHours: 32,
        maskedAccount: `XXXX-XXXX-${b.bankAccountLast4 || '4921'}`,
      }));

    res.json({
      success: true,
      data: {
        totalDbtInitiated: bookings.length,
        totalDbtCompleted: paidBookings.length,
        totalDbtPending: pendingBookings.length,
        totalDbtFailed: 0,
        totalAmountSettledRs: Math.round(totalSettled),
        totalAmountPendingRs: Math.round(totalPending),
        averageSettlementHours: 3.8,
        delayedPayments,
      },
    });
  });

  // Time-Series Trend
  app.get(['/api/v1/reports/time-series', '/api/reports/time-series'], (req, res) => {
    const days = parseInt(req.query.days as string, 10) || 7;
    const points = [];
    const now = new Date();

    for (let i = days - 1; i >= 0; i--) {
      const d = new Date(now.getTime() - i * 86400000);
      const label = d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
      // Count bookings matching day or simulate trend
      const count = bookings.filter((b) => {
        const bd = new Date(b.createdAt || b.scheduledDate);
        return bd.toDateString() === d.toDateString();
      }).length;

      points.push({
        periodLabel: label,
        bookings: count + Math.floor(Math.random() * 8) + 12,
        completedProcurements: Math.max(0, count + Math.floor(Math.random() * 6) + 10),
        certifiedQuantityQuintals: Math.floor(Math.random() * 300) + 750,
        totalPayoutRs: (Math.floor(Math.random() * 300) + 750) * 2425,
        farmersServed: count + 8,
      });
    }

    res.json({ success: true, data: points });
  });

  // Full Procurement Register Export (CSV & JSON)
  app.get(['/api/v1/reports/procurement-register', '/api/reports/procurement-register'], (req, res) => {
    const { district, mandiId, cropId } = req.query;
    let list = bookings;
    if (district && typeof district === 'string') {
      list = list.filter((b) => (b.district || '').toLowerCase() === district.toLowerCase());
    }
    if (mandiId && typeof mandiId === 'string') {
      list = list.filter((b) => b.mandiCenterId === mandiId);
    }
    if (cropId && typeof cropId === 'string') {
      list = list.filter((b) => (b.cropName || '').toLowerCase().includes(cropId.toLowerCase()));
    }

    const rows = list.map((b) => ({
      bookingId: b.id,
      tokenNumber: b.tokenNumber,
      tokenSequence: b.tokenSequence || 1,
      scheduledDate: b.scheduledDate,
      farmerName: b.farmerName,
      farmerPhone: b.farmerPhone,
      maskedAadhar: b.farmerAadhar || 'XXXX-XXXX-4589',
      district: b.district,
      mandiName: b.mandiCenterName,
      cropName: b.cropName,
      estimatedYieldQuintals: b.estimatedYieldQuintals || 50,
      netWeightQuintals: b.netWeightQuintals || b.estimatedYieldQuintals || 50,
      moisturePercentage: b.moisturePct || 11.4,
      foreignMatterPercentage: b.foreignMatterPct || 0.65,
      totalPayoutRs: b.totalPayoutRs || 121250,
      status: b.status,
      paymentStatus: b.paymentStatus,
      dbtReferenceNo: b.dbtReferenceNo || 'DBT-MP-2026-89412',
      bankAccountLast4: `XXXX${b.bankAccountLast4 || '4921'}`,
      ifscCode: b.bankIfsc || 'SBIN0001234',
      completedAt: b.completedAt || b.createdAt,
    }));

    res.json({ success: true, data: rows });
  });

  app.get(['/api/v1/reports/export/csv', '/api/reports/export/csv'], (req, res) => {
    const rows = bookings.map((b) => [
      b.tokenNumber,
      b.scheduledDate,
      `"${(b.farmerName || '').replace(/"/g, '""')}"`,
      b.farmerPhone,
      b.farmerAadhar || 'XXXX-XXXX-4589',
      `"${(b.district || '').replace(/"/g, '""')}"`,
      `"${(b.mandiCenterName || '').replace(/"/g, '""')}"`,
      `"${(b.cropName || '').replace(/"/g, '""')}"`,
      b.estimatedYieldQuintals || 50,
      b.netWeightQuintals || b.estimatedYieldQuintals || 50,
      b.moisturePct || 11.4,
      b.foreignMatterPct || 0.65,
      b.totalPayoutRs || 121250,
      b.status,
      b.paymentStatus,
      b.dbtReferenceNo || 'DBT-MP-2026-89412',
      `XXXX${b.bankAccountLast4 || '4921'}`,
      b.bankIfsc || 'SBIN0001234',
      b.completedAt || b.createdAt,
    ].join(','));

    const csvHeader = 'Token Number,Scheduled Date,Farmer Name,Phone,Masked Aadhaar,District,Mandi,Crop,Booked Qtl,Net Weight Qtl,Moisture %,Foreign Matter %,Payout Rs,Status,DBT Status,DBT Ref,Bank Account,IFSC,Completed At\n';
    const csvContent = csvHeader + rows.join('\n');

    res.setHeader('Content-Type', 'text/csv; charset=utf-8');
    res.setHeader('Content-Disposition', 'attachment; filename="kisansarthi_procurement_register.csv"');
    res.send(csvContent);
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

  // Listen on port 3000 (Mandatory for AI Studio nginx reverse proxy)
  server.listen(PORT, '0.0.0.0', () => {
    console.log(`KisanSarthi Unified Server running on http://0.0.0.0:${PORT} with WebSocket on /ws`);
  });

  // Support environments with dynamic $PORT (e.g. standalone Cloud Run containers without nginx)
  const envPort = process.env.PORT ? parseInt(process.env.PORT, 10) : null;
  if (envPort && !isNaN(envPort) && envPort !== PORT) {
    try {
      const prodIngress = http.createServer(app);
      prodIngress.on('upgrade', (request, socket, head) => {
        if (request.url?.startsWith('/ws')) {
          wss.handleUpgrade(request, socket, head, (ws) => {
            wss.emit('connection', ws, request);
          });
        }
      });
      prodIngress.on('error', (err: { code?: string; message?: string }) => {
        if (err?.code === 'EADDRINUSE') {
          console.log(`Port ${envPort} is managed by upstream ingress reverse proxy; routing to port ${PORT}`);
        } else {
          console.warn(`Dynamic port ${envPort} listener notice:`, err?.message || err);
        }
      });
      prodIngress.listen(envPort, '0.0.0.0', () => {
        console.log(`Dynamic Cloud Run listener active on http://0.0.0.0:${envPort}`);
      });
    } catch (err: unknown) {
      console.warn('Dynamic port listener init notice:', err);
    }
  }
}

startServer();
