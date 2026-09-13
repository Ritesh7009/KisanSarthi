export interface StatewideOverview {
  totalBookings: number;
  totalCompletedProcurements: number;
  totalCertifiedQuantityQuintals: number;
  totalProcurementValueRs: number;
  totalDbtDisbursedRs: number;
  totalFarmersServed: number;
  totalActiveMandis: number;
  totalWaitingFarmers: number;
  statusBreakdown: Record<string, number>;
}

export interface MandiPerformanceItem {
  mandiId: string;
  mandiName: string;
  hindiName: string;
  district: string;
  totalBookings: number;
  completedProcurements: number;
  certifiedQuantityQuintals: number;
  remainingSlotCapacityQuintals: number;
  averageProcessingMins: number;
  estimatedWaitTimeMins: number;
  currentQueueLength: number;
  throughputPerHour: number;
  totalDbtAmountRs: number;
  status: 'AVAILABLE' | 'BUSY' | 'HIGH_LOAD' | 'FULL' | 'CLOSED';
  bottleneckReason?: string;
}

export interface BottleneckAlert {
  mandiId: string;
  mandiName: string;
  district: string;
  severity: 'LOW' | 'MEDIUM' | 'CRITICAL';
  indicator: 'QUEUE_CONGESTION' | 'HIGH_WAIT_TIME' | 'CAPACITY_PRESSURE' | 'MOISTURE_SPIKE' | 'PAYMENT_DELAY';
  metricDescription: string;
  reason: string;
  waitingFarmers: number;
  estimatedWaitMinutes: number;
  remainingSlotCapacityQuintals: number;
}

export interface CropProcurementReport {
  cropId: string;
  cropName: string;
  hindiName: string;
  season: string;
  totalBookings: number;
  completedProcurements: number;
  certifiedQuantityQuintals: number;
  totalProcurementValueRs: number;
  farmersServed: number;
  averageQuantityPerFarmerQuintals: number;
  totalDbtDisbursedRs: number;
}

export interface QualityAndWeighmentReport {
  totalVehiclesWeighed: number;
  totalGrossQuintals: number;
  totalTareQuintals: number;
  totalCertifiedNetQuintals: number;
  averageNetQuintalsPerVehicle: number;
  averageMoisturePct: number;
  minMoisturePct: number;
  maxMoisturePct: number;
  samplesWithinFaqThreshold: number;
  samplesAboveFaqThreshold: number;
  percentAboveFaqThreshold: number;
  averageForeignMatterPct: number;
  totalDockageQuintals: number;
  dockagePercentage: number;
}

export interface PaymentDelayAlert {
  bookingId: string;
  tokenNumber: string;
  farmerReference: string;
  maskedAadhar: string;
  mandiId: string;
  mandiName: string;
  netPayableAmount: number;
  paymentStatus: string;
  initiatedAt?: string;
  completedAt?: string;
  delayHours: number;
  maskedAccount: string;
}

export interface PaymentAnalyticsReport {
  totalDbtInitiated: number;
  totalDbtCompleted: number;
  totalDbtPending: number;
  totalDbtFailed: number;
  totalAmountSettledRs: number;
  totalAmountPendingRs: number;
  averageSettlementHours: number;
  totalDelayedPaymentsCount?: number;
  totalDelayedAmountRs?: number;
  delayedPayments: PaymentDelayAlert[];
}

export interface TimeSeriesPoint {
  periodLabel: string;
  bookings: number;
  completedProcurements: number;
  certifiedQuantityQuintals: number;
  totalPayoutRs: number;
  farmersServed: number;
}

export interface ProcurementRegisterRow {
  bookingId: string;
  tokenNumber: string;
  tokenSequence: number;
  scheduledDate: string;
  farmerName: string;
  farmerPhone: string;
  maskedAadhar: string;
  district: string;
  mandiName: string;
  cropName: string;
  estimatedYieldQuintals: number;
  netWeightQuintals: number;
  moisturePercentage: number;
  foreignMatterPercentage: number;
  totalPayoutRs: number;
  status: string;
  paymentStatus: string;
  dbtReferenceNo?: string;
  bankAccountLast4: string;
  ifscCode: string;
  completedAt?: string;
}

export interface PaginatedProcurementRegister {
  content: ProcurementRegisterRow[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

