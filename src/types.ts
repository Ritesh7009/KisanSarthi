export type Language = 'en' | 'hi' | 'mal';

export type UserRole = 'FARMER' | 'ADMIN';

export interface CropInfo {
  id: string;
  name: string;
  hindiName: string;
  malwiName?: string;
  regionalName?: string;
  season: 'Rabi' | 'Kharif' | 'Zaid';
  standardMspPerQuintal: number;
  mpBonusPerQuintal?: number;
  stateBonusPerQuintal?: number;
  totalMsp: number;
  marketPricePerQuintal: number;
  typicalCostPerAcre: number;
  averageYieldPerAcreQuintal: number;
  moistureLimitPct: number;
  gradeSpecs: string;
  icon: string;
  majorStates?: string[];
}

export interface MandiCenter {
  id: string;
  name: string;
  hindiName: string;
  state: string;
  hindiState?: string;
  district: string;
  hindiDistrict: string;
  address: string;
  pinCode: string;
  openTime: string;
  closeTime: string;
  weighbridgesCount: number;
  dailyCapacityQuintals: number;
  totalCapacityKg: number;
  reservedCapacityKg: number;
  procuredCapacityKg: number;
  availableCapacityKg: number;
  occupiedCapacityKg: number;
  currentTokenServing: number;
  totalTokensToday: number;
  activeTokensWaiting: number;
  averageProcessingMins: number;
  gateStatus: 'OPEN' | 'CONGESTED' | 'RAIN_DELAY' | 'CLOSED';
  phone: string;
  commoditiesHandled: string[];
  lat: number;
  lng: number;
  eNamCode?: string;
  isENamMandi?: boolean;
}

export type BookingStatus =
  | 'BOOKED'
  | 'GATE_CALLED'
  | 'GATE_ENTERED'
  | 'WEIGHBRIDGE_GROSS'
  | 'QC_INSPECTION'
  | 'UNLOADING'
  | 'WEIGHBRIDGE_TARE'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REJECTED'
  | 'NO_SHOW';

export type PaymentStatus =
  | 'PENDING'
  | 'VERIFIED'
  | 'DBT_INITIATED'
  | 'CREDITED_TO_BANK';

export interface SlotBooking {
  id: string;
  tokenNumber: string;
  tokenSequence: number;
  farmerId: string;
  farmerName: string;
  farmerPhone: string;
  farmerAadhar?: string;
  state?: string;
  district: string;
  village: string;
  mandiCenterId: string;
  mandiCenterName: string;
  cropId: string;
  cropName: string;
  estimatedYieldQuintals: number;
  requestedYieldKg?: number;
  acreage: number;
  harvestDate: string;
  scheduledDate: string;
  timeSlot: string;
  vehicleType: 'TRACTOR_TROLLEY' | 'PICKUP_TRUCK' | 'BULLOCK_CART' | 'TRUCK';
  vehicleNumber: string;
  status: BookingStatus;
  aiRecommended?: boolean;
  aiReasoning?: string;
  waitTimeEstimateMins: number;
  qrCodeData: string;
  createdAt: string;
  cancelledAt?: string;
  completedAt?: string;
  rejectionReason?: string;
  
  // Fulfillment & QC fields
  gatePassTime?: string;
  actualGrossWeightKg?: number;
  actualTareWeightKg?: number;
  netWeightQuintals?: number;
  moisturePct?: number;
  foreignMatterPct?: number;
  qualityGrade?: 'Grade A' | 'Grade B' | 'Standard Fair Average (FAQ)';
  totalPayoutRs?: number;
  paymentStatus: PaymentStatus;
  utrNumber?: string;
  bankAccountLast4?: string;
  syncedOffline?: boolean;
}

export interface TimeSlotConfig {
  timeSlot: string;
  maxCapacityQuintals: number;
  maxCapacityKg: number;
  maxVehicles: number;
  bookedVehicles: number;
  bookedQuantityKg: number;
  availableQuantityKg: number;
  availableVehicles: number;
  status: 'OPEN' | 'LIMITED' | 'FULL' | 'WEATHER_HOLD';
  estimatedWaitMins: number;
}

export interface AuditLogItem {
  id: string;
  timestamp: string;
  action: string;
  entityType: 'BOOKING' | 'MANDI' | 'SLOT' | 'MSP' | 'WEIGHBRIDGE' | 'QUEUE' | 'SYSTEM';
  entityId: string;
  actor: string;
  details: string;
  previousState?: string;
  newState?: string;
  quantityKgDelta?: number;
  mandiId?: string;
}

export interface NotificationItem {
  id: string;
  recipientPhone?: string;
  type: 'SMS' | 'PUSH' | 'ALERT';
  senderTag: string; // e.g. "VK-EUPARJAN", "MD-MANDI"
  title: string;
  hindiTitle: string;
  message: string;
  hindiMessage: string;
  timestamp: string;
  read: boolean;
  category: 'SLOT' | 'QUEUE' | 'PAYMENT' | 'WEATHER';
  tokenRef?: string;
}

export interface WeatherAlert {
  id: string;
  district: string;
  date: string;
  tempCelsius: number;
  humidityPct: number;
  rainfallChancePct: number;
  condition: string;
  hindiCondition: string;
  severity: 'NORMAL' | 'WARNING' | 'CRITICAL';
  advisory: string;
  hindiAdvisory: string;
}

export interface DistrictProcurementStat {
  state: string;
  hindiState?: string;
  district: string;
  hindiDistrict: string;
  totalProcuredQuintals: number;
  targetQuintals: number;
  warehouseCapacityQuintals: number;
  warehouseOccupiedQuintals: number;
  dbtDisbursedCrores: number;
  activeCentersCount: number;
  farmersRegistered: number;
  topCrop: string;
}

export interface AISlotSuggestion {
  recommendedSlot: string;
  recommendedDate: string;
  estimatedWaitTimeMinutes: number;
  congestionScore: 'LOW' | 'MODERATE' | 'HIGH';
  reasons: string[];
  weatherWarning?: string;
  projectedProfitPerQuintal?: number;
}

export interface AIYieldAnalysis {
  predictedYieldQuintals: number;
  estimatedRevenue: number;
  estimatedProfit: number;
  profitMarginPercent: number;
  keyRecommendations: string[];
  soilHealthTips: string[];
  harvestWindowAdvice: string;
}

export interface FarmerProfile {
  id: string;
  kisanId?: string;
  name: string;
  hindiName?: string;
  phone: string;
  aadharNumber: string; // 12-digit UID
  maskedAadhar: string; // e.g. "XXXX-XXXX-4589"
  state?: string;
  district: string;
  village: string;
  landSizeAcres: number;
  bankAccountLast4?: string;
  ifscCode?: string;
  registeredAt: string;
  lastLoginAt: string;
  loginCount: number;
}

export interface SmsLogItem {
  id: string;
  recipientPhone: string;
  farmerName: string;
  aadharMasked?: string;
  message: string;
  senderHeader: string; // e.g. "VK-EUPARJAN"
  dltTemplateId: string;
  status: 'DELIVERED' | 'SENT' | 'FAILED' | 'PENDING';
  dispatchedAt: string;
  dispatchedBy: string; // e.g. "Admin (Mandi Secretary, Sehore)"
  channel: 'SMS_GATEWAY' | 'WEB_SMS' | 'SIM_DIRECT';
  deliveryReceiptId: string;
}

