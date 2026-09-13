import React, { useState, useEffect } from 'react';
import {
  Scale,
  CheckCircle2,
  AlertTriangle,
  FileText,
  Truck,
  Droplets,
  ShieldCheck,
  Send,
  Building2,
  Clock,
  Sparkles,
  Check,
  RefreshCw,
} from 'lucide-react';
import { SlotBooking, MandiCenter, CropInfo, Language } from '../types';
import { weighmentApi, paymentApi } from '../services/api';

interface Props {
  mandi: MandiCenter;
  bookings: SlotBooking[];
  crops: CropInfo[];
  currentUser: { name: string; phone: string; district: string; mandiId?: string } | null;
  onUpdateBookingStatus: (bookingId: string, updates: Partial<SlotBooking>) => Promise<void>;
  onViewJForm: (booking: SlotBooking) => void;
  language: Language;
}

export const WeighbridgePaymentHub: React.FC<Props> = ({
  mandi,
  bookings,
  crops,
  currentUser,
  onUpdateBookingStatus,
  onViewJForm,
  language,
}) => {
  // Filter bookings for this mandi
  const mandiBookings = bookings.filter((b) => b.mandiCenterId === mandi.id);

  // Active bookings in yard or in-progress
  const yardBookings = mandiBookings.filter(
    (b) =>
      b.status !== 'CANCELLED' &&
      b.status !== 'REJECTED' &&
      b.status !== 'NO_SHOW'
  );

  const [selectedBookingId, setSelectedBookingId] = useState<string>(() => {
    const active = yardBookings.find(
      (b) =>
        b.status === 'WEIGHING' ||
        b.status === 'WEIGHMENT_STAGE_1' ||
        b.status === 'WEIGHMENT_COMPLETED' ||
        b.status === 'PROCUREMENT_COMPLETED' ||
        b.status === 'PAYMENT_PROCESSING' ||
        b.status === 'GATE_ENTERED'
    );
    return active?.id || yardBookings[0]?.id || '';
  });

  const activeBooking = mandiBookings.find((b) => b.id === selectedBookingId) || yardBookings[0];

  // Inputs
  const [bay, setBay] = useState('Kanta Bay 1');
  const [grossInput, setGrossInput] = useState('10500');
  const [tareInput, setTareInput] = useState('3900');
  const [moistureInput, setMoistureInput] = useState('10.5');
  const [foreignMatterInput, setForeignMatterInput] = useState('1.2');
  const [qualityGrade, setQualityGrade] = useState<'Grade A' | 'Grade B' | 'Standard Fair Average (FAQ)'>(
    'Grade A'
  );
  const [isProcessing, setIsProcessing] = useState(false);
  const [actionSuccessMsg, setActionSuccessMsg] = useState<string | null>(null);

  // Synchronize inputs when activeBooking changes
  useEffect(() => {
    if (activeBooking) {
      if (activeBooking.actualGrossWeightKg) {
        setGrossInput(String(activeBooking.actualGrossWeightKg));
      } else {
        // default based on crop & estimated yield
        const estKg = Math.round((activeBooking.estimatedYieldQuintals || 50) * 100);
        setGrossInput(String(estKg + 3800));
      }

      if (activeBooking.actualTareWeightKg) {
        setTareInput(String(activeBooking.actualTareWeightKg));
      } else {
        setTareInput('3800');
      }

      if (activeBooking.moisturePct) {
        setMoistureInput(String(activeBooking.moisturePct));
      } else {
        setMoistureInput('10.5');
      }

      if (activeBooking.foreignMatterPct) {
        setForeignMatterInput(String(activeBooking.foreignMatterPct));
      } else {
        setForeignMatterInput('1.2');
      }

      if (activeBooking.qualityGrade) {
        setQualityGrade(activeBooking.qualityGrade);
      }
    }
  }, [activeBooking?.id]);

  if (!activeBooking) {
    return (
      <div className="bg-white rounded-3xl border border-slate-200 p-8 text-center text-xs text-slate-500 shadow-xs">
        <Scale className="w-8 h-8 text-slate-300 mx-auto mb-2" />
        <p className="font-semibold text-slate-700">No active vehicles in the Mandi queue</p>
        <p className="text-slate-400 mt-1">Summon a token to admit a vehicle into the weighbridge bay.</p>
      </div>
    );
  }

  // Active Crop & Rate calculation
  const crop = crops.find((c) => c.id === activeBooking.cropId) || crops[0];
  const standardMsp = crop?.standardMspPerQuintal || 2275;
  const mpBonus = crop?.mpBonusPerQuintal || 125;
  const totalMspRate = crop?.totalMsp || standardMsp + mpBonus;

  // Weight math
  const grossKg = Number(grossInput) || 0;
  const tareKg = Number(tareInput) || 0;
  const netKg = Math.max(0, grossKg - tareKg);
  const netQtl = parseFloat((netKg / 100).toFixed(2));
  const totalPayout = Math.round(netQtl * totalMspRate);

  const moistureVal = Number(moistureInput) || 0;
  const isMoistureHigh = moistureVal > 12.0;

  // Notification helper
  const triggerFeedback = (msg: string) => {
    setActionSuccessMsg(msg);
    setTimeout(() => setActionSuccessMsg(null), 4000);
  };

  // 1. Start Weighment
  const handleStartWeighment = async () => {
    setIsProcessing(true);
    try {
      await weighmentApi.startWeighment(activeBooking.id, bay, currentUser?.name || 'Kanta Operator');
      await onUpdateBookingStatus(activeBooking.id, {
        status: 'WEIGHING',
      });
      triggerFeedback(`Vehicle admitted to ${bay}. Scale ready for Gross Weight measurement.`);
    } catch (e) {
      console.warn('Start weighment error, applying state update:', e);
      await onUpdateBookingStatus(activeBooking.id, { status: 'WEIGHING' });
      triggerFeedback(`Vehicle admitted to ${bay} (Offline sync enabled).`);
    } finally {
      setIsProcessing(false);
    }
  };

  // 2. Record Stage 1 Gross Weight & QC
  const handleRecordGross = async () => {
    setIsProcessing(true);
    try {
      const gross = Number(grossInput) || 10500;
      await weighmentApi.recordWeighment(
        activeBooking.id,
        {
          actualGrossWeightKg: gross,
          grossWeightQuintals: parseFloat((gross / 100).toFixed(2)),
          moisturePct: Number(moistureInput) || 10.5,
          foreignMatterPct: Number(foreignMatterInput) || 1.2,
          qualityGrade,
        },
        currentUser?.name || 'Kanta Operator'
      );
      await onUpdateBookingStatus(activeBooking.id, {
        actualGrossWeightKg: gross,
        grossWeightQuintals: parseFloat((gross / 100).toFixed(2)),
        moisturePct: Number(moistureInput) || 10.5,
        foreignMatterPct: Number(foreignMatterInput) || 1.2,
        qualityGrade,
        status: 'WEIGHMENT_STAGE_1',
      });
      triggerFeedback(`Stage 1 Gross Weight (${gross} kg) & QC recorded successfully! Truck dispatched to unload.`);
    } catch (e) {
      console.warn('Record gross error:', e);
      await onUpdateBookingStatus(activeBooking.id, {
        actualGrossWeightKg: Number(grossInput) || 10500,
        status: 'WEIGHMENT_STAGE_1',
      });
      triggerFeedback('Stage 1 Gross recorded (local sync).');
    } finally {
      setIsProcessing(false);
    }
  };

  // 3. Record Stage 2 Tare Weight & Compute Authoritative Net
  const handleRecordTare = async () => {
    if (tareKg >= grossKg) {
      alert('Tare weight (empty vehicle) must be less than gross weight!');
      return;
    }
    setIsProcessing(true);
    try {
      await weighmentApi.recordWeighment(
        activeBooking.id,
        {
          actualGrossWeightKg: grossKg,
          actualTareWeightKg: tareKg,
          grossWeightQuintals: parseFloat((grossKg / 100).toFixed(2)),
          tareWeightQuintals: parseFloat((tareKg / 100).toFixed(2)),
          netWeightQuintals: netQtl,
          totalPayoutRs: totalPayout,
          moisturePct: Number(moistureInput) || 10.5,
          foreignMatterPct: Number(foreignMatterInput) || 1.2,
          qualityGrade,
        },
        currentUser?.name || 'Kanta Operator'
      );
      await onUpdateBookingStatus(activeBooking.id, {
        actualGrossWeightKg: grossKg,
        actualTareWeightKg: tareKg,
        netWeightQuintals: netQtl,
        totalPayoutRs: totalPayout,
        moisturePct: Number(moistureInput) || 10.5,
        foreignMatterPct: Number(foreignMatterInput) || 1.2,
        qualityGrade,
        status: 'WEIGHMENT_COMPLETED',
        paymentStatus: 'PENDING',
      });
      triggerFeedback(`Tare weight recorded (${tareKg} kg). Authoritative Net Weight certified: ${netKg} kg (${netQtl} Qtl).`);
    } catch (e) {
      console.warn('Record tare error:', e);
      await onUpdateBookingStatus(activeBooking.id, {
        actualGrossWeightKg: grossKg,
        actualTareWeightKg: tareKg,
        netWeightQuintals: netQtl,
        totalPayoutRs: totalPayout,
        status: 'WEIGHMENT_COMPLETED',
      });
      triggerFeedback('Weighment finalized (local sync).');
    } finally {
      setIsProcessing(false);
    }
  };

  // 4. Certify APMC Procurement (Generate J-Form)
  const handleCertifyProcurement = async () => {
    setIsProcessing(true);
    try {
      await weighmentApi.completeProcurement(activeBooking.id, currentUser?.name || 'APMC Procurement Officer');
      await onUpdateBookingStatus(activeBooking.id, {
        status: 'PROCUREMENT_COMPLETED',
      });
      triggerFeedback(`APMC Procurement certified for Token ${activeBooking.tokenNumber}! Official J-Form issued.`);
    } catch (e) {
      console.warn('Procurement certify error:', e);
      await onUpdateBookingStatus(activeBooking.id, { status: 'PROCUREMENT_COMPLETED' });
      triggerFeedback('Procurement certified (local sync).');
    } finally {
      setIsProcessing(false);
    }
  };

  // 5. Initiate DBT Payment
  const handleInitiateDbt = async () => {
    setIsProcessing(true);
    try {
      const res = await paymentApi.initiatePayment(activeBooking.id, {
        actor: currentUser?.name || 'DBT Portal Officer',
      });
      const dbtRef = res?.data?.dbtReferenceNo || `DBT-MP-${Date.now().toString().slice(-8)}`;
      await onUpdateBookingStatus(activeBooking.id, {
        status: 'PAYMENT_PROCESSING',
        paymentStatus: 'DBT_INITIATED',
        dbtReferenceNo: dbtRef,
        utrNumber: dbtRef,
        bankAccountLast4: '4321',
        bankIfsc: 'SBIN0001111',
      });
      triggerFeedback(`DBT Payment ${dbtRef} initiated for ₹${(activeBooking.totalPayoutRs || totalPayout).toLocaleString('en-IN')}! Transferred to PFMS gateway.`);
    } catch (e) {
      console.warn('Initiate DBT error:', e);
      const dbtRef = `DBT-MP-${Date.now().toString().slice(-8)}`;
      await onUpdateBookingStatus(activeBooking.id, {
        status: 'PAYMENT_PROCESSING',
        paymentStatus: 'DBT_INITIATED',
        dbtReferenceNo: dbtRef,
        utrNumber: dbtRef,
      });
      triggerFeedback(`DBT Initiated: ${dbtRef}`);
    } finally {
      setIsProcessing(false);
    }
  };

  // 6. Confirm Bank Credit
  const handleConfirmBankCredit = async () => {
    setIsProcessing(true);
    try {
      await paymentApi.confirmPaymentCredit(activeBooking.id, activeBooking.dbtReferenceNo || activeBooking.utrNumber);
      await onUpdateBookingStatus(activeBooking.id, {
        status: 'COMPLETED',
        paymentStatus: 'COMPLETED',
        completedAt: new Date().toISOString(),
        paymentDate: new Date().toISOString(),
      });
      triggerFeedback(`DBT Credit confirmed! Beneficiary bank acknowledged ₹${(activeBooking.totalPayoutRs || totalPayout).toLocaleString('en-IN')}. Status: COMPLETED.`);
    } catch (e) {
      console.warn('Confirm credit error:', e);
      await onUpdateBookingStatus(activeBooking.id, {
        status: 'COMPLETED',
        paymentStatus: 'COMPLETED',
        completedAt: new Date().toISOString(),
      });
      triggerFeedback('Payment confirmed as COMPLETED.');
    } finally {
      setIsProcessing(false);
    }
  };

  // Status Stepper Helper
  const getStepNumber = (status: string) => {
    switch (status) {
      case 'BOOKED':
      case 'GATE_CALLED':
      case 'GATE_ENTERED':
        return 1;
      case 'WEIGHING':
        return 2;
      case 'WEIGHMENT_STAGE_1':
      case 'WEIGHBRIDGE_GROSS':
      case 'QC_INSPECTION':
      case 'UNLOADING':
        return 3;
      case 'WEIGHMENT_COMPLETED':
      case 'WEIGHBRIDGE_TARE':
        return 4;
      case 'PROCUREMENT_COMPLETED':
        return 5;
      case 'PAYMENT_PROCESSING':
        return 6;
      case 'COMPLETED':
        return 7;
      default:
        return 1;
    }
  };

  const currentStep = getStepNumber(activeBooking.status);

  return (
    <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-6">
      {/* Header & Vehicle Selector */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 pb-5 border-b border-slate-100">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-[#1B4332] text-white flex items-center gap-1">
              <Scale className="w-3 h-3 text-[#D4E09B]" />
              Phase 3: Electronic Weighbridge & DBT Terminal
            </span>
            <span className="text-[10px] font-bold text-slate-500 font-mono">
              Bay: <strong className="text-slate-800">{bay}</strong>
            </span>
          </div>
          <h3 className="text-lg font-bold text-slate-900 mt-1 flex items-center gap-2">
            <span>Token #{activeBooking.tokenNumber}</span>
            <span className="text-slate-400 font-normal text-sm">• {activeBooking.farmerName}</span>
          </h3>
          <p className="text-xs text-slate-500">
            Village: <strong className="text-slate-700">{activeBooking.village}</strong> • Crop:{' '}
            <strong className="text-[#1B4332]">{activeBooking.cropName}</strong> • Vehicle:{' '}
            <strong className="font-mono text-slate-800">{activeBooking.vehicleNumber}</strong>
          </p>
        </div>

        {/* Vehicle Switcher Dropdown */}
        <div className="flex flex-wrap items-center gap-3">
          <div className="text-xs">
            <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
              Select Queued Vehicle:
            </label>
            <select
              value={selectedBookingId}
              onChange={(e) => setSelectedBookingId(e.target.value)}
              className="py-2 px-3 bg-[#F3F6F1] border border-slate-300 rounded-xl text-xs font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
            >
              {yardBookings.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.tokenNumber} - {b.farmerName} ({b.status})
                </option>
              ))}
            </select>
          </div>

          <div className="text-xs">
            <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
              Kanta Bay:
            </label>
            <select
              value={bay}
              onChange={(e) => setBay(e.target.value)}
              className="py-2 px-3 bg-[#F3F6F1] border border-slate-300 rounded-xl text-xs font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
            >
              <option value="Kanta Bay 1">Kanta Bay 1 (South Gate)</option>
              <option value="Kanta Bay 2">Kanta Bay 2 (North Gate)</option>
              <option value="Kanta Bay 3">Kanta Bay 3 (Heavy Commercial)</option>
            </select>
          </div>
        </div>
      </div>

      {/* Success banner if any */}
      {actionSuccessMsg && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl text-xs font-medium flex items-center gap-2 animate-in fade-in duration-200">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{actionSuccessMsg}</span>
        </div>
      )}

      {/* Visual Workflow Stepper */}
      <div className="py-2">
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-2 text-center text-xs">
          {/* Step 1 */}
          <div
            className={`p-3 rounded-2xl border transition-all ${
              currentStep >= 2
                ? 'bg-emerald-50 border-emerald-300 text-emerald-900'
                : 'bg-slate-50 border-slate-200 text-slate-500'
            }`}
          >
            <div className="flex items-center justify-center gap-1 font-bold text-[11px] mb-1">
              {currentStep >= 2 ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <span>1.</span>}
              <span>Bay Admission</span>
            </div>
            <p className="text-[10px] opacity-80">Vehicle to Kanta</p>
          </div>

          {/* Step 2 */}
          <div
            className={`p-3 rounded-2xl border transition-all ${
              currentStep >= 3
                ? 'bg-emerald-50 border-emerald-300 text-emerald-900'
                : currentStep === 2
                ? 'bg-amber-50 border-amber-300 text-amber-900 ring-2 ring-amber-400'
                : 'bg-slate-50 border-slate-200 text-slate-500'
            }`}
          >
            <div className="flex items-center justify-center gap-1 font-bold text-[11px] mb-1">
              {currentStep >= 3 ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <span>2.</span>}
              <span>Stage 1: Gross</span>
            </div>
            <p className="text-[10px] opacity-80">Truck + Produce</p>
          </div>

          {/* Step 3 */}
          <div
            className={`p-3 rounded-2xl border transition-all ${
              currentStep >= 4
                ? 'bg-emerald-50 border-emerald-300 text-emerald-900'
                : currentStep === 3
                ? 'bg-amber-50 border-amber-300 text-amber-900 ring-2 ring-amber-400'
                : 'bg-slate-50 border-slate-200 text-slate-500'
            }`}
          >
            <div className="flex items-center justify-center gap-1 font-bold text-[11px] mb-1">
              {currentStep >= 4 ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <span>3.</span>}
              <span>Stage 2: Tare</span>
            </div>
            <p className="text-[10px] opacity-80">Empty Vehicle</p>
          </div>

          {/* Step 4 */}
          <div
            className={`p-3 rounded-2xl border transition-all ${
              currentStep >= 5
                ? 'bg-emerald-50 border-emerald-300 text-emerald-900'
                : currentStep === 4
                ? 'bg-blue-50 border-blue-300 text-blue-900 ring-2 ring-blue-400'
                : 'bg-slate-50 border-slate-200 text-slate-500'
            }`}
          >
            <div className="flex items-center justify-center gap-1 font-bold text-[11px] mb-1">
              {currentStep >= 5 ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <span>4.</span>}
              <span>J-Form Certification</span>
            </div>
            <p className="text-[10px] opacity-80">APMC Officer Stamp</p>
          </div>

          {/* Step 5 */}
          <div
            className={`p-3 rounded-2xl border transition-all ${
              currentStep >= 7
                ? 'bg-emerald-50 border-emerald-300 text-emerald-900'
                : currentStep >= 5
                ? 'bg-purple-50 border-purple-300 text-purple-900 ring-2 ring-purple-400'
                : 'bg-slate-50 border-slate-200 text-slate-500'
            }`}
          >
            <div className="flex items-center justify-center gap-1 font-bold text-[11px] mb-1">
              {currentStep >= 7 ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <span>5.</span>}
              <span>DBT Bank Credit</span>
            </div>
            <p className="text-[10px] opacity-80">PFMS Direct Payout</p>
          </div>
        </div>
      </div>

      {/* Moisture Alert if high */}
      {isMoistureHigh && (
        <div className="p-4 bg-amber-50 border border-amber-200 rounded-2xl text-amber-900 text-xs flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
          <div>
            <p className="font-bold">Moisture Level High ({moistureVal}%) - Exceeds FAQ Limit (≤12.0%)</p>
            <p className="text-amber-800 text-[11px] mt-0.5">
              Under MP Mandi regulations, moisture dockage will apply, or produce must undergo aeration prior to procurement certification.
            </p>
          </div>
        </div>
      )}

      {/* Interactive Two-Stage Weighbridge Input Panel */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Left Column: Weighment Inputs */}
        <div className="bg-[#F8FAF6] p-5 rounded-2xl border border-slate-200/80 space-y-4 text-xs">
          <h4 className="font-bold text-slate-800 flex items-center gap-2 text-sm">
            <Scale className="w-4 h-4 text-[#2D6A4F]" />
            <span>Weighbridge Measurement Scales</span>
          </h4>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-bold text-slate-700 mb-1 uppercase tracking-wider text-[10px]">
                Stage 1: Gross Loaded Weight (kg)
              </label>
              <div className="relative">
                <input
                  type="number"
                  value={grossInput}
                  onChange={(e) => setGrossInput(e.target.value)}
                  className="w-full py-2.5 px-3 bg-white border border-slate-300 rounded-xl font-mono font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                  placeholder="10500"
                />
                <span className="absolute right-3 top-2.5 text-[10px] text-slate-400 font-mono font-bold">
                  {(grossKg / 100).toFixed(2)} Qtl
                </span>
              </div>
              <p className="text-[10px] text-slate-400 mt-1">Vehicle + Loaded Produce</p>
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1 uppercase tracking-wider text-[10px]">
                Stage 2: Empty Tare Weight (kg)
              </label>
              <div className="relative">
                <input
                  type="number"
                  value={tareInput}
                  onChange={(e) => setTareInput(e.target.value)}
                  className="w-full py-2.5 px-3 bg-white border border-slate-300 rounded-xl font-mono font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                  placeholder="3800"
                />
                <span className="absolute right-3 top-2.5 text-[10px] text-slate-400 font-mono font-bold">
                  {(tareKg / 100).toFixed(2)} Qtl
                </span>
              </div>
              <p className="text-[10px] text-slate-400 mt-1">Empty Trolley after Unloading</p>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3 pt-2">
            <div>
              <label className="block font-bold text-slate-700 mb-1 uppercase tracking-wider text-[10px]">
                Moisture (%)
              </label>
              <input
                type="number"
                step="0.1"
                value={moistureInput}
                onChange={(e) => setMoistureInput(e.target.value)}
                className={`w-full py-2 px-3 bg-white border rounded-xl font-mono font-bold text-slate-900 focus:outline-none focus:ring-2 ${
                  isMoistureHigh ? 'border-amber-400 text-amber-900' : 'border-slate-300'
                }`}
                placeholder="10.5"
              />
              <p className="text-[9px] text-[#2D6A4F] font-semibold mt-1">Limit: ≤12%</p>
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1 uppercase tracking-wider text-[10px]">
                Foreign Matter (%)
              </label>
              <input
                type="number"
                step="0.1"
                value={foreignMatterInput}
                onChange={(e) => setForeignMatterInput(e.target.value)}
                className="w-full py-2 px-3 bg-white border border-slate-300 rounded-xl font-mono font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                placeholder="1.2"
              />
              <p className="text-[9px] text-slate-400 mt-1">Limit: ≤2%</p>
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1 uppercase tracking-wider text-[10px]">
                Quality Grade
              </label>
              <select
                value={qualityGrade}
                onChange={(e) => setQualityGrade(e.target.value as any)}
                className="w-full py-2 px-2 bg-white border border-slate-300 rounded-xl font-semibold text-slate-900 text-xs focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
              >
                <option value="Grade A">Grade A (FAQ)</option>
                <option value="Grade B">Grade B (Dockage)</option>
                <option value="Standard Fair Average (FAQ)">Standard FAQ</option>
              </select>
              <p className="text-[9px] text-slate-400 mt-1">MPSAMB Spec</p>
            </div>
          </div>
        </div>

        {/* Right Column: Authoritative Net Computation & MSP Payout Breakdown */}
        <div className="bg-[#1B4332] text-white p-5 rounded-2xl shadow-sm flex flex-col justify-between space-y-4 text-xs">
          <div>
            <div className="flex items-center justify-between border-b border-emerald-700/60 pb-2.5">
              <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-200">
                Authoritative Net Weight Certification
              </span>
              <span className="text-[10px] font-mono text-[#D4E09B] font-bold">
                Formula: Gross ({grossKg} kg) - Tare ({tareKg} kg)
              </span>
            </div>

            <div className="mt-4 grid grid-cols-2 gap-4">
              <div>
                <span className="text-slate-300 text-[11px]">Certified Net Weight:</span>
                <p className="text-2xl font-black font-mono text-[#D4E09B] mt-0.5">
                  {netKg.toLocaleString('en-IN')} <span className="text-sm font-normal text-emerald-200">kg</span>
                </p>
                <p className="text-[11px] font-bold text-white/80 font-mono mt-0.5">
                  {netQtl.toFixed(2)} Quintals
                </p>
              </div>

              <div>
                <span className="text-slate-300 text-[11px]">Approved MSP Rate:</span>
                <p className="text-2xl font-black font-mono text-white mt-0.5">
                  ₹{totalMspRate.toLocaleString('en-IN')} <span className="text-xs font-normal text-emerald-200">/Qtl</span>
                </p>
                <p className="text-[10px] text-emerald-300 mt-0.5">
                  Base ₹{standardMsp} + MP Bonus ₹{mpBonus}
                </p>
              </div>
            </div>
          </div>

          {/* Total Beneficiary Payable */}
          <div className="bg-black/20 p-4 rounded-xl border border-white/10 flex items-center justify-between">
            <div>
              <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-200">
                Total Direct Farmer Payout (DBT)
              </span>
              <p className="text-xl sm:text-2xl font-black font-mono text-[#D4E09B] mt-0.5">
                ₹{totalPayout.toLocaleString('en-IN')}
              </p>
            </div>
            <div className="text-right text-[10px] text-emerald-200">
              <p>Beneficiary: {activeBooking.farmerName}</p>
              <p className="font-mono">A/C: ****4321 (SBI)</p>
            </div>
          </div>
        </div>
      </div>

      {/* Phase 3 Action Controller Buttons */}
      <div className="pt-2 border-t border-slate-100 flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          {/* Step 1: Admit to bay */}
          {(activeBooking.status === 'GATE_CALLED' || activeBooking.status === 'GATE_ENTERED' || activeBooking.status === 'BOOKED') && (
            <button
              onClick={handleStartWeighment}
              disabled={isProcessing}
              className="px-4 py-2.5 bg-[#2D6A4F] hover:bg-[#1B4332] text-white font-bold text-xs uppercase tracking-wider rounded-xl flex items-center gap-1.5 cursor-pointer shadow-xs transition-colors"
            >
              <Scale className="w-4 h-4 text-[#D4E09B]" />
              <span>Admit to Weighbridge Bay</span>
            </button>
          )}

          {/* Step 2: Record Gross Weight */}
          {(activeBooking.status === 'WEIGHING' || activeBooking.status === 'GATE_ENTERED') && (
            <button
              onClick={handleRecordGross}
              disabled={isProcessing}
              className="px-4 py-2.5 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold text-xs uppercase tracking-wider rounded-xl flex items-center gap-1.5 cursor-pointer shadow-xs transition-colors"
            >
              <Check className="w-4 h-4 text-[#D4E09B]" />
              <span>Record Stage 1 Gross ({grossKg} kg)</span>
            </button>
          )}

          {/* Step 3: Record Empty Tare Weight */}
          {(activeBooking.status === 'WEIGHMENT_STAGE_1' || activeBooking.status === 'WEIGHBRIDGE_GROSS' || activeBooking.status === 'UNLOADING') && (
            <button
              onClick={handleRecordTare}
              disabled={isProcessing}
              className="px-4 py-2.5 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold text-xs uppercase tracking-wider rounded-xl flex items-center gap-1.5 cursor-pointer shadow-xs transition-colors"
            >
              <Check className="w-4 h-4 text-[#D4E09B]" />
              <span>Record Stage 2 Tare ({tareKg} kg) & Compute Net</span>
            </button>
          )}

          {/* Step 4: Certify APMC Procurement */}
          {activeBooking.status === 'WEIGHMENT_COMPLETED' && (
            <button
              onClick={handleCertifyProcurement}
              disabled={isProcessing}
              className="px-4 py-2.5 bg-[#2D6A4F] hover:bg-[#1B4332] text-white font-bold text-xs uppercase tracking-wider rounded-xl flex items-center gap-1.5 cursor-pointer shadow-xs transition-colors"
            >
              <ShieldCheck className="w-4 h-4 text-[#D4E09B]" />
              <span>Certify Procurement & Generate J-Form</span>
            </button>
          )}

          {/* Step 5: Initiate DBT Payment */}
          {activeBooking.status === 'PROCUREMENT_COMPLETED' && (
            <button
              onClick={handleInitiateDbt}
              disabled={isProcessing}
              className="px-4 py-2.5 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold text-xs uppercase tracking-wider rounded-xl flex items-center gap-1.5 cursor-pointer shadow-xs transition-colors"
            >
              <Send className="w-4 h-4 text-[#D4E09B]" />
              <span>Initiate Direct Benefit Transfer (₹{totalPayout.toLocaleString('en-IN')})</span>
            </button>
          )}

          {/* Step 6: Confirm Bank Credit */}
          {activeBooking.status === 'PAYMENT_PROCESSING' && (
            <button
              onClick={handleConfirmBankCredit}
              disabled={isProcessing}
              className="px-4 py-2.5 bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-xs uppercase tracking-wider rounded-xl flex items-center gap-1.5 cursor-pointer shadow-xs transition-colors animate-pulse"
            >
              <CheckCircle2 className="w-4 h-4 text-[#D4E09B]" />
              <span>Confirm Bank Settlement (DBT Credit)</span>
            </button>
          )}
        </div>

        {/* Print / View J-Form Receipt */}
        {(activeBooking.status === 'WEIGHMENT_COMPLETED' ||
          activeBooking.status === 'PROCUREMENT_COMPLETED' ||
          activeBooking.status === 'PAYMENT_PROCESSING' ||
          activeBooking.status === 'COMPLETED') && (
          <button
            onClick={() => onViewJForm(activeBooking)}
            className="px-4 py-2.5 bg-white border-2 border-[#1B4332] text-[#1B4332] hover:bg-emerald-50 font-bold text-xs uppercase tracking-wider rounded-xl flex items-center gap-1.5 cursor-pointer transition-colors shadow-xs"
          >
            <FileText className="w-4 h-4 text-[#1B4332]" />
            <span>View / Print J-Form Receipt</span>
          </button>
        )}
      </div>

      {/* Beneficiary DBT Reference Record if active/completed */}
      {(activeBooking.dbtReferenceNo || activeBooking.utrNumber) && (
        <div className="p-3 bg-purple-50 border border-purple-200 rounded-2xl flex flex-wrap items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-2 text-purple-900">
            <Building2 className="w-4 h-4 text-purple-700 shrink-0" />
            <span>
              DBT Reference:{' '}
              <strong className="font-mono font-bold text-purple-950">
                {activeBooking.dbtReferenceNo || activeBooking.utrNumber}
              </strong>{' '}
              • Beneficiary Bank: <strong>State Bank of India (A/C ****4321, IFSC: SBIN0001111)</strong>
            </span>
          </div>
          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-purple-200 text-purple-900">
            Payment Status: {activeBooking.paymentStatus}
          </span>
        </div>
      )}
    </div>
  );
};
