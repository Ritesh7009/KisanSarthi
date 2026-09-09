/**
 * KisanSarthi Canonical REST API Client
 * Strict /api/v1/* contract with centralized error handling,
 * typed request/response models, and Idempotency-Key support.
 */

import {
  CropInfo,
  MandiCenter,
  SlotBooking,
  FarmerProfile,
  SmsLogItem,
  TimeSlotConfig,
  NotificationItem,
  WeatherAlert,
  DistrictProcurementStat,
  AISlotSuggestion,
  AIYieldAnalysis,
} from '../types';

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp?: string;
  error?: string;
  errors?: Record<string, string>;
}

export interface AuthSession {
  accessToken: string;
  refreshToken?: string;
  user: {
    id: string;
    name: string;
    phone: string;
    role: 'FARMER' | 'ADMIN' | 'MANDI_OPERATOR' | 'MANDI_MANAGER' | 'DISTRICT_OFFICER';
    maskedAadhar?: string;
    district: string;
    village?: string;
    mandiId?: string;
  };
}

export interface QueueCallResult {
  mandiId: string;
  currentTokenServing: number;
  calledToken: number;
  activeTokensWaiting: number;
  waitingCount: number;
  calledBookingId?: string;
  matchedBooking?: SlotBooking | null;
  timestamp: string;
}

export interface CreateBookingPayload {
  farmerId: string;
  farmerName: string;
  farmerPhone: string;
  farmerAadhar?: string;
  district: string;
  village?: string;
  mandiCenterId: string;
  mandiCenterName: string;
  cropId: string;
  cropName: string;
  estimatedYieldQuintals: number;
  acreage?: number;
  harvestDate?: string;
  scheduledDate: string;
  timeSlot: string;
  vehicleType: string;
  vehicleNumber: string;
  bankAccountLast4?: string;
  aiRecommended?: boolean;
  aiReasoning?: string;
  waitTimeEstimateMins?: number;
}

// API base URL: uses VITE_API_BASE_URL when provided, defaulting to https://kisansarthi-vsne.onrender.com.
const API_BASE_URL = ((import.meta as any).env?.VITE_API_BASE_URL || 'https://kisansarthi-vsne.onrender.com').replace(/\/$/, '');

export const apiUrl = (endpoint: string): string => {
  if (/^https?:\/\//i.test(endpoint)) return endpoint;
  return `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;
};

// Token storage helpers
let authToken: string | null = null;

export const setAuthToken = (token: string | null) => {
  authToken = token;
  if (token) {
    try {
      sessionStorage.setItem('ks_auth_token', token);
    } catch {}
  } else {
    try {
      sessionStorage.removeItem('ks_auth_token');
    } catch {}
  }
};

export const getAuthToken = (): string | null => {
  try {
    if (typeof sessionStorage !== 'undefined') {
      const stored = sessionStorage.getItem('ks_auth_token');
      if (stored) {
        authToken = stored;
        return stored;
      }
    }
  } catch {}
  return authToken;
};

// Generic Request Runner
async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const headers = new Headers(options.headers || {});
  
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  
  const token = getAuthToken();
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(apiUrl(endpoint), {
    ...options,
    headers,
  });

  const isJson = response.headers.get('content-type')?.includes('application/json');
  const body = isJson ? await response.json() : null;

  if (!response.ok) {
    const errorMsg = body?.message || body?.error || `HTTP ${response.status}: ${response.statusText}`;
    throw new Error(errorMsg);
  }

  // Handle standard { success: true, data: ... } format or raw objects
  if (body && typeof body === 'object' && 'data' in body && body.data !== undefined) {
    return body.data as T;
  }

  return body as T;
}

// ==========================================
// 1. AUTH API
// ==========================================
export const authApi = {
  async sendOtp(phone: string): Promise<{ success: boolean; message: string; cooldownSeconds?: number }> {
    return request('/api/v1/auth/send-otp', {
      method: 'POST',
      body: JSON.stringify({ phone }),
    });
  },

  async verifyOtp(phone: string, otp: string): Promise<{ success: boolean; farmer?: FarmerProfile; token?: string; data?: any }> {
    const res = await request<any>(
      '/api/v1/auth/verify-otp',
      {
        method: 'POST',
        body: JSON.stringify({ phone, otp }),
      }
    );
    const token = res.data?.accessToken || res.accessToken || res.token;
    if (token) {
      setAuthToken(token);
    }
    return res;
  },

  async adminLogin(officerId: string, passcode: string, mandiId?: string): Promise<{ success: boolean; admin?: any; data?: any; token?: string }> {
    const res = await request<any>(
      '/api/v1/auth/admin-login',
      {
        method: 'POST',
        body: JSON.stringify({ username: officerId, password: passcode }),
      }
    );
    const token = res.data?.accessToken || res.accessToken || res.token;
    if (token) {
      setAuthToken(token);
    }
    return res;
  },

  async refreshToken(refreshToken: string): Promise<{ accessToken: string }> {
    return request('/api/v1/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    });
  },

  async logout(): Promise<void> {
    try {
      await request('/api/v1/auth/logout', { method: 'POST' });
    } finally {
      setAuthToken(null);
    }
  },
};

// ==========================================
// 2. FARMERS API
// ==========================================
export const farmerApi = {
  async getMe(): Promise<FarmerProfile> {
    return request<FarmerProfile>('/api/v1/farmers/me');
  },

  async updateMe(data: Partial<FarmerProfile>): Promise<FarmerProfile> {
    return request<FarmerProfile>('/api/v1/farmers/me', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  async getAllFarmers(district?: string, search?: string): Promise<FarmerProfile[]> {
    const params = new URLSearchParams();
    if (district) params.append('district', district);
    if (search) params.append('search', search);
    const qs = params.toString() ? `?${params.toString()}` : '';
    const res = await request<{ farmers: FarmerProfile[] } | FarmerProfile[]>(`/api/v1/farmers${qs}`);
    return Array.isArray(res) ? res : res.farmers || [];
  },
};

// ==========================================
// 3. MANDIS API
// ==========================================
export const mandiApi = {
  async getAllMandis(district?: string): Promise<MandiCenter[]> {
    const qs = district ? `?district=${encodeURIComponent(district)}` : '';
    const res = await request<{ mandis: MandiCenter[] } | MandiCenter[]>(`/api/v1/mandis${qs}`);
    return Array.isArray(res) ? res : res.mandis || [];
  },

  async getMandiById(id: string): Promise<MandiCenter> {
    const res = await request<{ mandi: MandiCenter } | MandiCenter>(`/api/v1/mandis/${id}`);
    return (res as any).mandi || res;
  },

  async getMandiStatus(id: string): Promise<any> {
    return request(`/api/v1/mandis/${id}/status`);
  },
};

// ==========================================
// 4. CROPS API
// ==========================================
export const cropApi = {
  async getAllCrops(): Promise<CropInfo[]> {
    const res = await request<{ crops: CropInfo[] } | CropInfo[]>('/api/v1/crops');
    return Array.isArray(res) ? res : res.crops || [];
  },

  async updateMsp(
    cropId: string,
    payload: { standardMspPerQuintal?: number; mpBonusPerQuintal?: number; standardMsp?: number; mpBonus?: number }
  ): Promise<CropInfo> {
    const std = payload.standardMspPerQuintal ?? payload.standardMsp ?? 0;
    const bonus = payload.mpBonusPerQuintal ?? payload.mpBonus ?? 0;

    const res = await request<{ crop: CropInfo } | CropInfo>(`/api/v1/crops/${cropId}/msp`, {
      method: 'PUT',
      body: JSON.stringify({
        standardMspPerQuintal: std,
        mpBonusPerQuintal: bonus,
        standardMsp: std,
        mpBonus: bonus,
      }),
    });
    return (res as any).crop || res;
  },
};

// ==========================================
// 5. SLOTS API
// ==========================================
export const slotApi = {
  async getSlots(mandiId?: string): Promise<TimeSlotConfig[]> {
    const url = mandiId ? `/api/v1/mandis/${mandiId}/slots` : '/api/v1/slots';
    const res = await request<{ slots: TimeSlotConfig[] } | TimeSlotConfig[]>(url);
    return Array.isArray(res) ? res : res.slots || [];
  },

  async updateSlotCapacity(timeSlot: string, maxVehicles: number, status: string): Promise<TimeSlotConfig[]> {
    const res = await request<{ slots: TimeSlotConfig[] } | TimeSlotConfig[]>('/api/v1/slots/capacity', {
      method: 'PATCH',
      body: JSON.stringify({ timeSlot, maxVehicles, status }),
    });
    return Array.isArray(res) ? res : res.slots || [];
  },
};

// ==========================================
// 6. BOOKINGS API
// ==========================================
export const bookingApi = {
  async createBooking(payload: CreateBookingPayload, idempotencyKey?: string): Promise<SlotBooking> {
    const headers: Record<string, string> = {};
    if (idempotencyKey) {
      headers['Idempotency-Key'] = idempotencyKey;
    }

    const res = await request<{ booking: SlotBooking } | SlotBooking>('/api/v1/bookings', {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    });
    return (res as any).booking || res;
  },

  async getBookings(params?: { farmerId?: string; mandiId?: string; phone?: string }): Promise<SlotBooking[]> {
    const sp = new URLSearchParams();
    if (params?.farmerId) sp.append('farmerId', params.farmerId);
    if (params?.mandiId) sp.append('mandiId', params.mandiId);
    if (params?.phone) sp.append('phone', params.phone);
    const qs = sp.toString() ? `?${sp.toString()}` : '';

    const res = await request<{ bookings: SlotBooking[] } | SlotBooking[]>(`/api/v1/bookings${qs}`);
    return Array.isArray(res) ? res : res.bookings || [];
  },

  async getMyBookings(): Promise<SlotBooking[]> {
    const res = await request<{ bookings: SlotBooking[] } | SlotBooking[]>('/api/v1/bookings/my');
    return Array.isArray(res) ? res : res.bookings || [];
  },

  async getBookingById(id: string): Promise<SlotBooking> {
    const res = await request<{ booking: SlotBooking } | SlotBooking>(`/api/v1/bookings/${id}`);
    return (res as any).booking || res;
  },

  async updateBookingStatus(id: string, updates: Partial<SlotBooking>): Promise<SlotBooking> {
    const res = await request<{ booking: SlotBooking } | SlotBooking>(`/api/v1/bookings/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify(updates),
    });
    return (res as any).booking || res;
  },

  async cancelBooking(id: string): Promise<SlotBooking> {
    const res = await request<{ booking: SlotBooking } | SlotBooking>(`/api/v1/bookings/${id}/cancel`, {
      method: 'POST',
    });
    return (res as any).booking || res;
  },
};

// ==========================================
// 7. QUEUE API
// ==========================================
export const queueApi = {
  async getMandiQueue(mandiId: string): Promise<any> {
    return request(`/api/v1/mandis/${mandiId}/queue`);
  },

  async callNextToken(mandiId: string): Promise<QueueCallResult> {
    const res = await request<any>(`/api/v1/mandis/${mandiId}/queue/next`, {
      method: 'POST',
    });
    return {
      mandiId,
      currentTokenServing: res.currentTokenServing ?? res.calledToken ?? 0,
      calledToken: res.calledToken ?? res.currentTokenServing ?? 0,
      activeTokensWaiting: res.activeTokensWaiting ?? res.waitingCount ?? 0,
      waitingCount: res.waitingCount ?? res.activeTokensWaiting ?? 0,
      calledBookingId: res.calledBookingId || res.matchedBooking?.id,
      matchedBooking: res.matchedBooking || null,
      timestamp: res.timestamp || new Date().toISOString(),
    };
  },
};

// ==========================================
// 8. NOTIFICATIONS & SMS API
// ==========================================
export const notificationApi = {
  async getMyNotifications(): Promise<NotificationItem[]> {
    const res = await request<{ notifications: NotificationItem[] } | NotificationItem[]>(
      '/api/v1/farmers/me/notifications'
    );
    return Array.isArray(res) ? res : res.notifications || [];
  },

  async getSmsLogs(phone?: string): Promise<SmsLogItem[]> {
    const qs = phone ? `?phone=${encodeURIComponent(phone)}` : '';
    const res = await request<{ logs: SmsLogItem[] } | SmsLogItem[]>(`/api/v1/sms/logs${qs}`);
    return Array.isArray(res) ? res : res.logs || [];
  },

  async sendSms(data: {
    recipientPhone: string;
    farmerName: string;
    message: string;
    aadharMasked?: string;
    senderHeader?: string;
    dltTemplateId?: string;
  }): Promise<SmsLogItem> {
    return request<SmsLogItem>('/api/v1/sms/send', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

// ==========================================
// 9. WEIGHMENT & PAYMENTS API
// ==========================================
export const weighmentApi = {
  async recordWeighment(bookingId: string, data: any): Promise<any> {
    return request(`/api/v1/bookings/${bookingId}/weighment`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async getWeighment(bookingId: string): Promise<any> {
    return request(`/api/v1/bookings/${bookingId}/weighment`);
  },
};

export const paymentApi = {
  async getPayment(bookingId: string): Promise<any> {
    return request(`/api/v1/bookings/${bookingId}/payment`);
  },

  async initiatePayment(bookingId: string, data: any): Promise<any> {
    return request(`/api/v1/bookings/${bookingId}/payment`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

// ==========================================
// 10. WEATHER API
// ==========================================
export const weatherApi = {
  async getWeather(district: string): Promise<WeatherAlert> {
    const res = await request<{ weather: WeatherAlert } | WeatherAlert>(
      `/api/v1/weather/${encodeURIComponent(district)}`
    );
    return (res as any).weather || res;
  },
};

// ==========================================
// 11. AI SERVICES API
// ==========================================
export const aiApi = {
  async getSlotSuggestion(payload: any): Promise<AISlotSuggestion> {
    return request<AISlotSuggestion>('/api/v1/ai/slot-suggestion', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  async slotRecommendation(payload: any): Promise<AISlotSuggestion> {
    return request<AISlotSuggestion>('/api/v1/ai/slot-suggestion', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  async getYieldAnalysis(payload: any): Promise<AIYieldAnalysis> {
    return request<AIYieldAnalysis>('/api/v1/ai/yield-analysis', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  async yieldAdvisor(payload: any): Promise<AIYieldAnalysis> {
    return request<AIYieldAnalysis>('/api/v1/ai/yield-analysis', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
};

// ==========================================
// 12. REPORTS API
// ==========================================
export const reportApi = {
  async getDistrictStats(district?: string): Promise<DistrictProcurementStat[]> {
    const qs = district ? `?district=${encodeURIComponent(district)}` : '';
    const res = await request<{ stats: DistrictProcurementStat[] } | DistrictProcurementStat[]>(`/api/v1/reports/district-stats${qs}`);
    return Array.isArray(res) ? res : res.stats || [];
  },
};
