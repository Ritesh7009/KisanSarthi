import React, { useState } from 'react';
import {
  Tractor,
  Calendar,
  Clock,
  MapPin,
  Sparkles,
  TrendingUp,
  AlertTriangle,
  QrCode,
  CheckCircle2,
  ChevronRight,
  ShieldCheck,
  Coins,
  FileText,
  CloudSun,
  Loader2,
  RefreshCw,
  Info,
  Layers,
  ArrowUpRight,
  Phone,
  Navigation,
  LocateFixed,
  Compass,
} from 'lucide-react';
import confetti from 'canvas-confetti';
import {
  CropInfo,
  MandiCenter,
  SlotBooking,
  WeatherAlert,
  Language,
  AISlotSuggestion,
  AIYieldAnalysis,
} from '../types';
import { translations } from '../i18n/translations';
import { STANDARD_TIME_SLOTS } from '../data/mpMandiData';
import { aiApi } from '../services/api';
import {
  UserCoordinates,
  MandiWithDistance,
  getBrowserGeolocation,
  findNearestMandi,
  sortMandisByDistance,
  MP_LOCATION_PRESETS,
} from '../utils/geoUtils';

interface Props {
  language: Language;
  crops: CropInfo[];
  mandis: MandiCenter[];
  activeBookings: SlotBooking[];
  onBookSlot: (bookingData: any) => Promise<SlotBooking | null>;
  onViewTokenPass: (booking: SlotBooking) => void;
  onViewJForm: (booking: SlotBooking) => void;
  weatherAlerts: WeatherAlert[];
  currentUser: { name: string; phone: string; district: string } | null;
  isOnline: boolean;
}

export const FarmerDashboard: React.FC<Props> = ({
  language,
  crops,
  mandis,
  activeBookings,
  onBookSlot,
  onViewTokenPass,
  onViewJForm,
  weatherAlerts,
  currentUser,
  isOnline,
}) => {
  const t = translations[language];

  // Active view tab
  const [activeTab, setActiveTab] = useState<
    'TOKEN' | 'BOOK' | 'CALCULATOR' | 'MANDI_QUEUES' | 'MSP_RATES' | 'WEATHER'
  >('TOKEN');

  // Booking Form State
  const defaultDistrict = currentUser?.district || 'Sehore';
  const [district, setDistrict] = useState(defaultDistrict);
  const districtMandis = mandis.filter(
    (m) => m.district.toLowerCase() === district.toLowerCase()
  );
  const [mandiId, setMandiId] = useState(districtMandis[0]?.id || mandis[0]?.id || 'mandi-sehore');
  const [cropId, setCropId] = useState('crop-wheat');
  const [acreage, setAcreage] = useState('4.0');
  const [estimatedYield, setEstimatedYield] = useState('65');
  const [harvestDate, setHarvestDate] = useState(
    new Date(Date.now() + 86400000).toISOString().split('T')[0]
  );
  const [scheduledDate, setScheduledDate] = useState(
    new Date(Date.now() + 86400000).toISOString().split('T')[0]
  );
  const [timeSlot, setTimeSlot] = useState('08:00 AM - 10:00 AM');
  const [vehicleType, setVehicleType] = useState<'TRACTOR_TROLLEY' | 'PICKUP_TRUCK' | 'BULLOCK_CART' | 'TRUCK'>(
    'TRACTOR_TROLLEY'
  );
  const [vehicleNumber, setVehicleNumber] = useState('MP 37 AA 4821');

  // Geolocation & Nearest Mandi State
  const [userLocation, setUserLocation] = useState<UserCoordinates | null>(null);
  const [locationLoading, setLocationLoading] = useState(false);
  const [locationError, setLocationError] = useState<string | null>(null);
  const [locationSuccessMsg, setLocationSuccessMsg] = useState<string | null>(null);

  // Mandis sorted by proximity to user
  const mandisWithDistance: MandiWithDistance[] = userLocation
    ? sortMandisByDistance(userLocation, mandis)
    : mandis.map((m) => ({
        ...m,
        distanceKm: 0,
        distanceFormatted: '',
        travelEstimateMins: 0,
      }));

  const handleDetectLocation = async (presetCoords?: { lat: number; lng: number; label: string }) => {
    setLocationLoading(true);
    setLocationError(null);
    setLocationSuccessMsg(null);

    let coords: UserCoordinates | null = null;

    if (presetCoords) {
      coords = {
        lat: presetCoords.lat,
        lng: presetCoords.lng,
        source: 'PRESET',
        label: presetCoords.label,
      };
    } else {
      const geo = await getBrowserGeolocation();
      if (geo.success && geo.coords) {
        coords = geo.coords;
      } else {
        setLocationError(
          geo.error ||
            (language === 'hi'
              ? 'लोकेशन एक्सेस नहीं मिल सका। कृपया ब्राउज़र अनुमति दें अथवा नीचे दिया गया जिला चुनें।'
              : 'Could not access location. Please allow browser location or select a district below.')
        );
        setLocationLoading(false);
        return;
      }
    }

    setUserLocation(coords);
    const nearest = findNearestMandi(coords, mandis);
    if (nearest) {
      setDistrict(nearest.district);
      setMandiId(nearest.id);
      setLocationSuccessMsg(
        language === 'hi'
          ? `📍 निकटतम उपार्जन केंद्र: ${nearest.hindiName || nearest.name} (${nearest.distanceFormatted} दूर, ट्रैक्टर से ~${nearest.travelEstimateMins} मिनट) चयनित किया गया है!`
          : `📍 Nearest Center: ${nearest.name} (${nearest.distanceFormatted} away, ~${nearest.travelEstimateMins} mins by tractor) auto-selected!`
      );
    }
    setLocationLoading(false);
  };

  // AI states
  const [aiSlotLoading, setAiSlotLoading] = useState(false);
  const [aiSlotSuggestion, setAiSlotSuggestion] = useState<AISlotSuggestion | null>(null);

  const [aiYieldLoading, setAiYieldLoading] = useState(false);
  const [aiYieldAnalysis, setAiYieldAnalysis] = useState<AIYieldAnalysis | null>(null);

  const [bookingLoading, setBookingLoading] = useState(false);

  // Profit Calculator State
  const selectedCrop = crops.find((c) => c.id === cropId) || crops[0];
  const [calcAcreage, setCalcAcreage] = useState(4.0);
  const [calcCostPerAcre, setCalcCostPerAcre] = useState(selectedCrop?.typicalCostPerAcre || 11500);
  const [calcYieldPerAcre, setCalcYieldPerAcre] = useState(selectedCrop?.averageYieldPerAcreQuintal || 18);

  // Find most recent active booking for current user
  const userBookings = activeBookings.filter(
    (b) => !currentUser || b.farmerPhone === currentUser.phone || b.farmerId === 'farmer-01'
  );
  const latestBooking = userBookings[0] || activeBookings[0];

  // AI Slot Optimizer Handler
  const handleGetAiSlotRecommendation = async () => {
    setAiSlotLoading(true);
    const chosenMandi = mandis.find((m) => m.id === mandiId);
    try {
      const suggestion = await aiApi.slotRecommendation({
        district,
        mandiName: chosenMandi?.name || 'Krishi Upaj Mandi Sehore',
        cropName: selectedCrop?.name || 'Wheat',
        estimatedYieldQuintals: Number(estimatedYield) || 50,
        harvestDate,
        vehicleType,
      });
      if (suggestion) {
        setAiSlotSuggestion(suggestion);
        if (suggestion.recommendedSlot) {
          setTimeSlot(suggestion.recommendedSlot);
        }
      }
    } catch (e) {
      console.warn('AI slot fetch error', e);
    } finally {
      setAiSlotLoading(false);
    }
  };

  // AI Yield Advisor Handler
  const handleRunAiYieldAdvisor = async () => {
    setAiYieldLoading(true);
    try {
      const analysis = await aiApi.yieldAdvisor({
        cropName: selectedCrop?.name || 'Wheat',
        acreage: calcAcreage,
        district,
        estimatedYield: calcAcreage * calcYieldPerAcre,
      });
      if (analysis) {
        setAiYieldAnalysis(analysis);
      }
    } catch (e) {
      console.warn('AI yield advisor error', e);
    } finally {
      setAiYieldLoading(false);
    }
  };

  // Submit Booking
  const handleBookingSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBookingLoading(true);

    const chosenMandi = mandis.find((m) => m.id === mandiId) || mandis[0];

    const newBooking = await onBookSlot({
      farmerId: currentUser ? 'farmer-01' : 'farmer-01',
      farmerName: currentUser?.name || 'Kisan Ramesh Patel',
      farmerPhone: currentUser?.phone || '9826014522',
      district,
      village: 'Bilkisganj, Sehore',
      mandiCenterId: chosenMandi.id,
      mandiCenterName: chosenMandi.name,
      cropId: selectedCrop.id,
      cropName: selectedCrop.name,
      estimatedYieldQuintals: Number(estimatedYield) || 50,
      acreage: Number(acreage) || 3,
      harvestDate,
      scheduledDate,
      timeSlot,
      vehicleType,
      vehicleNumber,
      aiRecommended: !!aiSlotSuggestion,
      aiReasoning: aiSlotSuggestion?.reasons?.join(' ') || 'Scheduled via KisanSarthi Smart Optimizer',
      waitTimeEstimateMins: aiSlotSuggestion?.estimatedWaitTimeMinutes || 20,
    });

    setBookingLoading(false);

    if (newBooking) {
      // Confetti celebration
      try {
        confetti({
          particleCount: 80,
          spread: 70,
          origin: { y: 0.6 },
        });
      } catch (err) {
        // Safe fallback
      }
      setActiveTab('TOKEN');
    }
  };

  // Profit calculations
  const totalCalcYield = calcAcreage * calcYieldPerAcre;
  const totalGrossMsp = totalCalcYield * (selectedCrop?.totalMsp || 2400);
  const totalInputCost = calcAcreage * calcCostPerAcre;
  const totalNetProfit = totalGrossMsp - totalInputCost;
  const profitMarginPct = totalGrossMsp > 0 ? (totalNetProfit / totalGrossMsp) * 100 : 0;

  // Stages for Progress Tracking
  const queueStages = [
    { key: 'BOOKED', labelEn: 'Slot Booked', labelHi: 'स्लॉट बुक' },
    { key: 'GATE_ENTERED', labelEn: 'Gate Entry', labelHi: 'गेट प्रवेश' },
    { key: 'WEIGHBRIDGE_GROSS', labelEn: 'Gross Weight', labelHi: 'भरा तौल' },
    { key: 'QC_INSPECTION', labelEn: 'Moisture QC', labelHi: 'नमी जांच' },
    { key: 'UNLOADING', labelEn: 'Unloading', labelHi: 'खाली करना' },
    { key: 'WEIGHBRIDGE_TARE', labelEn: 'Tare Weight', labelHi: 'खाली तौल' },
    { key: 'COMPLETED', labelEn: 'DBT Payment', labelHi: 'डीबीटी भुगतान' },
  ];

  const getStageIndex = (status: string) => {
    switch (status) {
      case 'BOOKED':
        return 0;
      case 'GATE_ENTERED':
        return 1;
      case 'WEIGHBRIDGE_GROSS':
        return 2;
      case 'QC_INSPECTION':
        return 3;
      case 'UNLOADING':
        return 4;
      case 'WEIGHBRIDGE_TARE':
        return 5;
      case 'COMPLETED':
        return 6;
      default:
        return 0;
    }
  };

  const currentStageIdx = latestBooking ? getStageIndex(latestBooking.status) : 0;

  return (
    <div className="space-y-6">
      {/* Mobile-Friendly Horizontal Navigation Bar */}
      <div className="bg-white rounded-2xl shadow-xs border border-slate-200 p-1.5 overflow-x-auto scrollbar-none flex items-center gap-1">
        <button
          id="farmer-tab-live-token"
          onClick={() => setActiveTab('TOKEN')}
          className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
            activeTab === 'TOKEN'
              ? 'bg-[#1B4332] text-white shadow-xs'
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
          }`}
        >
          <Clock className="w-3.5 h-3.5" />
          <span>{t.tabActiveToken}</span>
          {latestBooking && (
            <span className="ml-1 bg-[#D4E09B] text-[#1B4332] font-mono text-[10px] font-black px-1.5 py-0.2 rounded-full">
              {latestBooking.tokenNumber.split('-')[2] || '38'}
            </span>
          )}
        </button>

        <button
          id="farmer-tab-book-slot"
          onClick={() => setActiveTab('BOOK')}
          className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
            activeTab === 'BOOK'
              ? 'bg-[#1B4332] text-white shadow-xs'
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
          }`}
        >
          <Calendar className="w-3.5 h-3.5" />
          <span>{t.tabBookSlot}</span>
        </button>

        <button
          id="farmer-tab-profit-calc"
          onClick={() => setActiveTab('CALCULATOR')}
          className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
            activeTab === 'CALCULATOR'
              ? 'bg-[#1B4332] text-white shadow-xs'
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
          }`}
        >
          <Coins className="w-3.5 h-3.5" />
          <span>{t.tabProfitCalc}</span>
        </button>

        <button
          id="farmer-tab-mandi-queues"
          onClick={() => setActiveTab('MANDI_QUEUES')}
          className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
            activeTab === 'MANDI_QUEUES'
              ? 'bg-[#1B4332] text-white shadow-xs'
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
          }`}
        >
          <Layers className="w-3.5 h-3.5" />
          <span>{t.tabMandiQueues}</span>
        </button>

        <button
          id="farmer-tab-msp-rates"
          onClick={() => setActiveTab('MSP_RATES')}
          className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
            activeTab === 'MSP_RATES'
              ? 'bg-[#1B4332] text-white shadow-xs'
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
          }`}
        >
          <TrendingUp className="w-3.5 h-3.5" />
          <span>{t.tabMspRates}</span>
        </button>

        <button
          id="farmer-tab-weather"
          onClick={() => setActiveTab('WEATHER')}
          className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
            activeTab === 'WEATHER'
              ? 'bg-[#1B4332] text-white shadow-xs'
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
          }`}
        >
          <CloudSun className="w-3.5 h-3.5" />
          <span>{t.tabWeather}</span>
        </button>
      </div>

      {/* ========================================================= */}
      {/* TAB 1: ACTIVE LIVE TOKEN & QUEUE TRACKER */}
      {/* ========================================================= */}
      {activeTab === 'TOKEN' && (
        <div className="space-y-5">
          {latestBooking ? (
            <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
              {/* Token Card Header */}
              <div className="px-6 py-4 border-b border-slate-100 flex justify-between items-center bg-slate-50/60">
                <div className="flex items-center gap-2">
                  <h2 className="font-bold text-slate-800 text-sm sm:text-base">Active Procurement Slot</h2>
                  <span className="text-[10px] text-slate-400 font-mono hidden sm:inline">#{latestBooking.id.slice(-6)}</span>
                </div>
                <span className="px-3 py-1 bg-[#D4E09B]/40 text-[#1B4332] text-[10px] font-bold rounded-full uppercase border border-[#A3B18A]/40 tracking-wider">
                  {latestBooking.status === 'COMPLETED' ? 'Completed' : 'Confirmed & Active'}
                </span>
              </div>

              {/* Token Main Banner */}
              <div className="p-6 sm:p-8 bg-gradient-to-br from-[#1B4332] via-[#245740] to-[#2D6A4F] text-white relative overflow-hidden">
                <div className="flex flex-col md:flex-row items-center justify-between gap-6 relative z-10">
                  <div className="flex flex-col sm:flex-row items-center sm:items-start gap-5 text-center sm:text-left">
                    <div className="w-20 h-20 sm:w-24 sm:h-24 bg-[#D4E09B]/25 rounded-full flex flex-col items-center justify-center border-4 border-white/90 shadow-inner shrink-0">
                      <span className="text-[10px] uppercase font-bold text-emerald-100 tracking-wider">Token</span>
                      <span className="text-2xl sm:text-3xl font-black text-[#D4E09B] font-mono leading-none">
                        #{latestBooking.tokenNumber.split('-')[2] || '38'}
                      </span>
                    </div>
                    <div>
                      <div className="inline-flex items-center gap-1.5 bg-white/10 text-emerald-100 text-[11px] font-semibold px-2.5 py-0.5 rounded-full mb-1.5 backdrop-blur-xs">
                        <span className="w-2 h-2 rounded-full bg-[#D4E09B] animate-pulse"></span>
                        <span>{latestBooking.mandiCenterName}</span>
                      </div>
                      <h2 className="text-2xl sm:text-3xl font-black font-mono tracking-tight text-white">
                        {latestBooking.tokenNumber}
                      </h2>
                      <p className="text-xs text-emerald-100/90 mt-1">
                        Vehicle: <strong className="text-white font-mono">{latestBooking.vehicleNumber}</strong> • Window: <strong className="text-white">{latestBooking.timeSlot}</strong>
                      </p>
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    <button
                      onClick={() => onViewTokenPass(latestBooking)}
                      className="px-4 py-2.5 rounded-xl bg-white text-[#1B4332] hover:bg-[#F3F6F1] font-bold text-xs shadow-xs flex items-center gap-1.5 cursor-pointer transition-colors"
                    >
                      <QrCode className="w-4 h-4 text-[#2D6A4F]" />
                      <span>{t.printPass}</span>
                    </button>
                    {(latestBooking.status === 'COMPLETED' || latestBooking.netWeightQuintals) && (
                      <button
                        onClick={() => onViewJForm(latestBooking)}
                        className="px-4 py-2.5 rounded-xl bg-[#D4E09B] hover:bg-[#c6d488] text-[#1B4332] font-bold text-xs shadow-xs flex items-center gap-1.5 cursor-pointer transition-colors"
                      >
                        <FileText className="w-4 h-4" />
                        <span>{t.viewJForm}</span>
                      </button>
                    )}
                  </div>
                </div>

                {/* Queue Stats Cards inside header */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 mt-6 pt-5 border-t border-emerald-700/50">
                  <div className="bg-white/10 rounded-xl p-3 backdrop-blur-xs">
                    <p className="text-[10px] text-emerald-200/80 uppercase font-bold tracking-wider">
                      {t.nowServing}
                    </p>
                    <p className="text-lg font-black font-mono text-[#D4E09B]">
                      Token #38
                    </p>
                  </div>
                  <div className="bg-white/10 rounded-xl p-3 backdrop-blur-xs">
                    <p className="text-[10px] text-emerald-200/80 uppercase font-bold tracking-wider">
                      {t.tokensAhead}
                    </p>
                    <p className="text-lg font-black font-mono text-white">
                      {Math.max(0, latestBooking.tokenSequence - 38)} Trucks
                    </p>
                  </div>
                  <div className="bg-white/10 rounded-xl p-3 backdrop-blur-xs">
                    <p className="text-[10px] text-emerald-200/80 uppercase font-bold tracking-wider">
                      {t.estimatedWait}
                    </p>
                    <p className="text-lg font-black font-mono text-white">
                      {latestBooking.status === 'COMPLETED' ? '0 min' : `${latestBooking.waitTimeEstimateMins} mins`}
                    </p>
                  </div>
                  <div className="bg-white/10 rounded-xl p-3 backdrop-blur-xs">
                    <p className="text-[10px] text-emerald-200/80 uppercase font-bold tracking-wider">
                      {t.gateStatus}
                    </p>
                    <p className="text-lg font-bold text-[#D4E09B] flex items-center gap-1">
                      <CheckCircle2 className="w-4 h-4" />
                      <span>Open (Gate 2)</span>
                    </p>
                  </div>
                </div>
              </div>

              {/* Progress Stepper for Queue Workflow */}
              <div className="p-5 sm:p-6 bg-slate-50/70 border-b border-slate-200">
                <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-4 flex items-center justify-between">
                  <span>Mandi Transit & Processing Progression</span>
                  <span className="text-[11px] font-semibold text-[#2D6A4F]">
                    Step {currentStageIdx + 1} of {queueStages.length}
                  </span>
                </h3>

                {/* Stepper bar */}
                <div className="relative">
                  {/* Background line */}
                  <div className="absolute top-4 left-4 right-4 h-1 bg-slate-200 -z-0">
                    <div
                      className="h-full bg-[#2D6A4F] transition-all duration-500"
                      style={{
                        width: `${(currentStageIdx / (queueStages.length - 1)) * 100}%`,
                      }}
                    ></div>
                  </div>

                  <div className="grid grid-cols-7 relative z-10">
                    {queueStages.map((stage, idx) => {
                      const isDone = idx < currentStageIdx;
                      const isCurrent = idx === currentStageIdx;
                      return (
                        <div key={stage.key} className="text-center flex flex-col items-center">
                          <div
                            className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-all shadow-xs ${
                              isDone
                                ? 'bg-[#2D6A4F] text-white'
                                : isCurrent
                                ? 'bg-[#D4E09B] text-[#1B4332] ring-4 ring-[#D4E09B]/40 scale-110'
                                : 'bg-slate-200 text-slate-500'
                            }`}
                          >
                            {isDone ? <CheckCircle2 className="w-4 h-4" /> : idx + 1}
                          </div>
                          <p
                            className={`text-[10px] mt-1.5 font-semibold line-clamp-1 leading-tight ${
                              isCurrent ? 'text-slate-900 font-bold' : 'text-slate-500'
                            }`}
                          >
                            {language === 'hi' ? stage.labelHi : stage.labelEn}
                          </p>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>

              {/* Active Booking Summary Details */}
              <div className="p-6 grid grid-cols-1 md:grid-cols-3 gap-4 text-xs bg-white">
                <div className="p-4 bg-[#F3F6F1]/70 rounded-2xl border border-slate-200/80 space-y-1">
                  <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Commodity Details</p>
                  <p className="font-bold text-slate-900 text-sm">{latestBooking.cropName}</p>
                  <p className="text-slate-600">
                    Estimated Yield: <strong className="text-slate-900">{latestBooking.estimatedYieldQuintals} Quintals</strong>
                  </p>
                  <p className="text-[#2D6A4F] font-semibold">
                    MSP Rate: ₹2,400/Qtl (Incl. MP Bonus)
                  </p>
                </div>

                <div className="p-4 bg-[#F3F6F1]/70 rounded-2xl border border-slate-200/80 space-y-1">
                  <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Logistics & AI Route</p>
                  <p className="font-bold text-slate-900">{latestBooking.mandiCenterName}</p>
                  <p className="text-slate-600">
                    Date: <strong className="text-slate-900">{latestBooking.scheduledDate}</strong> ({latestBooking.timeSlot})
                  </p>
                  {latestBooking.aiRecommended && (
                    <span className="inline-flex items-center gap-1 text-[10px] font-bold text-[#1B4332] bg-[#D4E09B]/50 px-2 py-0.5 rounded-md border border-[#A3B18A]/40">
                      <Sparkles className="w-3 h-3 text-[#2D6A4F]" />
                      AI Optimized Low-Wait Slot
                    </span>
                  )}
                </div>

                <div className="p-4 bg-[#F3F6F1]/70 rounded-2xl border border-slate-200/80 space-y-1">
                  <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Direct Benefit Transfer (DBT)</p>
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-slate-700">Payment Status</span>
                    <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-[#D4E09B]/40 text-[#1B4332] border border-[#A3B18A]/40">
                      {latestBooking.paymentStatus.replace(/_/g, ' ')}
                    </span>
                  </div>
                  <p className="text-slate-600">
                    Expected Payout: <strong className="text-[#1B4332]">₹{(latestBooking.estimatedYieldQuintals * 2400).toLocaleString('en-IN')}</strong>
                  </p>
                  <p className="text-slate-400 font-mono text-[10px]">
                    Linked A/C: ****{latestBooking.bankAccountLast4 || '4589'}
                  </p>
                </div>
              </div>

              {/* Sleek Interface Bottom Action Bar */}
              <div className="p-4 bg-slate-900 text-white flex flex-wrap items-center justify-center gap-6 sm:gap-10 text-xs border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => alert(`SMS Alert Sent: Token ${latestBooking.tokenNumber} for ${latestBooking.cropName} dispatched to ${latestBooking.farmerPhone}`)}
                  className="text-xs font-bold uppercase tracking-wider flex items-center gap-2 text-slate-300 hover:text-[#D4E09B] transition-colors cursor-pointer"
                >
                  <Phone className="w-3.5 h-3.5 text-[#D4E09B]" />
                  <span>SEND TO SMS</span>
                </button>
                <div className="h-3 w-px bg-slate-700 hidden sm:block"></div>
                <div className="text-xs font-bold uppercase tracking-wider flex items-center gap-2 text-slate-300">
                  <CheckCircle2 className="w-3.5 h-3.5 text-[#D4E09B]" />
                  <span>OFFLINE SYNCED</span>
                </div>
              </div>
            </div>
          ) : (
            <div className="p-10 text-center bg-white rounded-3xl border border-slate-200 shadow-sm">
              <div className="w-16 h-16 bg-[#F3F6F1] rounded-2xl flex items-center justify-center mx-auto mb-3">
                <Tractor className="w-8 h-8 text-[#2D6A4F]" />
              </div>
              <h3 className="text-base font-bold text-slate-900">No Active Slot Booked Yet</h3>
              <p className="text-xs text-slate-500 max-w-md mx-auto mt-1">
                Book a time slot at your nearest APMC Mandi to get an instant digital token and avoid queue waiting.
              </p>
              <button
                onClick={() => setActiveTab('BOOK')}
                className="mt-4 px-5 py-2.5 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold text-xs uppercase tracking-wider rounded-xl shadow-xs cursor-pointer transition-colors"
              >
                {t.tabBookSlot}
              </button>
            </div>
          )}

          {/* Quick Actions & Tips */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-5 rounded-2xl bg-white border border-slate-200 shadow-sm">
              <div className="flex items-center gap-2 text-[#1B4332] font-bold text-xs mb-1.5">
                <AlertTriangle className="w-4 h-4 text-amber-600" />
                <span>Mandi Quality Inspection FAQ Norms</span>
              </div>
              <p className="text-[11px] text-slate-600 leading-relaxed">
                Ensure moisture is below <strong>12.0% for Wheat</strong> and <strong>10.0% for Soybean</strong>. Keep tarpaulin ready in case of unexpected rain to protect grain quality before weighbridge entry.
              </p>
            </div>

            <div className="p-5 rounded-2xl bg-[#2D6A4F] text-white shadow-sm">
              <div className="flex items-center justify-between gap-2 mb-1.5">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-[#D4E09B]" />
                  <span className="text-xs font-bold uppercase tracking-wider text-white/90">AI Slot Scheduling Benefit</span>
                </div>
                <span className="text-[10px] bg-white/15 text-[#D4E09B] font-mono px-2 py-0.5 rounded-full">KisanSarthi AI</span>
              </div>
              <p className="text-[11px] text-white/90 leading-relaxed">
                KisanSarthi AI dynamically predicts weighbridge queue clearance rates based on live tractor arrival trends, reducing average turnaround from <strong>4.5 hours down to 25 minutes</strong>.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* TAB 2: BOOK MANDI SLOT & AI OPTIMIZER */}
      {/* ========================================================= */}
      {activeTab === 'BOOK' && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8">
          <div className="border-b border-slate-100 pb-4 mb-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                  <Calendar className="w-5 h-5 text-[#2D6A4F]" />
                  <span>{t.tabBookSlot}</span>
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Book your procurement center visit to secure a verified token and bypass weighbridge congestion.
                </p>
              </div>
              <span className="hidden sm:inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider bg-[#D4E09B]/40 text-[#1B4332] border border-[#A3B18A]/40">
                <ShieldCheck className="w-3.5 h-3.5 text-[#2D6A4F]" />
                MP e-Uparjan Verified
              </span>
            </div>
          </div>

          <form onSubmit={handleBookingSubmit} className="space-y-6">
            {/* Location-Based Nearest Mandi Detection Banner */}
            <div className="bg-gradient-to-r from-[#D4E09B]/35 via-[#F3F6F1] to-emerald-50/50 p-4 rounded-2xl border border-[#A3B18A]/60 space-y-3">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-2xl bg-[#1B4332] text-white flex items-center justify-center shadow-xs shrink-0">
                    <Navigation className="w-5 h-5 text-[#D4E09B]" />
                  </div>
                  <div>
                    <h4 className="text-xs sm:text-sm font-bold text-slate-900 flex items-center gap-1.5">
                      <span>{language === 'hi' ? 'लाइव लोकेशन से निकटतम मंडी चुनें' : 'Choose Nearest Center Using Location Access'}</span>
                      <span className="px-1.5 py-0.2 text-[9px] font-mono font-bold bg-[#2D6A4F] text-[#D4E09B] rounded">
                        GPS
                      </span>
                    </h4>
                    <p className="text-[11px] text-slate-600">
                      {language === 'hi'
                        ? 'अपने वर्तमान स्थान (GPS) के आधार पर स्वतः सबसे नजदीकी उपार्जन केंद्र चुनें।'
                        : 'Detect device GPS location to auto-select the nearest APMC Mandi and estimate travel time.'}
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  id="detect-nearest-mandi-btn"
                  onClick={() => handleDetectLocation()}
                  disabled={locationLoading}
                  className="px-4 py-2.5 bg-[#1B4332] hover:bg-[#2D6A4F] disabled:opacity-50 text-white rounded-xl text-xs font-bold uppercase tracking-wider flex items-center justify-center gap-2 cursor-pointer shadow-sm transition-all shrink-0"
                >
                  {locationLoading ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin text-[#D4E09B]" />
                      <span>{language === 'hi' ? 'खोजा जा रहा है...' : 'Locating...'}</span>
                    </>
                  ) : (
                    <>
                      <LocateFixed className="w-4 h-4 text-[#D4E09B]" />
                      <span>{language === 'hi' ? '📍 मेरी लोकेशन का उपयोग करें' : '📍 Use My Location'}</span>
                    </>
                  )}
                </button>
              </div>

              {/* Success notification if detected */}
              {locationSuccessMsg && (
                <div className="p-3 bg-white/95 rounded-xl border border-emerald-300 text-xs text-[#1B4332] flex items-start gap-2 shadow-xs">
                  <CheckCircle2 className="w-4 h-4 text-[#2D6A4F] shrink-0 mt-0.5" />
                  <span className="font-semibold leading-relaxed">{locationSuccessMsg}</span>
                </div>
              )}

              {/* Error or manual presets */}
              {locationError && (
                <div className="p-3 bg-amber-50 rounded-xl border border-amber-200 text-xs text-amber-800 space-y-2">
                  <div className="flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
                    <span className="font-medium">{locationError}</span>
                  </div>
                  <div className="flex items-center gap-1.5 flex-wrap text-[11px]">
                    <span className="font-bold text-slate-700">
                      {language === 'hi' ? 'अथवा म.प्र. के जिले से टेस्ट करें:' : 'Or test with an MP District:'}
                    </span>
                    {MP_LOCATION_PRESETS.map((p) => (
                      <button
                        key={p.district}
                        type="button"
                        onClick={() => handleDetectLocation({ lat: p.lat, lng: p.lng, label: p.name })}
                        className="px-2.5 py-1 rounded-lg bg-white border border-slate-300 hover:border-[#2D6A4F] hover:bg-[#D4E09B]/20 font-semibold text-slate-800 cursor-pointer shadow-2xs"
                      >
                        {p.district}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Proximity sorted Quick Selector Pills if location is known */}
              {userLocation && mandisWithDistance.length > 0 && (
                <div className="pt-2 border-t border-slate-200/80">
                  <div className="flex items-center justify-between text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-2">
                    <span>{language === 'hi' ? 'निकटतम से दूरस्थ मंडियां (दूरी अनुसार):' : 'Mandis Ordered by Proximity to You:'}</span>
                    <span className="font-mono text-[#2D6A4F]">
                      GPS: {userLocation.lat.toFixed(4)}, {userLocation.lng.toFixed(4)}
                    </span>
                  </div>
                  <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
                    {mandisWithDistance.slice(0, 6).map((m, idx) => {
                      const isSelected = mandiId === m.id;
                      return (
                        <button
                          key={m.id}
                          type="button"
                          onClick={() => {
                            setDistrict(m.district);
                            setMandiId(m.id);
                          }}
                          className={`px-3 py-2 rounded-xl text-left border text-xs whitespace-nowrap transition-all cursor-pointer shrink-0 ${
                            isSelected
                              ? 'bg-[#1B4332] text-white border-[#1B4332] shadow-xs ring-1 ring-[#1B4332]'
                              : 'bg-white hover:bg-slate-50 text-slate-800 border-slate-200'
                          }`}
                        >
                          <div className="flex items-center gap-2">
                            <span
                              className={`w-4 h-4 rounded-full text-[10px] font-bold flex items-center justify-center ${
                                isSelected ? 'bg-[#D4E09B] text-[#1B4332]' : 'bg-slate-200 text-slate-700'
                              }`}
                            >
                              {idx + 1}
                            </span>
                            <span className="font-bold">{m.district}</span>
                            <span
                              className={`text-[10px] font-mono px-1.5 py-0.2 rounded font-semibold ${
                                isSelected
                                  ? 'bg-white/20 text-[#D4E09B]'
                                  : 'bg-[#D4E09B]/30 text-[#1B4332]'
                              }`}
                            >
                              {m.distanceFormatted}
                            </span>
                            <span className="text-[9px] opacity-75">~{m.travelEstimateMins}m</span>
                          </div>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* District */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                  {t.selectDistrict}
                </label>
                <select
                  id="booking-district-select"
                  value={district}
                  onChange={(e) => {
                    setDistrict(e.target.value);
                    const matching = mandis.filter(
                      (m) => m.district.toLowerCase() === e.target.value.toLowerCase()
                    );
                    if (matching[0]) setMandiId(matching[0].id);
                  }}
                  className="w-full py-2.5 px-3 bg-[#F3F6F1]/60 border border-slate-300 rounded-xl text-xs font-semibold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F] focus:border-[#2D6A4F]"
                >
                  <option value="Sehore">Sehore (सीहोर - Sharbati Wheat Hub)</option>
                  <option value="Harda">Harda (हरदा - Moong / Soybean)</option>
                  <option value="Ujjain">Ujjain (उज्जैन - Malwa APMC)</option>
                  <option value="Indore">Indore (इंदौर - Laxmibai Nagar)</option>
                  <option value="Vidisha">Vidisha (विदिशा)</option>
                  <option value="Bhopal">Bhopal (भोपाल - Karond Mandi)</option>
                  <option value="Dewas">Dewas (देवास)</option>
                  <option value="Mandsaur">Mandsaur (मंदसौर - Mustard/Garlic)</option>
                  <option value="Narmadapuram">Narmadapuram / Hoshangabad (नर्मदापुरम)</option>
                  <option value="Jabalpur">Jabalpur (जबलपुर)</option>
                  <option value="Morena">Morena (मुरैना)</option>
                </select>
              </div>

              {/* Mandi Center */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider flex items-center justify-between">
                  <span>{t.selectMandi}</span>
                  {userLocation && (
                    <span className="text-[10px] text-[#2D6A4F] font-semibold lowercase tracking-normal">
                      📍 distance calculated
                    </span>
                  )}
                </label>
                <select
                  id="booking-mandi-select"
                  value={mandiId}
                  onChange={(e) => setMandiId(e.target.value)}
                  className="w-full py-2.5 px-3 bg-[#F3F6F1]/60 border border-slate-300 rounded-xl text-xs font-semibold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F] focus:border-[#2D6A4F]"
                >
                  {districtMandis.length > 0 ? (
                    districtMandis.map((m) => {
                      const distObj = mandisWithDistance.find((mw) => mw.id === m.id);
                      return (
                        <option key={m.id} value={m.id}>
                          {m.name} ({m.weighbridgesCount} Weighbridges)
                          {distObj && distObj.distanceKm > 0 ? ` • ${distObj.distanceFormatted} away` : ''}
                        </option>
                      );
                    })
                  ) : (
                    <option value={mandis[0]?.id}>{mandis[0]?.name}</option>
                  )}
                </select>
              </div>

              {/* Crop Commodity */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                  {t.selectCrop}
                </label>
                <select
                  id="booking-crop-select"
                  value={cropId}
                  onChange={(e) => setCropId(e.target.value)}
                  className="w-full py-2.5 px-3 bg-[#F3F6F1]/60 border border-slate-300 rounded-xl text-xs font-semibold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F] focus:border-[#2D6A4F]"
                >
                  {crops.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} • MSP ₹{c.totalMsp}/qtl
                    </option>
                  ))}
                </select>
              </div>

              {/* Estimated Yield & Acreage */}
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                    {t.estimatedYield}
                  </label>
                  <input
                    id="booking-yield-input"
                    type="number"
                    value={estimatedYield}
                    onChange={(e) => setEstimatedYield(e.target.value)}
                    placeholder="65"
                    min="1"
                    max="1000"
                    className="w-full py-2.5 px-3 bg-[#F3F6F1]/60 border border-slate-300 rounded-xl text-xs font-semibold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F] focus:border-[#2D6A4F]"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                    {t.acreage}
                  </label>
                  <input
                    id="booking-acreage-input"
                    type="number"
                    step="0.5"
                    value={acreage}
                    onChange={(e) => setAcreage(e.target.value)}
                    placeholder="4.0"
                    className="w-full py-2.5 px-3 bg-[#F3F6F1]/60 border border-slate-300 rounded-xl text-xs font-semibold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F] focus:border-[#2D6A4F]"
                    required
                  />
                </div>
              </div>

              {/* Harvest Date & Scheduled Date */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                  {t.harvestDate}
                </label>
                <input
                  id="booking-harvest-date-input"
                  type="date"
                  value={harvestDate}
                  onChange={(e) => setHarvestDate(e.target.value)}
                  className="w-full py-2.5 px-3 bg-[#F3F6F1]/60 border border-slate-300 rounded-xl text-xs font-semibold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F] focus:border-[#2D6A4F]"
                  required
                />
              </div>

              {/* Vehicle Type & Number */}
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                    {t.vehicleType}
                  </label>
                  <select
                    id="booking-vehicle-type-select"
                    value={vehicleType}
                    onChange={(e) => setVehicleType(e.target.value as any)}
                    className="w-full py-2.5 px-3 bg-[#F3F6F1]/60 border border-slate-300 rounded-xl text-xs font-semibold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F] focus:border-[#2D6A4F]"
                  >
                    <option value="TRACTOR_TROLLEY">Tractor Trolley</option>
                    <option value="PICKUP_TRUCK">Pickup Truck</option>
                    <option value="TRUCK">Heavy Truck</option>
                    <option value="BULLOCK_CART">Bullock Cart</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                    {t.vehicleNumber}
                  </label>
                  <input
                    id="booking-vehicle-num-input"
                    type="text"
                    value={vehicleNumber}
                    onChange={(e) => setVehicleNumber(e.target.value.toUpperCase())}
                    placeholder="MP 37 AA 4821"
                    className="w-full py-2.5 px-3 bg-[#F3F6F1]/60 border border-slate-300 rounded-xl text-xs font-mono font-semibold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F] focus:border-[#2D6A4F]"
                    required
                  />
                </div>
              </div>
            </div>

            {/* AI Slot Recommendation Feature Card */}
            <div className="p-5 rounded-2xl bg-gradient-to-br from-[#1B4332] to-[#2D6A4F] text-white shadow-sm">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-3">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-white/15 text-[#D4E09B] flex items-center justify-center">
                    <Sparkles className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="text-xs font-bold uppercase tracking-wider text-white">
                      {t.aiSlotRecommendation}
                    </h4>
                    <p className="text-[11px] text-emerald-100/80 mt-0.5">
                      {t.aiSlotDesc}
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  id="run-ai-slot-btn"
                  onClick={handleGetAiSlotRecommendation}
                  disabled={aiSlotLoading}
                  className="px-4 py-2 bg-[#D4E09B] hover:bg-[#c6d488] disabled:opacity-60 text-[#1B4332] text-xs font-bold uppercase tracking-wider rounded-xl shadow-xs flex items-center justify-center gap-1.5 cursor-pointer transition-colors"
                >
                  {aiSlotLoading ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin text-[#1B4332]" />
                      <span>Analyzing MP Queue Data...</span>
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-3.5 h-3.5 text-[#1B4332]" />
                      <span>Get AI Recommended Slot</span>
                    </>
                  )}
                </button>
              </div>

              {aiSlotSuggestion && (
                <div className="p-4 bg-white text-slate-800 rounded-xl shadow-xs text-xs space-y-2 mt-3 animate-in fade-in">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-[#1B4332] flex items-center gap-1.5 text-xs sm:text-sm">
                      <CheckCircle2 className="w-4 h-4 text-[#2D6A4F]" />
                      Recommended Window: <strong>{aiSlotSuggestion.recommendedSlot}</strong>
                    </span>
                    <span className="px-2.5 py-0.5 rounded-full bg-[#D4E09B]/40 text-[#1B4332] border border-[#A3B18A]/40 font-bold text-[10px]">
                      Est. Wait: {aiSlotSuggestion.estimatedWaitTimeMinutes} Mins (Low Congestion)
                    </span>
                  </div>
                  <ul className="text-[11px] text-slate-600 space-y-1 list-disc list-inside">
                    {aiSlotSuggestion.reasons.map((r, i) => (
                      <li key={i}>{r}</li>
                    ))}
                  </ul>
                  {aiSlotSuggestion.weatherWarning && (
                    <p className="text-[10px] text-amber-800 bg-amber-50 p-2 rounded-lg border border-amber-200">
                      🌦️ {aiSlotSuggestion.weatherWarning}
                    </p>
                  )}
                </div>
              )}
            </div>

            {/* Time Slot Selection Options */}
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-2 uppercase tracking-wider">
                {t.selectSlot}
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2.5">
                {STANDARD_TIME_SLOTS.map((slot) => {
                  const isSelected = timeSlot === slot.timeSlot;
                  const isAiChoice = aiSlotSuggestion?.recommendedSlot === slot.timeSlot;
                  return (
                    <button
                      key={slot.timeSlot}
                      type="button"
                      onClick={() => setTimeSlot(slot.timeSlot)}
                      className={`p-3.5 rounded-2xl border text-left transition-all cursor-pointer relative ${
                        isSelected
                          ? 'border-[#2D6A4F] bg-[#D4E09B]/20 ring-2 ring-[#2D6A4F]/25 shadow-xs'
                          : 'border-slate-200 bg-[#F3F6F1]/50 hover:bg-white'
                      }`}
                    >
                      {isAiChoice && (
                        <span className="absolute -top-2 right-2 bg-[#2D6A4F] text-[#D4E09B] text-[9px] font-bold px-2 py-0.5 rounded-full shadow-xs flex items-center gap-0.5">
                          <Sparkles className="w-2.5 h-2.5" /> AI Pick
                        </span>
                      )}
                      <p className="text-xs font-bold text-slate-900">{slot.timeSlot}</p>
                      <div className="flex items-center justify-between text-[11px] mt-1.5 text-slate-500">
                        <span>Wait: ~{slot.estimatedWaitMins}m</span>
                        <span
                          className={`font-semibold ${
                            slot.status === 'OPEN'
                              ? 'text-[#2D6A4F]'
                              : slot.status === 'LIMITED'
                              ? 'text-amber-700'
                              : 'text-rose-700'
                          }`}
                        >
                          {slot.status}
                        </span>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Summary & Submit Button */}
            <div className="pt-4 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-3">
              <div className="text-xs text-slate-600">
                Guaranteed MSP: <strong className="text-slate-900">₹{selectedCrop.totalMsp}/qtl</strong> • Estimated Payout:{' '}
                <strong className="text-[#1B4332] font-bold">
                  ₹{(Number(estimatedYield || 0) * selectedCrop.totalMsp).toLocaleString('en-IN')}
                </strong>
              </div>

              <button
                id="confirm-booking-btn"
                type="submit"
                disabled={bookingLoading}
                className="w-full sm:w-auto px-7 py-3 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold text-xs uppercase tracking-widest rounded-xl shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer"
              >
                {bookingLoading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Generating e-Token & Slot...</span>
                  </>
                ) : (
                  <>
                    <QrCode className="w-4 h-4 text-[#D4E09B]" />
                    <span>{t.bookSlotBtn}</span>
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ========================================================= */}
      {/* TAB 3: CROP YIELD & PROFIT MARGIN CALCULATOR */}
      {/* ========================================================= */}
      {activeTab === 'CALCULATOR' && (
        <div className="space-y-5">
          <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8">
            <div className="border-b border-slate-100 pb-4 mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                  <Coins className="w-5 h-5 text-[#2D6A4F]" />
                  <span>{t.calculatorTitle}</span>
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  {t.calculatorSubtitle}
                </p>
              </div>

              <button
                type="button"
                id="run-ai-yield-btn"
                onClick={handleRunAiYieldAdvisor}
                disabled={aiYieldLoading}
                className="px-4 py-2.5 bg-[#1B4332] hover:bg-[#2D6A4F] disabled:opacity-60 text-white text-xs font-bold uppercase tracking-wider rounded-xl shadow-xs flex items-center gap-1.5 cursor-pointer transition-colors"
              >
                {aiYieldLoading ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin text-[#D4E09B]" />
                    <span>Analyzing Soil & Yield...</span>
                  </>
                ) : (
                  <>
                    <Sparkles className="w-3.5 h-3.5 text-[#D4E09B]" />
                    <span>{t.aiYieldAdvisorBtn}</span>
                  </>
                )}
              </button>
            </div>

            {/* Calculator Controls */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                  Crop Commodity
                </label>
                <select
                  value={cropId}
                  onChange={(e) => {
                    setCropId(e.target.value);
                    const c = crops.find((cr) => cr.id === e.target.value);
                    if (c) {
                      setCalcCostPerAcre(c.typicalCostPerAcre);
                      setCalcYieldPerAcre(c.averageYieldPerAcreQuintal);
                    }
                  }}
                  className="w-full py-2.5 px-3 bg-[#F3F6F1]/60 border border-slate-300 rounded-xl text-xs font-semibold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                >
                  {crops.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} (MSP ₹{c.totalMsp}/qtl)
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                  Land Size: <strong className="text-[#1B4332]">{calcAcreage} Acres</strong>
                </label>
                <input
                  type="range"
                  min="1"
                  max="25"
                  step="0.5"
                  value={calcAcreage}
                  onChange={(e) => setCalcAcreage(Number(e.target.value))}
                  className="w-full accent-[#2D6A4F] cursor-pointer"
                />
                <div className="flex justify-between text-[10px] text-slate-400 font-medium">
                  <span>1 Acre</span>
                  <span>12 Acres</span>
                  <span>25 Acres</span>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                  Expected Yield: <strong className="text-[#1B4332]">{calcYieldPerAcre} Qtl/Acre</strong>
                </label>
                <input
                  type="range"
                  min="4"
                  max="35"
                  value={calcYieldPerAcre}
                  onChange={(e) => setCalcYieldPerAcre(Number(e.target.value))}
                  className="w-full accent-[#2D6A4F] cursor-pointer"
                />
                <div className="flex justify-between text-[10px] text-slate-400 font-medium">
                  <span>4 Qtl</span>
                  <span>18 Qtl</span>
                  <span>35 Qtl</span>
                </div>
              </div>
            </div>

            {/* Calculations Result Cards */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3.5 mt-6 pt-6 border-t border-slate-200">
              <div className="p-4 bg-[#F3F6F1]/70 rounded-2xl border border-slate-200/80">
                <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Total Harvest</p>
                <p className="text-xl font-black text-slate-900 mt-1 font-mono">
                  {totalCalcYield.toFixed(1)} <span className="text-xs font-sans text-slate-500">Quintals</span>
                </p>
                <p className="text-[10px] text-slate-500 mt-0.5">Across {calcAcreage} acres</p>
              </div>

              <div className="p-4 bg-[#F3F6F1]/70 rounded-2xl border border-slate-200/80">
                <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">{t.totalInputCost}</p>
                <p className="text-xl font-black text-rose-700 mt-1 font-mono">
                  ₹{totalInputCost.toLocaleString('en-IN')}
                </p>
                <p className="text-[10px] text-slate-500 mt-0.5">₹{calcCostPerAcre}/acre input</p>
              </div>

              <div className="p-4 bg-[#F3F6F1]/70 rounded-2xl border border-slate-200/80">
                <p className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">{t.grossRevenueMsp}</p>
                <p className="text-xl font-black text-[#1B4332] mt-1 font-mono">
                  ₹{totalGrossMsp.toLocaleString('en-IN')}
                </p>
                <p className="text-[10px] text-slate-500 mt-0.5">At ₹{selectedCrop.totalMsp}/qtl MSP</p>
              </div>

              <div className="p-4 bg-gradient-to-br from-[#1B4332] to-[#2D6A4F] text-white rounded-2xl shadow-sm">
                <p className="text-[10px] text-emerald-200/90 uppercase font-bold tracking-wider">{t.projectedNetProfit}</p>
                <p className="text-xl font-black text-[#D4E09B] mt-1 font-mono">
                  ₹{totalNetProfit.toLocaleString('en-IN')}
                </p>
                <p className="text-[10px] text-emerald-200/80 font-bold mt-0.5">
                  Margin: {profitMarginPct.toFixed(1)}%
                </p>
              </div>
            </div>

            {/* AI Yield Analysis Output Card */}
            {aiYieldAnalysis && (
              <div className="mt-6 p-5 rounded-2xl bg-[#F3F6F1] border border-slate-200 space-y-3 animate-in fade-in">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-[#1B4332] text-xs flex items-center gap-1.5">
                    <Sparkles className="w-4 h-4 text-[#2D6A4F]" />
                    AI Soil Health & Yield Recommendations ({district}, MP)
                  </span>
                  <span className="px-2.5 py-0.5 rounded-full bg-[#D4E09B]/40 text-[#1B4332] border border-[#A3B18A]/40 font-bold text-[10px]">
                    Projected Margin: {aiYieldAnalysis.profitMarginPercent.toFixed(1)}%
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                  <div className="bg-white p-4 rounded-xl border border-slate-200">
                    <p className="font-bold text-slate-800 text-[11px] mb-1.5">Key Yield Maximization Steps</p>
                    <ul className="list-disc list-inside space-y-1 text-slate-600 text-[11px]">
                      {aiYieldAnalysis.keyRecommendations.map((r, i) => (
                        <li key={i}>{r}</li>
                      ))}
                    </ul>
                  </div>

                  <div className="bg-white p-4 rounded-xl border border-slate-200">
                    <p className="font-bold text-slate-800 text-[11px] mb-1.5">Soil Health & Harvest Window</p>
                    <p className="text-slate-600 text-[11px] leading-relaxed mb-1.5">
                      {aiYieldAnalysis.harvestWindowAdvice}
                    </p>
                    <ul className="list-disc list-inside space-y-0.5 text-slate-500 text-[10px]">
                      {aiYieldAnalysis.soilHealthTips.map((tip, i) => (
                        <li key={i}>{tip}</li>
                      ))}
                    </ul>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* TAB 4: LIVE MANDI QUEUE MONITOR & CENTRES */}
      {/* ========================================================= */}
      {activeTab === 'MANDI_QUEUES' && (
        <div className="space-y-4">
          <div className="bg-white p-4 rounded-2xl border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-xs">
            <div>
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Layers className="w-5 h-5 text-[#2D6A4F]" />
                <span>Madhya Pradesh APMC Mandi Queue Status</span>
              </h2>
              <p className="text-xs text-slate-500">
                Real-time weighbridge throughput, wait queues, and distance from your location.
              </p>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => handleDetectLocation()}
                disabled={locationLoading}
                className="px-3.5 py-1.5 rounded-xl bg-[#1B4332] hover:bg-[#2D6A4F] text-white text-xs font-bold flex items-center gap-1.5 shadow-2xs cursor-pointer transition-all shrink-0"
              >
                {locationLoading ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin text-[#D4E09B]" />
                ) : (
                  <LocateFixed className="w-3.5 h-3.5 text-[#D4E09B]" />
                )}
                <span>
                  {userLocation
                    ? language === 'hi'
                      ? 'लोकेशन रिफ्रेश करें'
                      : 'Refresh My Location'
                    : language === 'hi'
                    ? '📍 मेरी लोकेशन से दूरी देखें'
                    : '📍 Sort by Distance to Me'}
                </span>
              </button>

              <span className="text-xs font-mono text-[#1B4332] bg-[#D4E09B]/35 border border-[#A3B18A]/35 px-2.5 py-1 rounded-xl font-bold">
                {mandis.length} Active Centers
              </span>
            </div>
          </div>

          {userLocation && (
            <div className="p-3 bg-emerald-50/80 rounded-xl border border-emerald-200 text-xs text-[#1B4332] flex items-center justify-between">
              <span className="flex items-center gap-1.5 font-semibold">
                <Navigation className="w-4 h-4 text-[#2D6A4F]" />
                <span>
                  {language === 'hi'
                    ? 'सभी मंडियां आपकी लोकेशन से दूरी के क्रम में व्यवस्थित हैं'
                    : 'Mandis sorted by proximity to your current location (nearest first)'}
                </span>
              </span>
              <span className="font-mono text-[11px] bg-white px-2 py-0.5 rounded border border-emerald-200 text-[#1B4332] font-bold">
                GPS: {userLocation.lat.toFixed(4)}, {userLocation.lng.toFixed(4)}
              </span>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {(userLocation ? mandisWithDistance : mandis).map((mandi, idx) => {
              const withDist = mandi as MandiWithDistance;
              const hasDist = withDist.distanceKm && withDist.distanceKm > 0;
              const isNearest = userLocation && idx === 0;

              return (
                <div
                  key={mandi.id}
                  className={`bg-white rounded-2xl border p-5 space-y-3.5 transition-all shadow-xs ${
                    isNearest
                      ? 'border-[#2D6A4F] ring-1 ring-[#2D6A4F]/30 bg-gradient-to-b from-emerald-50/20 to-white'
                      : 'border-slate-200 hover:border-[#2D6A4F]'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="font-bold text-sm text-slate-900 leading-snug">
                          {mandi.name}
                        </h3>
                        {isNearest && (
                          <span className="px-2 py-0.2 rounded-full text-[9px] font-bold bg-[#2D6A4F] text-[#D4E09B] uppercase tracking-wider">
                            Nearest
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-slate-500 flex items-center gap-1 mt-0.5">
                        <MapPin className="w-3.5 h-3.5 text-slate-400" />
                        <span>{mandi.district}, MP • {mandi.pinCode}</span>
                      </p>
                    </div>
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                        mandi.gateStatus === 'OPEN'
                          ? 'bg-[#D4E09B]/40 text-[#1B4332] border border-[#A3B18A]/40'
                          : 'bg-amber-100 text-amber-800'
                      }`}
                    >
                      Gate {mandi.gateStatus}
                    </span>
                  </div>

                  {/* Proximity & Estimated Travel Time Badge */}
                  {hasDist && (
                    <div className="flex items-center justify-between text-xs px-3 py-1.5 rounded-xl bg-emerald-50/70 border border-emerald-100 text-[#1B4332]">
                      <span className="flex items-center gap-1 font-semibold text-[11px]">
                        <Navigation className="w-3.5 h-3.5 text-[#2D6A4F]" />
                        <span>Distance: <strong>{withDist.distanceFormatted}</strong></span>
                      </span>
                      <span className="text-[11px] text-slate-600">
                        ~<strong>{withDist.travelEstimateMins} mins</strong> by tractor
                      </span>
                    </div>
                  )}

                  <div className="grid grid-cols-3 gap-2 bg-[#F3F6F1]/70 p-3 rounded-xl text-center text-xs">
                    <div>
                      <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Now Serving</p>
                      <p className="font-mono font-bold text-[#1B4332] text-sm mt-0.5">
                        Token #{mandi.currentTokenServing}
                      </p>
                    </div>
                    <div>
                      <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Queue Waiting</p>
                      <p className="font-mono font-bold text-slate-900 text-sm mt-0.5">
                        {mandi.activeTokensWaiting} Trucks
                      </p>
                    </div>
                    <div>
                      <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Weighbridges</p>
                      <p className="font-mono font-bold text-slate-900 text-sm mt-0.5">
                        {mandi.weighbridgesCount} Active
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center justify-between text-xs pt-2 border-t border-slate-100">
                    <span className="text-slate-500 text-[11px]">
                      Avg Turnaround: <strong className="text-slate-800">{mandi.averageProcessingMins} mins</strong>
                    </span>
                    <button
                      onClick={() => {
                        setDistrict(mandi.district);
                        setMandiId(mandi.id);
                        setActiveTab('BOOK');
                      }}
                      className="px-3 py-1.5 rounded-lg bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold text-xs flex items-center gap-1 cursor-pointer transition-colors shadow-2xs"
                    >
                      <span>{language === 'hi' ? 'यहाँ स्लॉट बुक करें' : 'Select & Book Slot'}</span>
                      <ArrowUpRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* TAB 5: TODAY'S MSP RATES & COMMODITIES */}
      {/* ========================================================= */}
      {activeTab === 'MSP_RATES' && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8">
          <div className="border-b border-slate-100 pb-4 mb-6">
            <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
              <TrendingUp className="w-5 h-5 text-[#2D6A4F]" />
              <span>Madhya Pradesh Official Minimum Support Prices (MSP)</span>
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Current government guaranteed procurement prices with MP State Bonus additions.
            </p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-[#F3F6F1] text-[10px] uppercase font-bold text-slate-500 tracking-wider">
                <tr>
                  <th className="p-3.5 rounded-l-xl">Commodity / Crop</th>
                  <th className="p-3.5">Season</th>
                  <th className="p-3.5">Central MSP</th>
                  <th className="p-3.5">MP State Bonus</th>
                  <th className="p-3.5 font-bold text-[#1B4332]">Total Mandi MSP</th>
                  <th className="p-3.5">Max Moisture Limit</th>
                  <th className="p-3.5 rounded-r-xl">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {crops.map((crop) => (
                  <tr key={crop.id} className="hover:bg-[#F3F6F1]/50 transition-colors">
                    <td className="p-3.5">
                      <div className="font-bold text-slate-900 flex items-center gap-2">
                        <span className="text-base">{crop.icon}</span>
                        <div>
                          <span>{crop.name}</span>
                          <p className="text-[10px] text-slate-400 font-normal">{crop.hindiName}</p>
                        </div>
                      </div>
                    </td>
                    <td className="p-3.5">
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 text-slate-700">
                        {crop.season}
                      </span>
                    </td>
                    <td className="p-3.5 font-mono">₹{crop.standardMspPerQuintal.toLocaleString()}/qtl</td>
                    <td className="p-3.5 font-mono text-[#2D6A4F] font-bold">
                      {crop.mpBonusPerQuintal > 0 ? `+₹${crop.mpBonusPerQuintal}/qtl` : '—'}
                    </td>
                    <td className="p-3.5 font-mono font-black text-sm text-[#1B4332]">
                      ₹{crop.totalMsp.toLocaleString()}/qtl
                    </td>
                    <td className="p-3.5 text-slate-600 font-medium">≤ {crop.moistureLimitPct}%</td>
                    <td className="p-3.5">
                      <button
                        onClick={() => {
                          setCropId(crop.id);
                          setActiveTab('BOOK');
                        }}
                        className="px-3 py-1.5 bg-[#1B4332] hover:bg-[#2D6A4F] text-white rounded-xl text-[11px] font-bold uppercase tracking-wider cursor-pointer transition-colors"
                      >
                        Book Slot
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
      {/* TAB 6: WEATHER & HARVEST ADVISORY */}
      {/* ========================================================= */}
      {activeTab === 'WEATHER' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <CloudSun className="w-5 h-5 text-[#2D6A4F]" />
                <span>Agricultural Weather & Transit Advisories</span>
              </h2>
              <p className="text-xs text-slate-500">
                Weather forecasts mapped to APMC mandi transit routes to avoid rain damage and moisture penalties.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {weatherAlerts.map((w) => {
              const isWarning = w.severity === 'WARNING';
              return (
                <div
                  key={w.id}
                  className={`rounded-2xl p-5 space-y-3.5 transition-shadow shadow-xs ${
                    isWarning
                      ? 'bg-[#2D6A4F] text-white'
                      : 'bg-white border border-slate-200 text-slate-800'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-sm">{w.district} District</span>
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                        isWarning
                          ? 'bg-[#D4E09B] text-[#1B4332]'
                          : 'bg-[#D4E09B]/40 text-[#1B4332] border border-[#A3B18A]/40'
                      }`}
                    >
                      {w.severity}
                    </span>
                  </div>

                  <div className="flex items-baseline gap-2">
                    <span className="text-2xl font-black font-mono">{w.tempCelsius}°C</span>
                    <span className={`text-xs ${isWarning ? 'text-white/70' : 'text-slate-500'}`}>
                      Rain Chance: {w.rainfallChancePct}%
                    </span>
                  </div>

                  <p className={`text-xs font-semibold ${isWarning ? 'text-white/90' : 'text-slate-800'}`}>
                    {w.condition}
                  </p>

                  <div
                    className={`p-3 rounded-xl text-[11px] ${
                      isWarning
                        ? 'bg-white/10 text-white/90 border border-white/10'
                        : 'bg-[#F3F6F1] text-slate-700 border border-slate-200/80'
                    }`}
                  >
                    <p className={`font-bold mb-0.5 ${isWarning ? 'text-[#D4E09B]' : 'text-slate-900'}`}>
                      🌾 Harvest & Transit Tip:
                    </p>
                    <p>{w.advisory}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
