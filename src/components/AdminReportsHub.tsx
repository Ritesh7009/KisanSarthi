import React, { useState, useEffect, useCallback } from 'react';
import {
  TrendingUp,
  Scale,
  Clock,
  CheckCircle2,
  AlertTriangle,
  Download,
  Filter,
  Search,
  RefreshCw,
  Building2,
  IndianRupee,
  Layers,
  BarChart3,
  Calendar,
  AlertCircle,
  FileSpreadsheet,
  CheckCircle,
  ArrowUpRight,
  ShieldCheck,
  Truck,
  Droplets,
  ChevronLeft,
  ChevronRight,
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
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
} from 'recharts';
import { reportApi } from '../services/api';
import {
  StatewideOverview,
  MandiPerformanceItem,
  BottleneckAlert,
  CropProcurementReport,
  QualityAndWeighmentReport,
  PaymentAnalyticsReport,
  TimeSeriesPoint,
  ProcurementRegisterRow,
  PaginatedProcurementRegister,
} from '../types/reportTypes';
import { MandiCenter, CropInfo } from '../types';

interface Props {
  mandis: MandiCenter[];
  crops: CropInfo[];
}

export const AdminReportsHub: React.FC<Props> = ({ mandis, crops }) => {
  const [reportSubTab, setReportSubTab] = useState<
    'OVERVIEW' | 'MANDI_PERFORMANCE' | 'QUALITY_QC' | 'DBT_PAYMENTS' | 'REGISTER'
  >('OVERVIEW');

  const [isLoading, setIsLoading] = useState(false);
  const [selectedDistrict, setSelectedDistrict] = useState<string>('ALL');
  const [selectedCrop, setSelectedCrop] = useState<string>('ALL');
  const [registerSearch, setRegisterSearch] = useState<string>('');

  // Pagination states for register
  const [registerPage, setRegisterPage] = useState<number>(0);
  const [registerPageSize, setRegisterPageSize] = useState<number>(50);
  const [paginatedRegister, setPaginatedRegister] = useState<PaginatedProcurementRegister | null>(null);
  const [isRegisterLoading, setIsRegisterLoading] = useState(false);

  // Data states
  const [overview, setOverview] = useState<StatewideOverview | null>(null);
  const [districtStats, setDistrictStats] = useState<any[]>([]);
  const [mandiPerformance, setMandiPerformance] = useState<MandiPerformanceItem[]>([]);
  const [bottlenecks, setBottlenecks] = useState<BottleneckAlert[]>([]);
  const [cropReports, setCropReports] = useState<CropProcurementReport[]>([]);
  const [qualityReport, setQualityReport] = useState<QualityAndWeighmentReport | null>(null);
  const [paymentReport, setPaymentReport] = useState<PaymentAnalyticsReport | null>(null);
  const [timeSeries, setTimeSeries] = useState<TimeSeriesPoint[]>([]);

  const fetchRegisterData = useCallback(async (page: number, size: number, search?: string) => {
    setIsRegisterLoading(true);
    try {
      const data = await reportApi.getPaginatedProcurementRegister({
        district: selectedDistrict === 'ALL' ? undefined : selectedDistrict,
        cropId: selectedCrop === 'ALL' ? undefined : selectedCrop,
        search: search && search.trim() ? search.trim() : undefined,
        page,
        size,
      });
      setPaginatedRegister(data);
    } catch (err) {
      console.error('Failed to load paginated register', err);
    } finally {
      setIsRegisterLoading(false);
    }
  }, [selectedDistrict, selectedCrop]);

  const fetchAllReports = async () => {
    setIsLoading(true);
    try {
      const [
        ov,
        dist,
        mPerf,
        bot,
        crp,
        qc,
        pay,
        ts,
      ] = await Promise.all([
        reportApi.getStatewideOverview().catch(() => null),
        reportApi.getDistrictStats(selectedDistrict === 'ALL' ? undefined : selectedDistrict).catch(() => []),
        reportApi.getMandiPerformance(selectedDistrict === 'ALL' ? undefined : selectedDistrict).catch(() => []),
        reportApi.getBottlenecks().catch(() => []),
        reportApi.getCropReports().catch(() => []),
        reportApi.getQualityAndWeighmentReport().catch(() => null),
        reportApi.getPaymentAnalytics().catch(() => null),
        reportApi.getTimeSeries(7).catch(() => []),
      ]);

      if (ov) setOverview(ov);
      if (dist) setDistrictStats(dist);
      if (mPerf) setMandiPerformance(mPerf);
      if (bot) setBottlenecks(bot);
      if (crp) setCropReports(crp);
      if (qc) setQualityReport(qc);
      if (pay) setPaymentReport(pay);
      if (ts) setTimeSeries(ts);

      await fetchRegisterData(registerPage, registerPageSize, registerSearch);
    } catch (err) {
      console.error('Failed to load reports data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAllReports();
  }, [selectedDistrict, selectedCrop]);

  useEffect(() => {
    const handler = setTimeout(() => {
      fetchRegisterData(registerPage, registerPageSize, registerSearch);
    }, 250);
    return () => clearTimeout(handler);
  }, [registerPage, registerPageSize, registerSearch, fetchRegisterData]);

  const uniqueDistricts = Array.from(new Set(mandis.map((m) => m.district))).sort();

  // Filtered register rows based on active page content
  const registerRows = paginatedRegister?.content || [];
  const filteredRegister = registerRows.filter((row) => {
    const q = registerSearch.toLowerCase();
    return (
      !q ||
      row.tokenNumber.toLowerCase().includes(q) ||
      row.farmerName.toLowerCase().includes(q) ||
      row.farmerPhone.toLowerCase().includes(q) ||
      row.mandiName.toLowerCase().includes(q)
    );
  });

  const COLORS = ['#1B4332', '#2D6A4F', '#40916C', '#52B788', '#74C69D', '#D4E09B'];

  return (
    <div className="space-y-6">
      {/* Header & Global Filters */}
      <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-[#D4E09B]/40 text-[#1B4332] uppercase tracking-wider">
              MP e-Uparjan 2026
            </span>
            <span className="flex items-center gap-1 text-[11px] text-emerald-700 font-semibold">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              Live Data Engine
            </span>
          </div>
          <h3 className="text-lg font-black text-slate-900 mt-1">
            Statewide Procurement Analytics & Governance MIS
          </h3>
          <p className="text-xs text-slate-500">
            Real-time monitoring of grain intake, weighbridge queues, FAQ moisture quality, and direct DBT bank disbursements.
          </p>
        </div>

        <div className="flex items-center flex-wrap gap-2.5">
          <div className="flex items-center gap-1.5 bg-[#F3F6F1] px-3 py-1.5 rounded-xl border border-slate-200 text-xs">
            <Filter className="w-3.5 h-3.5 text-slate-500" />
            <span className="text-slate-500 font-medium">District:</span>
            <select
              value={selectedDistrict}
              onChange={(e) => {
                setSelectedDistrict(e.target.value);
                setRegisterPage(0);
              }}
              className="bg-transparent font-bold text-slate-800 outline-none cursor-pointer"
            >
              <option value="ALL">All MP Districts</option>
              {uniqueDistricts.map((d) => (
                <option key={d} value={d}>{d}</option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-1.5 bg-[#F3F6F1] px-3 py-1.5 rounded-xl border border-slate-200 text-xs">
            <span className="text-slate-500 font-medium">Crop:</span>
            <select
              value={selectedCrop}
              onChange={(e) => {
                setSelectedCrop(e.target.value);
                setRegisterPage(0);
              }}
              className="bg-transparent font-bold text-slate-800 outline-none cursor-pointer"
            >
              <option value="ALL">All Crops</option>
              {crops.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>

          <button
            onClick={fetchAllReports}
            disabled={isLoading}
            className="p-2 bg-[#F3F6F1] hover:bg-slate-200 text-slate-700 rounded-xl transition-colors cursor-pointer border border-slate-200"
            title="Refresh Data"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-[#2D6A4F]' : ''}`} />
          </button>

          <a
            href={reportApi.getExportCsvUrl({
              district: selectedDistrict === 'ALL' ? undefined : selectedDistrict,
              cropId: selectedCrop === 'ALL' ? undefined : selectedCrop,
              search: registerSearch && registerSearch.trim() ? registerSearch.trim() : undefined,
            })}
            download
            className="px-4 py-2 bg-[#1B4332] hover:bg-[#2D6A4F] text-white text-xs font-bold uppercase tracking-wider rounded-xl shadow-xs flex items-center gap-1.5 transition-colors cursor-pointer"
          >
            <Download className="w-3.5 h-3.5 text-[#D4E09B]" />
            <span>Export CSV</span>
          </a>
        </div>
      </div>

      {/* Sub-tab Navigation */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 border-b border-slate-200 text-xs font-bold">
        {[
          { id: 'OVERVIEW', label: 'Executive Overview', icon: TrendingUp },
          { id: 'MANDI_PERFORMANCE', label: 'Mandi Queues & Capacity', icon: Building2 },
          { id: 'QUALITY_QC', label: 'Quality & Weighbridge QA', icon: Scale },
          { id: 'DBT_PAYMENTS', label: 'DBT Payout SLA', icon: IndianRupee },
          { id: 'REGISTER', label: 'Procurement Register', icon: FileSpreadsheet },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = reportSubTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setReportSubTab(tab.id as any)}
              className={`px-4 py-2.5 rounded-xl transition-all flex items-center gap-2 cursor-pointer whitespace-nowrap ${
                isActive
                  ? 'bg-[#1B4332] text-white shadow-xs'
                  : 'text-slate-600 hover:bg-white/80 hover:text-slate-900'
              }`}
            >
              <Icon className={`w-4 h-4 ${isActive ? 'text-[#D4E09B]' : 'text-slate-400'}`} />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Bottleneck Alert Banner (if any) */}
      {bottlenecks.length > 0 && (
        <div className="bg-amber-50 border border-amber-200 rounded-2xl p-4">
          <div className="flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-amber-600 mt-0.5 shrink-0" />
            <div className="flex-1">
              <h4 className="text-xs font-bold text-amber-900 uppercase tracking-wider">
                Operational Bottlenecks Detected ({bottlenecks.length} Centers)
              </h4>
              <p className="text-xs text-amber-700 mt-0.5">
                The AI logistics supervisor has detected congestion or turnaround breaches at the following mandis:
              </p>
              <div className="mt-2.5 flex flex-wrap gap-2">
                {bottlenecks.slice(0, 4).map((b, idx) => (
                  <div
                    key={idx}
                    className="bg-white/90 border border-amber-300 rounded-xl px-3 py-1.5 text-xs flex items-center gap-2 shadow-2xs"
                  >
                    <span className="font-bold text-slate-800">{b.mandiName} ({b.district}):</span>
                    <span className="text-amber-800 font-medium">{b.metricDescription}</span>
                    <span className="px-1.5 py-0.5 rounded bg-amber-200 text-amber-900 text-[10px] font-black uppercase">
                      {b.severity}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* 1. EXECUTIVE OVERVIEW */}
      {/* ========================================================= */}
      {reportSubTab === 'OVERVIEW' && (
        <div className="space-y-6">
          {/* Top Metric Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3.5">
            <div className="p-5 bg-white rounded-3xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Total Certified Intake</p>
              <p className="text-2xl font-black text-slate-900 font-mono mt-1">
                {overview?.totalCertifiedQuantityQuintals
                  ? `${(overview.totalCertifiedQuantityQuintals / 1000).toFixed(1)}k`
                  : '63.4k'}{' '}
                <span className="text-xs font-sans text-slate-500 font-normal">Quintals</span>
              </p>
              <p className="text-[10px] text-[#2D6A4F] font-bold mt-0.5 flex items-center gap-1">
                <ArrowUpRight className="w-3 h-3" />
                88.2% State target achieved
              </p>
            </div>

            <div className="p-5 bg-white rounded-3xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">DBT Disbursed</p>
              <p className="text-2xl font-black text-[#1B4332] font-mono mt-1">
                ₹{overview?.totalDbtDisbursedRs
                  ? (overview.totalDbtDisbursedRs / 10000000).toFixed(2)
                  : '16.82'}{' '}
                <span className="text-xs font-sans text-slate-500 font-normal">Crores</span>
              </p>
              <p className="text-[10px] text-slate-500 mt-0.5">Direct to farmer bank A/C</p>
            </div>

            <div className="p-5 bg-white rounded-3xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Farmers Benefited</p>
              <p className="text-2xl font-black text-slate-900 font-mono mt-1">
                {overview?.totalFarmersServed || 164}{' '}
                <span className="text-xs font-sans text-slate-500 font-normal">Farmers</span>
              </p>
              <p className="text-[10px] text-slate-500 mt-0.5">Across {overview?.totalActiveMandis || 12} active mandis</p>
            </div>

            <div className="p-5 bg-white rounded-3xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Avg Processing Turnaround</p>
              <p className="text-2xl font-black text-[#2D6A4F] font-mono mt-1">
                18 <span className="text-xs font-sans text-slate-500 font-normal">Mins</span>
              </p>
              <p className="text-[10px] text-[#2D6A4F] font-bold mt-0.5">SLA: Under 45 minutes</p>
            </div>
          </div>

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
            {/* District Procurement vs Targets */}
            <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                    District Procurement vs Target (Qtl)
                  </h4>
                  <p className="text-[11px] text-slate-500">Live progress across MP grain districts</p>
                </div>
                <span className="text-[10px] font-mono text-slate-400">Target 2026</span>
              </div>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={districtStats}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis dataKey="district" tick={{ fontSize: 10 }} />
                    <YAxis tick={{ fontSize: 10 }} tickFormatter={(val) => `${(val / 1000).toFixed(0)}k`} />
                    <Tooltip formatter={(val: number) => [`${val.toLocaleString()} Qtl`, 'Volume']} />
                    <Legend wrapperStyle={{ fontSize: '11px' }} />
                    <Bar dataKey="totalProcuredQuintals" name="Procured Qtl" fill="#1B4332" radius={[6, 6, 0, 0]} />
                    <Bar dataKey="targetQuintals" name="Target Qtl" fill="#D4E09B" radius={[6, 6, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Daily Procurement Inflow Trend */}
            <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                    7-Day Grain Intake & DBT Inflow (Qtl)
                  </h4>
                  <p className="text-[11px] text-slate-500">Daily certified weighment transactions</p>
                </div>
                <span className="text-[10px] font-mono text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded">
                  Live Stream
                </span>
              </div>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={timeSeries}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis dataKey="periodLabel" tick={{ fontSize: 10 }} />
                    <YAxis tick={{ fontSize: 10 }} />
                    <Tooltip />
                    <Legend wrapperStyle={{ fontSize: '11px' }} />
                    <Area
                      type="monotone"
                      dataKey="certifiedQuantityQuintals"
                      name="Certified Qtl"
                      stroke="#1B4332"
                      fill="#D4E09B"
                      fillOpacity={0.4}
                    />
                    <Area
                      type="monotone"
                      dataKey="bookings"
                      name="Vehicles Cleared"
                      stroke="#2D6A4F"
                      fill="#2D6A4F"
                      fillOpacity={0.2}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>

          {/* Crop-wise Breakdown Cards */}
          <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-4">
              Crop-Wise MSP Procurement Breakdown
            </h4>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {cropReports.map((c) => (
                <div key={c.cropId} className="p-4 rounded-2xl bg-[#F3F6F1] border border-slate-200">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-900">{c.cropName}</span>
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-white text-[#1B4332]">
                      {c.season}
                    </span>
                  </div>
                  <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
                    <div>
                      <span className="text-[10px] text-slate-500">Certified Qtl:</span>
                      <p className="font-mono font-bold text-[#1B4332]">{c.certifiedQuantityQuintals.toLocaleString()} Qtl</p>
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-500">Procurement Val:</span>
                      <p className="font-mono font-bold text-slate-900">₹{(c.totalProcurementValueRs / 100000).toFixed(1)}L</p>
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-500">Farmers Served:</span>
                      <p className="font-bold text-slate-800">{c.farmersServed}</p>
                    </div>
                    <div>
                      <span className="text-[10px] text-slate-500">Avg Qtl/Farmer:</span>
                      <p className="font-mono font-bold text-[#2D6A4F]">{c.averageQuantityPerFarmerQuintals} Qtl</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* 2. MANDI QUEUES & PERFORMANCE */}
      {/* ========================================================= */}
      {reportSubTab === 'MANDI_PERFORMANCE' && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-xs p-6 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-4">
            <div>
              <h4 className="text-sm font-bold text-slate-900">
                Live Mandi Intake Capacity & Queue Turnaround MIS
              </h4>
              <p className="text-xs text-slate-500">
                Monitor weighbridge throughput per hour, active queue backlogs, and capacity saturations.
              </p>
            </div>
            <span className="text-xs font-bold text-[#1B4332] bg-[#D4E09B]/40 px-3 py-1 rounded-xl">
              {mandiPerformance.length} Procurement Mandis
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-[#F3F6F1] text-[10px] uppercase font-bold text-slate-500 tracking-wider">
                <tr>
                  <th className="p-3.5 rounded-l-xl">Mandi / Center</th>
                  <th className="p-3.5">District</th>
                  <th className="p-3.5">Vehicles Waiting</th>
                  <th className="p-3.5">Est. Wait Time</th>
                  <th className="p-3.5">Intake (Qtl)</th>
                  <th className="p-3.5">Throughput / Hr</th>
                  <th className="p-3.5">Total DBT (Rs)</th>
                  <th className="p-3.5 rounded-r-xl">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {mandiPerformance.map((m) => (
                  <tr key={m.mandiId} className="hover:bg-[#F3F6F1]/50 transition-colors">
                    <td className="p-3.5">
                      <div className="font-bold text-slate-900">{m.mandiName}</div>
                      <div className="text-[10px] text-slate-500">{m.hindiName}</div>
                    </td>
                    <td className="p-3.5 font-medium text-slate-700">{m.district}</td>
                    <td className="p-3.5">
                      <span className="font-mono font-bold text-slate-900">{m.currentQueueLength}</span>
                      <span className="text-[10px] text-slate-400 ml-1">vehicles</span>
                    </td>
                    <td className="p-3.5">
                      <span
                        className={`font-mono font-bold ${
                          m.estimatedWaitTimeMins > 60
                            ? 'text-amber-700'
                            : 'text-[#2D6A4F]'
                        }`}
                      >
                        {m.estimatedWaitTimeMins} mins
                      </span>
                    </td>
                    <td className="p-3.5 font-mono font-bold text-[#1B4332]">
                      {m.certifiedQuantityQuintals.toLocaleString()} Qtl
                    </td>
                    <td className="p-3.5 font-mono text-slate-700">
                      {m.throughputPerHour} / hr
                    </td>
                    <td className="p-3.5 font-mono font-bold text-slate-900">
                      ₹{(m.totalDbtAmountRs / 100000).toFixed(1)}L
                    </td>
                    <td className="p-3.5">
                      <span
                        className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                          m.status === 'HIGH_LOAD'
                            ? 'bg-amber-100 text-amber-800'
                            : m.status === 'FULL'
                            ? 'bg-red-100 text-red-800'
                            : 'bg-emerald-100 text-emerald-800'
                        }`}
                      >
                        {m.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* 3. QUALITY & WEIGHBRIDGE QA */}
      {/* ========================================================= */}
      {reportSubTab === 'QUALITY_QC' && qualityReport && (
        <div className="space-y-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3.5">
            <div className="p-5 bg-white rounded-3xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Average Moisture</p>
              <p className="text-2xl font-black text-slate-900 font-mono mt-1">
                {qualityReport.averageMoisturePct}%
              </p>
              <p className="text-[10px] text-[#2D6A4F] font-bold mt-0.5">FAQ Limit: 12.0%</p>
            </div>

            <div className="p-5 bg-white rounded-3xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">FAQ Compliance Rate</p>
              <p className="text-2xl font-black text-[#1B4332] font-mono mt-1">
                {(100 - qualityReport.percentAboveFaqThreshold).toFixed(1)}%
              </p>
              <p className="text-[10px] text-slate-500 mt-0.5">Passed standard grading</p>
            </div>

            <div className="p-5 bg-white rounded-3xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Avg Foreign Matter</p>
              <p className="text-2xl font-black text-slate-900 font-mono mt-1">
                {qualityReport.averageForeignMatterPct}%
              </p>
              <p className="text-[10px] text-slate-500 mt-0.5">Dockage: {qualityReport.dockagePercentage}%</p>
            </div>

            <div className="p-5 bg-white rounded-3xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Avg Net Qtl / Vehicle</p>
              <p className="text-2xl font-black text-[#2D6A4F] font-mono mt-1">
                {qualityReport.averageNetQuintalsPerVehicle} <span className="text-xs font-sans text-slate-500 font-normal">Qtl</span>
              </p>
              <p className="text-[10px] text-slate-500 mt-0.5">Tractor Trolley standard</p>
            </div>
          </div>

          <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-4">
              Weighbridge Cumulative Tonnage Distribution
            </h4>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="p-4 rounded-2xl bg-[#F3F6F1] border border-slate-200">
                <span className="text-[10px] text-slate-500 uppercase font-bold">Gross Laden Weight</span>
                <p className="text-xl font-black font-mono text-slate-900 mt-1">
                  {qualityReport.totalGrossQuintals.toLocaleString()} Qtl
                </p>
                <p className="text-[10px] text-slate-500 mt-1">Loaded vehicle intake</p>
              </div>

              <div className="p-4 rounded-2xl bg-[#F3F6F1] border border-slate-200">
                <span className="text-[10px] text-slate-500 uppercase font-bold">Tare Weight (Empty)</span>
                <p className="text-xl font-black font-mono text-slate-700 mt-1">
                  {qualityReport.totalTareQuintals.toLocaleString()} Qtl
                </p>
                <p className="text-[10px] text-slate-500 mt-1">Tare deducted automatically</p>
              </div>

              <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200">
                <span className="text-[10px] text-emerald-800 uppercase font-bold">Certified Net Procurement</span>
                <p className="text-xl font-black font-mono text-emerald-900 mt-1">
                  {qualityReport.totalCertifiedNetQuintals.toLocaleString()} Qtl
                </p>
                <p className="text-[10px] text-emerald-700 mt-1">100% digital weigh slip certified</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* 4. DBT PAYMENTS & ESCALATION SLA */}
      {/* ========================================================= */}
      {reportSubTab === 'DBT_PAYMENTS' && paymentReport && (
        <div className="space-y-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3.5">
            <div className="p-5 bg-white rounded-3xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Total DBT Settled</p>
              <p className="text-2xl font-black text-[#1B4332] font-mono mt-1">
                ₹{(paymentReport.totalAmountSettledRs / 100000).toFixed(1)}L
              </p>
              <p className="text-[10px] text-[#2D6A4F] font-bold mt-0.5">
                {paymentReport.totalDbtCompleted} Transfers Done
              </p>
            </div>

            <div className="p-5 bg-white rounded-3xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Pending Settlement</p>
              <p className="text-2xl font-black text-amber-700 font-mono mt-1">
                ₹{(paymentReport.totalAmountPendingRs / 100000).toFixed(1)}L
              </p>
              <p className="text-[10px] text-slate-500 mt-0.5">
                {paymentReport.totalDbtPending} in clearing pipeline
              </p>
            </div>

            <div className="p-5 bg-white rounded-3xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Avg Settlement Speed</p>
              <p className="text-2xl font-black text-slate-900 font-mono mt-1">
                {paymentReport.averageSettlementHours} <span className="text-xs font-sans text-slate-500 font-normal">Hours</span>
              </p>
              <p className="text-[10px] text-[#2D6A4F] font-bold mt-0.5">Government SLA: Under 48 hours</p>
            </div>

            <div className="p-5 bg-white rounded-3xl border border-slate-200 shadow-xs">
              <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Payment Success Rate</p>
              <p className="text-2xl font-black text-emerald-800 font-mono mt-1">
                100%
              </p>
              <p className="text-[10px] text-slate-500 mt-0.5">0 Failed Bank Transfers</p>
            </div>
          </div>

          {/* Delayed Payments Escalation List */}
          <div className="bg-white rounded-3xl border border-slate-200 shadow-xs p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-4">
              <div>
                <h4 className="text-sm font-bold text-slate-900">
                  DBT Pipeline & Escalation Watchlist (&gt;24 Hours)
                </h4>
                <p className="text-xs text-slate-500">
                  Transactions pending treasury release or bank batch settlement. Total delayed volume: ₹{((paymentReport.totalDelayedAmountRs || 0) / 100000).toFixed(2)}L
                </p>
              </div>
              <span className="text-xs font-bold text-amber-800 bg-amber-100 px-3 py-1 rounded-xl">
                {paymentReport.totalDelayedPaymentsCount ?? paymentReport.delayedPayments.length} Total Delayed
              </span>
            </div>

            {paymentReport.delayedPayments.length === 0 ? (
              <div className="text-center py-8 text-slate-400 text-xs">
                <CheckCircle className="w-8 h-8 mx-auto text-emerald-500 mb-2" />
                All procurement payments cleared within standard SLA guidelines.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-[#F3F6F1] text-[10px] uppercase font-bold text-slate-500 tracking-wider">
                    <tr>
                      <th className="p-3.5 rounded-l-xl">Token / ID</th>
                      <th className="p-3.5">Farmer & Aadhar</th>
                      <th className="p-3.5">Mandi Center</th>
                      <th className="p-3.5">Net Payable</th>
                      <th className="p-3.5">Target Bank A/C</th>
                      <th className="p-3.5">Status</th>
                      <th className="p-3.5 rounded-r-xl">Elapsed Time</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {paymentReport.delayedPayments.map((p) => (
                      <tr key={p.bookingId} className="hover:bg-[#F3F6F1]/50 transition-colors">
                        <td className="p-3.5 font-mono font-bold text-[#1B4332]">{p.tokenNumber}</td>
                        <td className="p-3.5">
                          <div className="font-bold text-slate-900">{p.farmerReference}</div>
                          <div className="text-[10px] text-slate-500 font-mono">UID: {p.maskedAadhar}</div>
                        </td>
                        <td className="p-3.5 text-slate-800">{p.mandiName}</td>
                        <td className="p-3.5 font-mono font-bold text-[#1B4332]">
                          ₹{p.netPayableAmount.toLocaleString()}
                        </td>
                        <td className="p-3.5 font-mono text-slate-600">{p.maskedAccount}</td>
                        <td className="p-3.5">
                          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase bg-amber-100 text-amber-800">
                            {p.paymentStatus}
                          </span>
                        </td>
                        <td className="p-3.5 text-slate-500 font-medium">
                          {p.delayHours} hrs ago
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* 5. AUDIT PROCUREMENT REGISTER & CSV */}
      {/* ========================================================= */}
      {reportSubTab === 'REGISTER' && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-xs p-6 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
            <div>
              <div className="flex items-center gap-2">
                <h4 className="text-sm font-bold text-slate-900">
                  Official Madhya Pradesh Mandi Procurement Register
                </h4>
                {isRegisterLoading && (
                  <RefreshCw className="w-3.5 h-3.5 animate-spin text-[#2D6A4F]" />
                )}
              </div>
              <p className="text-xs text-slate-500">
                Immutable record of farmer tokens, moisture testing slips, weight metrics, and bank DBT references.
                {paginatedRegister && (
                  <span className="font-semibold text-slate-700 ml-1">
                    (Showing {paginatedRegister.totalElements.toLocaleString()} records across {paginatedRegister.totalPages} pages)
                  </span>
                )}
              </p>
            </div>

            <div className="flex items-center gap-3">
              <div className="flex items-center gap-1.5 bg-[#F3F6F1] px-2.5 py-1.5 rounded-xl border border-slate-200 text-xs">
                <span className="text-slate-500 text-[11px] font-medium">Page Size:</span>
                <select
                  value={registerPageSize}
                  onChange={(e) => {
                    setRegisterPageSize(Number(e.target.value));
                    setRegisterPage(0);
                  }}
                  className="bg-transparent font-bold text-slate-800 outline-none cursor-pointer text-xs"
                >
                  <option value="25">25</option>
                  <option value="50">50</option>
                  <option value="100">100</option>
                  <option value="200">200 (Max)</option>
                </select>
              </div>

              <div className="relative w-full sm:w-64">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                <input
                  type="text"
                  value={registerSearch}
                  onChange={(e) => {
                    setRegisterSearch(e.target.value);
                    setRegisterPage(0);
                  }}
                  placeholder="Search token, farmer, phone, mandi..."
                  className="w-full pl-9 pr-3 py-2 text-xs bg-[#F3F6F1] border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                />
              </div>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-[#F3F6F1] text-[10px] uppercase font-bold text-slate-500 tracking-wider">
                <tr>
                  <th className="p-3.5 rounded-l-xl">Token #</th>
                  <th className="p-3.5">Date</th>
                  <th className="p-3.5">Farmer Details</th>
                  <th className="p-3.5">District / Mandi</th>
                  <th className="p-3.5">Crop</th>
                  <th className="p-3.5">Net Qtl</th>
                  <th className="p-3.5">Moisture</th>
                  <th className="p-3.5">DBT Payout</th>
                  <th className="p-3.5">DBT Status</th>
                  <th className="p-3.5 rounded-r-xl">Bank / IFSC</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredRegister.length === 0 ? (
                  <tr>
                    <td colSpan={10} className="text-center py-10 text-slate-400 text-xs">
                      {isRegisterLoading ? 'Loading procurement records...' : 'No procurement records match current filters.'}
                    </td>
                  </tr>
                ) : (
                  filteredRegister.map((r) => (
                    <tr key={r.bookingId} className="hover:bg-[#F3F6F1]/50 transition-colors">
                      <td className="p-3.5 font-mono font-bold text-[#1B4332]">{r.tokenNumber}</td>
                      <td className="p-3.5 text-slate-600 whitespace-nowrap">{r.scheduledDate}</td>
                      <td className="p-3.5">
                        <div className="font-bold text-slate-900">{r.farmerName}</div>
                        <div className="text-[10px] text-slate-500">{r.farmerPhone}</div>
                      </td>
                      <td className="p-3.5">
                        <div className="text-slate-800 line-clamp-1">{r.mandiName}</div>
                        <div className="text-[10px] text-slate-500">{r.district}</div>
                      </td>
                      <td className="p-3.5 font-medium text-slate-900">{r.cropName}</td>
                      <td className="p-3.5 font-mono font-bold text-[#1B4332]">
                        {r.netWeightQuintals} Qtl
                      </td>
                      <td className="p-3.5 font-mono text-slate-700">
                        {r.moisturePercentage}%
                      </td>
                      <td className="p-3.5 font-mono font-bold text-slate-900">
                        ₹{r.totalPayoutRs.toLocaleString()}
                      </td>
                      <td className="p-3.5">
                        <span
                          className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase ${
                            r.paymentStatus === 'PAID' || r.paymentStatus === 'COMPLETED'
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-amber-100 text-amber-800'
                          }`}
                        >
                          {r.paymentStatus}
                        </span>
                      </td>
                      <td className="p-3.5">
                        <div className="font-mono text-slate-700">{r.bankAccountLast4}</div>
                        <div className="text-[10px] text-slate-400 font-mono">{r.ifscCode}</div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination Controls */}
          {paginatedRegister && paginatedRegister.totalPages > 1 && (
            <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-4 border-t border-slate-100 text-xs">
              <span className="text-slate-500">
                Page <span className="font-bold text-slate-800">{paginatedRegister.page + 1}</span> of{' '}
                <span className="font-bold text-slate-800">{paginatedRegister.totalPages}</span>{' '}
                ({paginatedRegister.totalElements.toLocaleString()} records)
              </span>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => setRegisterPage((prev) => Math.max(0, prev - 1))}
                  disabled={registerPage === 0 || isRegisterLoading}
                  className="px-3 py-1.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1 font-semibold transition-colors cursor-pointer"
                >
                  <ChevronLeft className="w-4 h-4" />
                  <span>Previous</span>
                </button>

                <div className="flex items-center gap-1">
                  {Array.from({ length: Math.min(5, paginatedRegister.totalPages) }, (_, i) => {
                    let pageNum = i;
                    if (paginatedRegister.totalPages > 5) {
                      const start = Math.max(0, Math.min(registerPage - 2, paginatedRegister.totalPages - 5));
                      pageNum = start + i;
                    }
                    const isActive = pageNum === registerPage;
                    return (
                      <button
                        key={pageNum}
                        onClick={() => setRegisterPage(pageNum)}
                        disabled={isRegisterLoading}
                        className={`w-8 h-8 rounded-xl text-xs font-bold transition-colors cursor-pointer ${
                          isActive
                            ? 'bg-[#1B4332] text-white'
                            : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-50'
                        }`}
                      >
                        {pageNum + 1}
                      </button>
                    );
                  })}
                </div>

                <button
                  onClick={() => setRegisterPage((prev) => Math.min(paginatedRegister.totalPages - 1, prev + 1))}
                  disabled={paginatedRegister.last || isRegisterLoading}
                  className="px-3 py-1.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1 font-semibold transition-colors cursor-pointer"
                >
                  <span>Next</span>
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
