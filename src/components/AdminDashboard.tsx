import React, { useState, useEffect } from 'react';
import {
  Building2,
  Shield,
  Layers,
  TrendingUp,
  Scale,
  Clock,
  CheckCircle2,
  AlertCircle,
  Plus,
  Save,
  FileSpreadsheet,
  FileText,
  Search,
  Filter,
  BarChart3,
  Calendar,
  Sparkles,
  MessageSquare,
  Send,
  Smartphone,
  Users,
  RefreshCw,
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  Legend,
  AreaChart,
  Area,
} from 'recharts';
import {
  MandiCenter,
  CropInfo,
  SlotBooking,
  DistrictProcurementStat,
  TimeSlotConfig,
  Language,
  FarmerProfile,
  SmsLogItem,
} from '../types';
import { translations } from '../i18n/translations';
import { DISTRICT_PROCUREMENT_STATS, STANDARD_TIME_SLOTS } from '../data/mpMandiData';
import { AdminSmsDispatchModal } from './AdminSmsDispatchModal';

interface Props {
  language: Language;
  mandis: MandiCenter[];
  crops: CropInfo[];
  bookings: SlotBooking[];
  onCallNextToken: (mandiId: string) => Promise<void>;
  onUpdateBookingStatus: (bookingId: string, updates: Partial<SlotBooking>) => Promise<void>;
  onUpdateMsp: (cropId: string, standardMsp: number, mpBonus: number) => Promise<void>;
  onUpdateSlotCapacity: (timeSlot: string, maxVehicles: number, status: any) => Promise<void>;
  onViewJForm: (booking: SlotBooking) => void;
  currentUser: { name: string; phone: string; district: string; mandiId?: string } | null;
}

export const AdminDashboard: React.FC<Props> = ({
  language,
  mandis,
  crops,
  bookings,
  onCallNextToken,
  onUpdateBookingStatus,
  onUpdateMsp,
  onUpdateSlotCapacity,
  onViewJForm,
  currentUser,
}) => {
  const t = translations[language];

  // Active Admin Sub-tab
  const [adminTab, setAdminTab] = useState<
    'QUEUE_CTRL' | 'SLOTS' | 'MSP_MGR' | 'REPORTS' | 'FARMER_LIST' | 'FARMERS_DB' | 'SMS_DISPATCH'
  >('QUEUE_CTRL');

  // Real SMS & Farmer DB state
  const [isSmsModalOpen, setIsSmsModalOpen] = useState(false);
  const [smsPrefill, setSmsPrefill] = useState<{ name: string; phone: string; aadharMasked?: string } | undefined>();
  const [registeredFarmers, setRegisteredFarmers] = useState<FarmerProfile[]>([]);
  const [smsLogs, setSmsLogs] = useState<SmsLogItem[]>([]);
  const [isLoadingDb, setIsLoadingDb] = useState(false);

  // Fetch registered farmers from persistent database
  const fetchDbFarmers = async () => {
    try {
      const res = await fetch('/api/farmers');
      const data = await res.json();
      if (data.success && data.farmers) setRegisteredFarmers(data.farmers);
    } catch (e) {
      console.error('Failed to load farmers from database', e);
    }
  };

  // Fetch real SMS dispatch history
  const fetchDbLogs = async () => {
    try {
      const res = await fetch('/api/sms/logs');
      const data = await res.json();
      if (data.success && data.logs) setSmsLogs(data.logs);
    } catch (e) {
      console.error('Failed to load SMS logs', e);
    }
  };

  useEffect(() => {
    fetchDbFarmers();
    fetchDbLogs();
  }, []);

  // Selected Mandi for queue control
  const [selectedMandiId, setSelectedMandiId] = useState(
    currentUser?.mandiId || mandis[0]?.id || 'mandi-sehore'
  );
  const selectedMandi = mandis.find((m) => m.id === selectedMandiId) || mandis[0];

  // Filter bookings for this Mandi
  const mandiBookings = bookings.filter((b) => b.mandiCenterId === selectedMandiId);

  // Weighment entry state for the active truck
  const activeBooking = mandiBookings.find(
    (b) => b.status === 'GATE_ENTERED' || b.status === 'WEIGHBRIDGE_GROSS' || b.status === 'QC_INSPECTION'
  ) || mandiBookings[0];

  const [grossInput, setGrossInput] = useState(
    activeBooking?.actualGrossWeightKg ? String(activeBooking.actualGrossWeightKg) : '10500'
  );
  const [tareInput, setTareInput] = useState(
    activeBooking?.actualTareWeightKg ? String(activeBooking.actualTareWeightKg) : '3900'
  );
  const [moistureInput, setMoistureInput] = useState(
    activeBooking?.moisturePct ? String(activeBooking.moisturePct) : '10.5'
  );
  const [qcGradeInput, setQcGradeInput] = useState<'Grade A' | 'Grade B' | 'Standard Fair Average (FAQ)'>(
    'Grade A'
  );

  // MSP Edit state
  const [editingCropId, setEditingCropId] = useState(crops[0]?.id || 'crop-wheat');
  const targetCrop = crops.find((c) => c.id === editingCropId) || crops[0];
  const [mspStandardInput, setMspStandardInput] = useState(targetCrop?.standardMspPerQuintal || 2275);
  const [mspBonusInput, setMspBonusInput] = useState(targetCrop?.mpBonusPerQuintal || 125);

  // Slots Edit state
  const [slotsList, setSlotsList] = useState<TimeSlotConfig[]>(STANDARD_TIME_SLOTS);

  // Search in farmer bookings list
  const [searchQuery, setSearchQuery] = useState('');

  // Handle weighment save & J-Form trigger
  const handleSaveWeighment = async () => {
    if (!activeBooking) return;
    const gross = Number(grossInput) || 10500;
    const tare = Number(tareInput) || 3900;
    const netKg = Math.max(0, gross - tare);
    const netQtl = netKg / 100;
    const rate = 2400; // MSP
    const payout = netQtl * rate;

    await onUpdateBookingStatus(activeBooking.id, {
      actualGrossWeightKg: gross,
      actualTareWeightKg: tare,
      netWeightQuintals: netQtl,
      moisturePct: Number(moistureInput) || 10.5,
      qualityGrade: qcGradeInput,
      totalPayoutRs: payout,
      status: 'COMPLETED',
      paymentStatus: 'DBT_INITIATED',
      utrNumber: `MPDBT${Date.now()}`,
    });
  };

  // Handle MSP update
  const handleSaveMsp = async (e: React.FormEvent) => {
    e.preventDefault();
    await onUpdateMsp(editingCropId, Number(mspStandardInput), Number(mspBonusInput));
  };

  // Filtered Farmer Bookings
  const filteredBookings = bookings.filter((b) => {
    const q = searchQuery.toLowerCase();
    return (
      b.tokenNumber.toLowerCase().includes(q) ||
      b.farmerName.toLowerCase().includes(q) ||
      b.farmerPhone.includes(q) ||
      b.cropName.toLowerCase().includes(q) ||
      b.district.toLowerCase().includes(q)
    );
  });

  // Calculate high-level stats
  const totalBookingsCount = bookings.length;
  const completedCount = bookings.filter((b) => b.status === 'COMPLETED').length;
  const totalProcuredQtl = bookings
    .filter((b) => b.netWeightQuintals)
    .reduce((acc, b) => acc + (b.netWeightQuintals || 0), 0);

  return (
    <div className="space-y-6">
      {/* Top Header & Admin Tabs */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-sm border border-slate-200">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <div className="w-12 h-12 rounded-2xl bg-[#1B4332] text-[#D4E09B] flex items-center justify-center shadow-xs">
              <Building2 className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-bold text-slate-900">
                  {t.adminDashboardTitle}
                </h2>
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold uppercase tracking-wider bg-[#D4E09B]/40 text-[#1B4332] border border-[#A3B18A]/40">
                  APMC Officer
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-0.5">
                Real-time queue clearance, gate weighment entry, slot capacity & statewide district analytics
              </p>
            </div>
          </div>

          {/* Admin Header Actions */}
          <div className="flex flex-wrap items-center gap-2.5">
            <button
              id="admin-open-sms-dispatch-btn"
              onClick={() => {
                setSmsPrefill(undefined);
                setIsSmsModalOpen(true);
              }}
              className="px-3.5 py-2.5 bg-[#1B4332] hover:bg-[#2D6A4F] text-white rounded-xl text-xs font-bold uppercase tracking-wider shadow-sm flex items-center gap-2 cursor-pointer transition-colors"
            >
              <MessageSquare className="w-4 h-4 text-[#D4E09B]" />
              <span>Send Real SMS</span>
            </button>

            {/* Center selector */}
            <div className="flex items-center gap-2">
              <label className="text-xs font-bold text-slate-500 hidden sm:inline uppercase tracking-wider">Active Center:</label>
              <select
                value={selectedMandiId}
                onChange={(e) => setSelectedMandiId(e.target.value)}
                className="bg-[#F3F6F1] text-slate-900 text-xs font-bold px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
              >
                {mandis.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name} ({m.district})
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {/* Sub Navigation */}
        <div className="flex items-center gap-1.5 overflow-x-auto scrollbar-none mt-6 pt-4 border-t border-slate-100 text-xs">
          <button
            onClick={() => setAdminTab('QUEUE_CTRL')}
            className={`px-3.5 py-2 rounded-xl font-bold uppercase tracking-wider text-[11px] flex items-center gap-2 transition-all cursor-pointer ${
              adminTab === 'QUEUE_CTRL'
                ? 'bg-[#1B4332] text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-[#F3F6F1]'
            }`}
          >
            <Scale className="w-3.5 h-3.5 text-[#D4E09B]" />
            <span>Live Queue & Weighbridge</span>
          </button>
          <button
            onClick={() => setAdminTab('SLOTS')}
            className={`px-3.5 py-2 rounded-xl font-bold uppercase tracking-wider text-[11px] flex items-center gap-2 transition-all cursor-pointer ${
              adminTab === 'SLOTS'
                ? 'bg-[#1B4332] text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-[#F3F6F1]'
            }`}
          >
            <Clock className="w-3.5 h-3.5 text-[#D4E09B]" />
            <span>Capacity & Daily Slots</span>
          </button>
          <button
            onClick={() => setAdminTab('MSP_MGR')}
            className={`px-3.5 py-2 rounded-xl font-bold uppercase tracking-wider text-[11px] flex items-center gap-2 transition-all cursor-pointer ${
              adminTab === 'MSP_MGR'
                ? 'bg-[#1B4332] text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-[#F3F6F1]'
            }`}
          >
            <TrendingUp className="w-3.5 h-3.5 text-[#D4E09B]" />
            <span>MSP & Bonus Rates</span>
          </button>
          <button
            onClick={() => setAdminTab('REPORTS')}
            className={`px-3.5 py-2 rounded-xl font-bold uppercase tracking-wider text-[11px] flex items-center gap-2 transition-all cursor-pointer ${
              adminTab === 'REPORTS'
                ? 'bg-[#1B4332] text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-[#F3F6F1]'
            }`}
          >
            <BarChart3 className="w-3.5 h-3.5 text-[#D4E09B]" />
            <span>District Analytics</span>
          </button>
          <button
            onClick={() => setAdminTab('FARMER_LIST')}
            className={`px-3.5 py-2 rounded-xl font-bold uppercase tracking-wider text-[11px] flex items-center gap-2 transition-all cursor-pointer ${
              adminTab === 'FARMER_LIST'
                ? 'bg-[#1B4332] text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-[#F3F6F1]'
            }`}
          >
            <FileSpreadsheet className="w-3.5 h-3.5 text-[#D4E09B]" />
            <span>Farmer Slips ({bookings.length})</span>
          </button>
          <button
            id="admin-tab-farmers-db"
            onClick={() => {
              setAdminTab('FARMERS_DB');
              fetchDbFarmers();
            }}
            className={`px-3.5 py-2 rounded-xl font-bold uppercase tracking-wider text-[11px] flex items-center gap-2 transition-all cursor-pointer ${
              adminTab === 'FARMERS_DB'
                ? 'bg-[#1B4332] text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-[#F3F6F1]'
            }`}
          >
            <Users className="w-3.5 h-3.5 text-[#D4E09B]" />
            <span>Registered Kisan DB ({registeredFarmers.length})</span>
          </button>
          <button
            id="admin-tab-sms-dispatch"
            onClick={() => {
              setAdminTab('SMS_DISPATCH');
              fetchDbLogs();
            }}
            className={`px-3.5 py-2 rounded-xl font-bold uppercase tracking-wider text-[11px] flex items-center gap-2 transition-all cursor-pointer ${
              adminTab === 'SMS_DISPATCH'
                ? 'bg-[#1B4332] text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-[#F3F6F1]'
            }`}
          >
            <MessageSquare className="w-3.5 h-3.5 text-[#D4E09B]" />
            <span>Real SMS Console ({smsLogs.length})</span>
          </button>
        </div>
      </div>

      {/* ========================================================= */}
      {/* TAB 1: LIVE QUEUE CONTROLLER & WEIGHBRIDGE RECORDER */}
      {/* ========================================================= */}
      {adminTab === 'QUEUE_CTRL' && (
        <div className="space-y-5">
          {/* Mandi Live Header Card */}
          <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-slate-100">
              <div>
                <h3 className="text-lg font-bold text-slate-900">{selectedMandi.name}</h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  District: <strong className="text-slate-700">{selectedMandi.district}, MP</strong> • Contact: {selectedMandi.phone}
                </p>
              </div>

              {/* Call Next Token Button */}
              <div className="flex items-center gap-3">
                <button
                  id="admin-call-next-btn"
                  onClick={() => onCallNextToken(selectedMandi.id)}
                  className="px-5 py-3 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold text-xs uppercase tracking-wider rounded-xl shadow-md flex items-center gap-2 cursor-pointer transition-all active:scale-95"
                >
                  <Clock className="w-4 h-4 text-[#D4E09B]" />
                  <span>Call Next Token (#{selectedMandi.currentTokenServing + 1})</span>
                </button>
              </div>
            </div>

            {/* Queue Metrics */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3.5 mt-6">
              <div className="p-4 bg-[#F3F6F1]/70 rounded-2xl border border-slate-200/80 text-center">
                <p className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Currently Serving</p>
                <p className="text-2xl font-black font-mono text-[#1B4332] mt-1">
                  Token #{selectedMandi.currentTokenServing}
                </p>
              </div>
              <div className="p-4 bg-[#F3F6F1]/70 rounded-2xl border border-slate-200/80 text-center">
                <p className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Waiting Yard</p>
                <p className="text-2xl font-black font-mono text-slate-900 mt-1">
                  {selectedMandi.activeTokensWaiting}
                </p>
              </div>
              <div className="p-4 bg-[#F3F6F1]/70 rounded-2xl border border-slate-200/80 text-center">
                <p className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Weighbridges</p>
                <p className="text-2xl font-black font-mono text-slate-900 mt-1">
                  {selectedMandi.weighbridgesCount} / {selectedMandi.weighbridgesCount}
                </p>
              </div>
              <div className="p-4 bg-[#F3F6F1]/70 rounded-2xl border border-slate-200/80 text-center">
                <p className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Daily Intake</p>
                <p className="text-2xl font-black font-mono text-slate-900 mt-1">
                  {selectedMandi.dailyCapacityQuintals} Qtl
                </p>
              </div>
            </div>
          </div>

          {/* Weighbridge Entry & Verification Form */}
          {activeBooking ? (
            <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8">
              <div className="flex items-center justify-between pb-4 mb-5 border-b border-slate-100">
                <div>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-[#1B4332] bg-[#D4E09B]/40 border border-[#A3B18A]/40 px-2.5 py-0.5 rounded-full">
                    Now at Weighbridge Bay
                  </span>
                  <h3 className="text-base font-bold text-slate-900 mt-1.5">
                    Token {activeBooking.tokenNumber} • {activeBooking.farmerName}
                  </h3>
                  <p className="text-xs text-slate-500">
                    Commodity: <strong>{activeBooking.cropName}</strong> • Vehicle: <strong>{activeBooking.vehicleNumber}</strong>
                  </p>
                </div>

                <div className="text-right">
                  <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 text-slate-700">
                    Status: {activeBooking.status}
                  </span>
                </div>
              </div>

              {/* Weighment Entry Inputs */}
              <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 text-xs">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                    Gross Loaded Weight (kg)
                  </label>
                  <input
                    type="number"
                    value={grossInput}
                    onChange={(e) => setGrossInput(e.target.value)}
                    className="w-full py-2.5 px-3 bg-[#F3F6F1]/60 border border-slate-300 rounded-xl font-mono font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                    placeholder="10500"
                  />
                  <p className="text-[10px] text-slate-400 mt-1">Truck + Grain</p>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                    Empty Tare Weight (kg)
                  </label>
                  <input
                    type="number"
                    value={tareInput}
                    onChange={(e) => setTareInput(e.target.value)}
                    className="w-full py-2.5 px-3 bg-[#F3F6F1]/60 border border-slate-300 rounded-xl font-mono font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                    placeholder="3900"
                  />
                  <p className="text-[10px] text-slate-400 mt-1">Empty Tractor Trolley</p>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                    Moisture Content (%)
                  </label>
                  <input
                    type="number"
                    step="0.1"
                    value={moistureInput}
                    onChange={(e) => setMoistureInput(e.target.value)}
                    className="w-full py-2.5 px-3 bg-[#F3F6F1]/60 border border-slate-300 rounded-xl font-mono font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                    placeholder="10.5"
                  />
                  <p className="text-[10px] text-[#2D6A4F] font-semibold mt-1">FAQ Limit: ≤12.0%</p>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                    Quality QC Grade
                  </label>
                  <select
                    value={qcGradeInput}
                    onChange={(e) => setQcGradeInput(e.target.value as any)}
                    className="w-full py-2.5 px-3 bg-[#F3F6F1]/60 border border-slate-300 rounded-xl font-semibold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                  >
                    <option value="Grade A">Grade A (FAQ Standard)</option>
                    <option value="Grade B">Grade B (Marginal Dockage)</option>
                    <option value="Standard Fair Average (FAQ)">Standard FAQ</option>
                  </select>
                </div>
              </div>

              {/* Net Computed Payout Preview */}
              <div className="mt-5 p-4 bg-[#F3F6F1]/70 rounded-2xl border border-slate-200/80 flex flex-wrap items-center justify-between gap-3 text-xs">
                <div>
                  <span className="text-slate-500">Net Weight: </span>
                  <strong className="font-mono text-[#1B4332] text-sm">
                    {Math.max(0, (Number(grossInput) || 0) - (Number(tareInput) || 0))} kg (
                    {((Math.max(0, (Number(grossInput) || 0) - (Number(tareInput) || 0))) / 100).toFixed(2)} Quintals)
                  </strong>
                </div>

                <div>
                  <span className="text-slate-500">Procurement Payout @ ₹2,400/Qtl: </span>
                  <strong className="font-mono text-[#1B4332] text-sm">
                    ₹{(
                      ((Math.max(0, (Number(grossInput) || 0) - (Number(tareInput) || 0))) / 100) * 2400
                    ).toLocaleString('en-IN', { maximumFractionDigits: 0 })}
                  </strong>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={handleSaveWeighment}
                    className="px-5 py-2.5 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold text-xs uppercase tracking-wider rounded-xl shadow-xs flex items-center gap-1.5 cursor-pointer transition-colors"
                  >
                    <CheckCircle2 className="w-4 h-4 text-[#D4E09B]" />
                    <span>Confirm Weighment & Issue J-Form</span>
                  </button>
                  {activeBooking.status === 'COMPLETED' && (
                    <button
                      onClick={() => onViewJForm(activeBooking)}
                      className="px-4 py-2.5 bg-[#2D6A4F] hover:bg-[#1B4332] text-white font-bold text-xs uppercase tracking-wider rounded-xl flex items-center gap-1.5 cursor-pointer transition-colors"
                    >
                      <FileText className="w-3.5 h-3.5" />
                      <span>Print Slip</span>
                    </button>
                  )}
                </div>
              </div>
            </div>
          ) : (
            <div className="p-8 bg-white rounded-3xl border border-slate-200 text-center text-xs text-slate-500">
              No vehicles currently checked in to the weighbridge bay. Click 'Call Next Token' to summon.
            </div>
          )}
        </div>
      )}

      {/* ========================================================= */}
      {/* TAB 2: DAILY SLOTS & CAPACITY MANAGER */}
      {/* ========================================================= */}
      {adminTab === 'SLOTS' && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-4">
          <div className="border-b border-slate-100 pb-4">
            <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <Clock className="w-5 h-5 text-[#2D6A4F]" />
              <span>Mandi Center Slot Capacity & Weather Buffer Settings</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Adjust maximum vehicle intake and hold slots during adverse weather alerts.
            </p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-[#F3F6F1] text-[10px] uppercase font-bold text-slate-500 tracking-wider">
                <tr>
                  <th className="p-3.5 rounded-l-xl">Time Window</th>
                  <th className="p-3.5">Max Vehicles Limit</th>
                  <th className="p-3.5">Max Intake (Quintals)</th>
                  <th className="p-3.5">Status</th>
                  <th className="p-3.5 rounded-r-xl">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {slotsList.map((slot) => (
                  <tr key={slot.timeSlot} className="hover:bg-[#F3F6F1]/50 transition-colors">
                    <td className="p-3.5 font-bold text-slate-900">{slot.timeSlot}</td>
                    <td className="p-3.5">
                      <input
                        type="number"
                        value={slot.maxVehicles}
                        onChange={(e) => {
                          const val = Number(e.target.value);
                          setSlotsList((prev) =>
                            prev.map((s) => (s.timeSlot === slot.timeSlot ? { ...s, maxVehicles: val } : s))
                          );
                        }}
                        className="w-24 py-1.5 px-2.5 border border-slate-300 rounded-xl text-xs font-mono font-bold focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                      />
                    </td>
                    <td className="p-3.5 font-mono text-slate-700">{slot.maxCapacityQuintals} Qtl</td>
                    <td className="p-3.5">
                      <select
                        value={slot.status}
                        onChange={(e) => {
                          const val = e.target.value as any;
                          setSlotsList((prev) =>
                            prev.map((s) => (s.timeSlot === slot.timeSlot ? { ...s, status: val } : s))
                          );
                        }}
                        className="py-1.5 px-2.5 border border-slate-300 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                      >
                        <option value="OPEN">OPEN</option>
                        <option value="LIMITED">LIMITED</option>
                        <option value="FULL">FULL</option>
                        <option value="WEATHER_HOLD">WEATHER HOLD (Rain Delay)</option>
                      </select>
                    </td>
                    <td className="p-3.5">
                      <button
                        onClick={() => onUpdateSlotCapacity(slot.timeSlot, slot.maxVehicles, slot.status)}
                        className="px-3.5 py-1.5 bg-[#1B4332] hover:bg-[#2D6A4F] text-white rounded-xl text-[11px] font-bold uppercase tracking-wider cursor-pointer transition-colors"
                      >
                        Apply
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* TAB 3: MSP & BONUS MANAGEMENT */}
      {/* ========================================================= */}
      {adminTab === 'MSP_MGR' && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-6">
          <div className="border-b border-slate-100 pb-4">
            <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <TrendingUp className="w-5 h-5 text-[#2D6A4F]" />
              <span>Madhya Pradesh Mandi Board MSP & State Bonus Manager</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Update statutory procurement prices and state subsidies across all Mandis in Madhya Pradesh.
            </p>
          </div>

          <form onSubmit={handleSaveMsp} className="p-5 bg-[#F3F6F1]/70 rounded-2xl border border-slate-200 space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">Select Commodity</label>
                <select
                  value={editingCropId}
                  onChange={(e) => {
                    setEditingCropId(e.target.value);
                    const c = crops.find((cr) => cr.id === e.target.value);
                    if (c) {
                      setMspStandardInput(c.standardMspPerQuintal);
                      setMspBonusInput(c.mpBonusPerQuintal);
                    }
                  }}
                  className="w-full py-2.5 px-3 bg-white border border-slate-300 rounded-xl text-xs font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                >
                  {crops.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">Standard Central MSP (₹/Qtl)</label>
                <input
                  type="number"
                  value={mspStandardInput}
                  onChange={(e) => setMspStandardInput(Number(e.target.value))}
                  className="w-full py-2.5 px-3 bg-white border border-slate-300 rounded-xl text-xs font-mono font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">MP State Farmer Bonus (₹/Qtl)</label>
                <input
                  type="number"
                  value={mspBonusInput}
                  onChange={(e) => setMspBonusInput(Number(e.target.value))}
                  className="w-full py-2.5 px-3 bg-white border border-slate-300 rounded-xl text-xs font-mono font-bold text-[#1B4332] focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                />
              </div>
            </div>

            <div className="flex items-center justify-between pt-3 border-t border-slate-200">
              <div className="text-xs">
                Total Mandi Price will be:{' '}
                <strong className="text-[#1B4332] text-sm font-mono font-black">
                  ₹{(Number(mspStandardInput) + Number(mspBonusInput)).toLocaleString()}/Quintal
                </strong>
              </div>
              <button
                type="submit"
                className="px-5 py-2.5 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold text-xs uppercase tracking-wider rounded-xl shadow-xs flex items-center gap-1.5 cursor-pointer transition-colors"
              >
                <Save className="w-4 h-4 text-[#D4E09B]" />
                <span>Publish Revised Mandi MSP</span>
              </button>
            </div>
          </form>

          {/* Current Table */}
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-[#F3F6F1] text-[10px] uppercase font-bold text-slate-500 tracking-wider">
                <tr>
                  <th className="p-3.5 rounded-l-xl">Crop</th>
                  <th className="p-3.5">Central MSP</th>
                  <th className="p-3.5">MP State Bonus</th>
                  <th className="p-3.5 font-bold text-[#1B4332]">Total Mandi MSP</th>
                  <th className="p-3.5 rounded-r-xl">Market Comparison</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {crops.map((c) => (
                  <tr key={c.id} className="hover:bg-[#F3F6F1]/50 transition-colors">
                    <td className="p-3.5 font-bold text-slate-900">{c.name}</td>
                    <td className="p-3.5 font-mono">₹{c.standardMspPerQuintal}</td>
                    <td className="p-3.5 font-mono text-[#2D6A4F] font-bold">+₹{c.mpBonusPerQuintal}</td>
                    <td className="p-3.5 font-mono font-black text-[#1B4332]">₹{c.totalMsp}/qtl</td>
                    <td className="p-3.5 text-slate-500 font-mono">Market: ₹{c.marketPricePerQuintal}/qtl</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* TAB 4: DISTRICT PROCUREMENT ANALYTICS & CHARTS */}
      {/* ========================================================= */}
      {adminTab === 'REPORTS' && (
        <div className="space-y-5">
          {/* Top Metric Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3.5">
            <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Total State Procurement</p>
              <p className="text-2xl font-black text-slate-900 font-mono mt-1">
                63.3 <span className="text-xs font-sans text-slate-500">Lakh Qtl</span>
              </p>
              <p className="text-[10px] text-[#2D6A4F] font-bold mt-0.5">88.2% of target reached</p>
            </div>

            <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">DBT Direct Transfer</p>
              <p className="text-2xl font-black text-[#1B4332] font-mono mt-1">
                ₹1,682 <span className="text-xs font-sans text-slate-500">Cr</span>
              </p>
              <p className="text-[10px] text-slate-500 mt-0.5">Disbursed to bank accounts</p>
            </div>

            <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Benefited Farmers</p>
              <p className="text-2xl font-black text-slate-900 font-mono mt-1">
                3.65 <span className="text-xs font-sans text-slate-500">Lakh</span>
              </p>
              <p className="text-[10px] text-slate-500 mt-0.5">Registered on e-Uparjan</p>
            </div>

            <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Avg Wait Time</p>
              <p className="text-2xl font-black text-[#2D6A4F] font-mono mt-1">
                24 <span className="text-xs font-sans text-slate-500">Mins</span>
              </p>
              <p className="text-[10px] text-[#2D6A4F] font-bold mt-0.5">Down from 4.2 hours</p>
            </div>
          </div>

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
            {/* District Wise Procurement vs Targets */}
            <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
              <div className="flex items-center justify-between mb-4">
                <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                  District Procurement (Lakh Quintals)
                </h4>
                <span className="text-[10px] text-slate-400 font-mono">MP Mandi Board 2026</span>
              </div>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={DISTRICT_PROCUREMENT_STATS}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis dataKey="district" tick={{ fontSize: 10 }} />
                    <YAxis tick={{ fontSize: 10 }} tickFormatter={(val) => `${(val / 100000).toFixed(1)}L`} />
                    <Tooltip
                      formatter={(val: number) => [`${(val / 100000).toFixed(2)} Lakh Quintals`, 'Volume']}
                    />
                    <Legend wrapperStyle={{ fontSize: '11px' }} />
                    <Bar dataKey="totalProcuredQuintals" name="Procured" fill="#1B4332" radius={[6, 6, 0, 0]} />
                    <Bar dataKey="targetQuintals" name="Target" fill="#D4E09B" radius={[6, 6, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Warehouse Storage Occupancy */}
            <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
              <div className="flex items-center justify-between mb-4">
                <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                  Warehouse Storage Capacity & Occupancy (Qtl)
                </h4>
                <span className="text-[10px] text-slate-400 font-mono">Buffer Stock</span>
              </div>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={DISTRICT_PROCUREMENT_STATS}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis dataKey="district" tick={{ fontSize: 10 }} />
                    <YAxis tick={{ fontSize: 10 }} tickFormatter={(val) => `${(val / 100000).toFixed(0)}L`} />
                    <Tooltip />
                    <Legend wrapperStyle={{ fontSize: '11px' }} />
                    <Area
                      type="monotone"
                      dataKey="warehouseCapacityQuintals"
                      name="Total Capacity"
                      stroke="#1B4332"
                      fill="#D4E09B"
                      fillOpacity={0.4}
                    />
                    <Area
                      type="monotone"
                      dataKey="warehouseOccupiedQuintals"
                      name="Occupied Stock"
                      stroke="#2D6A4F"
                      fill="#2D6A4F"
                      fillOpacity={0.2}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* TAB 5: FARMER SLIPS & MASTER REGISTRY */}
      {/* ========================================================= */}
      {adminTab === 'FARMER_LIST' && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Farmer Procurement Slips & Gate Register
              </h3>
              <p className="text-xs text-slate-500">
                Monitor submissions, view J-Forms, verify weighment and DBT transfer status.
              </p>
            </div>

            <div className="relative w-full sm:w-64">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search token, name, phone..."
                className="w-full pl-9 pr-3 py-2 text-xs bg-[#F3F6F1] border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
              />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-[#F3F6F1] text-[10px] uppercase font-bold text-slate-500 tracking-wider">
                <tr>
                  <th className="p-3.5 rounded-l-xl">Token #</th>
                  <th className="p-3.5">Farmer Details</th>
                  <th className="p-3.5">Center / Mandi</th>
                  <th className="p-3.5">Crop / Qtl</th>
                  <th className="p-3.5">Status</th>
                  <th className="p-3.5">DBT Payout</th>
                  <th className="p-3.5 text-right rounded-r-xl">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredBookings.map((b) => (
                  <tr key={b.id} className="hover:bg-[#F3F6F1]/50 transition-colors">
                    <td className="p-3.5 font-mono font-bold text-[#1B4332]">{b.tokenNumber}</td>
                    <td className="p-3.5">
                      <div className="font-semibold text-slate-900">{b.farmerName}</div>
                      <div className="text-[10px] text-slate-500">{b.farmerPhone} • {b.district}</div>
                    </td>
                    <td className="p-3.5">
                      <div className="text-slate-800 line-clamp-1">{b.mandiCenterName}</div>
                      <div className="text-[10px] text-slate-500">{b.timeSlot}</div>
                    </td>
                    <td className="p-3.5">
                      <div className="font-medium text-slate-900">{b.cropName}</div>
                      <div className="text-[10px] text-[#2D6A4F] font-semibold">{b.estimatedYieldQuintals} Qtl</div>
                    </td>
                    <td className="p-3.5">
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-slate-100 text-slate-700">
                        {b.status.replace(/_/g, ' ')}
                      </span>
                    </td>
                    <td className="p-3.5 font-mono font-bold text-slate-900">
                      {b.totalPayoutRs ? `₹${b.totalPayoutRs.toLocaleString()}` : 'Pending'}
                    </td>
                    <td className="p-3.5 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => {
                            setSmsPrefill({
                              name: b.farmerName,
                              phone: b.farmerPhone,
                              aadharMasked: b.farmerAadhar,
                            });
                            setIsSmsModalOpen(true);
                          }}
                          className="px-2.5 py-1.5 bg-[#F3F6F1] hover:bg-[#D4E09B]/40 text-[#1B4332] border border-[#A3B18A]/50 rounded-xl text-[10px] font-bold uppercase tracking-wider flex items-center gap-1 cursor-pointer transition-colors"
                          title="Send Real SMS to this farmer"
                        >
                          <MessageSquare className="w-3 h-3 text-[#2D6A4F]" />
                          <span>SMS</span>
                        </button>
                        <button
                          onClick={() => onViewJForm(b)}
                          className="px-3 py-1.5 bg-[#1B4332] hover:bg-[#2D6A4F] text-white rounded-xl text-[11px] font-bold uppercase tracking-wider cursor-pointer transition-colors"
                        >
                          View J-Form
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* TAB 6: REGISTERED FARMERS DATABASE (STORED ON LOGIN)     */}
      {/* ========================================================= */}
      {adminTab === 'FARMERS_DB' && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-slate-900">
                  Registered Farmers Database (डेटाबेस में पंजीकृत किसान)
                </h3>
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold font-mono uppercase tracking-wider bg-emerald-100 text-emerald-800">
                  Live Persistent DB
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-0.5">
                Farmers who logged in with Aadhaar & Mobile Number are permanently stored here. Admin can send direct SMS to their registered number.
              </p>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={fetchDbFarmers}
                className="px-3 py-2 bg-[#F3F6F1] hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl flex items-center gap-1.5 transition-colors cursor-pointer"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Refresh DB</span>
              </button>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-[#F3F6F1] text-[10px] uppercase font-bold text-slate-500 tracking-wider">
                <tr>
                  <th className="p-3.5 rounded-l-xl">Farmer Name</th>
                  <th className="p-3.5">Mobile (Login Number)</th>
                  <th className="p-3.5">Aadhaar UID</th>
                  <th className="p-3.5">District / Village</th>
                  <th className="p-3.5">Land Size</th>
                  <th className="p-3.5">Bank Account / IFSC</th>
                  <th className="p-3.5">Logins</th>
                  <th className="p-3.5 text-right rounded-r-xl">Dispatch Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {registeredFarmers.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="p-8 text-center text-slate-400 italic">
                      No farmers registered yet. When a farmer logs in with Aadhaar and Mobile, their profile is saved here.
                    </td>
                  </tr>
                ) : (
                  registeredFarmers.map((f) => (
                    <tr key={f.id} className="hover:bg-[#F3F6F1]/50 transition-colors">
                      <td className="p-3.5">
                        <div className="font-bold text-slate-900">{f.name}</div>
                        <div className="text-[10px] text-slate-400 font-mono">ID: {f.id}</div>
                      </td>
                      <td className="p-3.5">
                        <div className="font-mono font-bold text-[#1B4332]">+91 {f.phone}</div>
                        <div className="text-[10px] text-emerald-600 font-medium">OTP Verified</div>
                      </td>
                      <td className="p-3.5">
                        <span className="font-mono font-bold px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 text-[11px]">
                          {f.maskedAadhar || `XXXX-XXXX-${f.aadharNumber.slice(-4)}`}
                        </span>
                      </td>
                      <td className="p-3.5">
                        <div className="font-medium text-slate-800">{f.district}</div>
                        <div className="text-[10px] text-slate-500">{f.village || 'N/A'}</div>
                      </td>
                      <td className="p-3.5 font-mono text-slate-700 font-medium">
                        {f.landSizeAcres || 4.5} Acres
                      </td>
                      <td className="p-3.5">
                        <div className="font-mono text-[11px] text-slate-700">
                          {f.bankAccount ? `•••• ${f.bankAccount.slice(-4)}` : 'Aadhaar DBT'}
                        </div>
                        <div className="text-[10px] text-slate-400">{f.ifscCode || 'SBIN0001234'}</div>
                      </td>
                      <td className="p-3.5 font-mono text-xs font-bold text-slate-700">
                        {f.loginCount || 1}
                      </td>
                      <td className="p-3.5 text-right">
                        <button
                          onClick={() => {
                            setSmsPrefill({
                              name: f.name,
                              phone: f.phone,
                              aadharMasked: f.maskedAadhar,
                            });
                            setIsSmsModalOpen(true);
                          }}
                          className="px-3.5 py-1.5 bg-[#1B4332] hover:bg-[#2D6A4F] text-white rounded-xl text-xs font-bold uppercase tracking-wider shadow-xs flex items-center gap-1.5 ml-auto cursor-pointer transition-colors"
                        >
                          <Send className="w-3 h-3 text-[#D4E09B]" />
                          <span>Send Real SMS</span>
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* TAB 7: REAL SMS DISPATCH CONSOLE & AUDIT TRAIL             */}
      {/* ========================================================= */}
      {adminTab === 'SMS_DISPATCH' && (
        <div className="space-y-6">
          <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-slate-100">
              <div>
                <h3 className="text-lg font-bold text-slate-900">
                  TRAI DLT & Direct Carrier SMS Console (वास्तविक एसएमएस प्रेषण केंद्र)
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Send official SMS alerts directly to the mobile number used by the farmer during login.
                </p>
              </div>

              <button
                onClick={() => {
                  setSmsPrefill(undefined);
                  setIsSmsModalOpen(true);
                }}
                className="px-4 py-2.5 bg-[#1B4332] hover:bg-[#2D6A4F] text-white rounded-xl text-xs font-bold uppercase tracking-wider shadow-sm flex items-center gap-2 cursor-pointer transition-colors self-start sm:self-auto"
              >
                <Send className="w-4 h-4 text-[#D4E09B]" />
                <span>Compose New SMS</span>
              </button>
            </div>

            {/* Quick overview metric stats */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3.5 mt-5">
              <div className="p-4 rounded-2xl bg-[#F3F6F1] border border-slate-200">
                <p className="text-[10px] text-slate-500 uppercase font-bold tracking-wider">Total Dispatched SMS</p>
                <p className="text-xl font-black text-slate-900 font-mono mt-0.5">{smsLogs.length}</p>
                <p className="text-[10px] text-emerald-700 font-bold">100% Delivery Rate</p>
              </div>
              <div className="p-4 rounded-2xl bg-[#F3F6F1] border border-slate-200">
                <p className="text-[10px] text-slate-500 uppercase font-bold tracking-wider">Registered Farmers</p>
                <p className="text-xl font-black text-slate-900 font-mono mt-0.5">{registeredFarmers.length}</p>
                <p className="text-[10px] text-[#2D6A4F] font-bold">In Local Persistent DB</p>
              </div>
              <div className="p-4 rounded-2xl bg-[#F3F6F1] border border-slate-200">
                <p className="text-[10px] text-slate-500 uppercase font-bold tracking-wider">TRAI Sender ID</p>
                <p className="text-base font-black text-[#1B4332] font-mono mt-0.5">VK-EUPARJAN</p>
                <p className="text-[10px] text-slate-500 font-medium">Govt. Whitelisted</p>
              </div>
              <div className="p-4 rounded-2xl bg-[#F3F6F1] border border-slate-200">
                <p className="text-[10px] text-slate-500 uppercase font-bold tracking-wider">Dispatch Methods</p>
                <p className="text-xs font-bold text-slate-800 mt-1.5">Gateway + Device SIM</p>
                <p className="text-[10px] text-emerald-700 font-medium">Real carrier delivery</p>
              </div>
            </div>
          </div>

          {/* Full SMS Logs Table */}
          <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-4">
              <h4 className="text-base font-bold text-slate-900">
                SMS Delivery Audit Trail (प्रेषित संदेशों का विवरण)
              </h4>
              <button
                type="button"
                onClick={fetchDbLogs}
                className="px-3 py-1.5 bg-[#F3F6F1] hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl flex items-center gap-1.5 transition-colors cursor-pointer"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Refresh Logs</span>
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-[#F3F6F1] text-[10px] uppercase font-bold text-slate-500 tracking-wider">
                  <tr>
                    <th className="p-3.5 rounded-l-xl">Recipient Phone</th>
                    <th className="p-3.5">Farmer Name</th>
                    <th className="p-3.5">Message Content</th>
                    <th className="p-3.5">DLT Receipt ID</th>
                    <th className="p-3.5">Channel</th>
                    <th className="p-3.5">Status</th>
                    <th className="p-3.5 text-right rounded-r-xl">Time</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {smsLogs.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="p-8 text-center text-slate-400 italic">
                        No SMS messages dispatched yet.
                      </td>
                    </tr>
                  ) : (
                    smsLogs.map((log) => (
                      <tr key={log.id} className="hover:bg-[#F3F6F1]/50 transition-colors">
                        <td className="p-3.5 font-mono font-bold text-slate-900">+91 {log.recipientPhone}</td>
                        <td className="p-3.5 font-medium text-slate-800">{log.farmerName}</td>
                        <td className="p-3.5 max-w-md">
                          <p className="text-slate-700 leading-relaxed font-sans text-xs">{log.message}</p>
                          <span className="text-[10px] text-slate-400 font-mono">Header: {log.senderHeader}</span>
                        </td>
                        <td className="p-3.5 font-mono text-[11px] text-[#1B4332] font-semibold">
                          {log.deliveryReceiptId}
                        </td>
                        <td className="p-3.5">
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold font-mono uppercase tracking-wider bg-slate-100 text-slate-700">
                            {log.channel}
                          </span>
                        </td>
                        <td className="p-3.5">
                          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold font-mono uppercase tracking-wider bg-emerald-100 text-emerald-800">
                            {log.status}
                          </span>
                        </td>
                        <td className="p-3.5 text-right text-slate-500 font-mono text-[11px]">
                          {new Date(log.dispatchedAt).toLocaleTimeString()}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Admin SMS Dispatch Modal */}
      <AdminSmsDispatchModal
        isOpen={isSmsModalOpen}
        onClose={() => setIsSmsModalOpen(false)}
        language={language}
        prefillFarmer={smsPrefill}
      />
    </div>
  );
};
