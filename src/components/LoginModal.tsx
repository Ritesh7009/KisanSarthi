import React, { useState } from 'react';
import {
  Tractor,
  Shield,
  Phone,
  CheckCircle2,
  ArrowRight,
  Sparkles,
  Building2,
  AlertCircle,
  X,
  Lock,
  Send,
  RotateCcw,
} from 'lucide-react';
import { Language, UserRole } from '../types';
import { translations } from '../i18n/translations';
import { DEMO_FARMERS, ALL_INDIA_MANDIS, INDIAN_STATES } from '../data/mpMandiData';
import { apiUrl, setAuthToken } from '../services/api';

interface Props {
  isOpen: boolean;
  onClose?: () => void;
  language: Language;
  onLoginSuccess: (user: {
    name: string;
    phone: string;
    aadharNumber?: string;
    maskedAadhar?: string;
    district: string;
    village?: string;
    role: UserRole;
    mandiId?: string;
  }) => void;
}

export const LoginModal: React.FC<Props> = ({ isOpen, onClose, language, onLoginSuccess }) => {
  const t = translations[language];
  const [activeTab, setActiveTab] = useState<UserRole>('FARMER');

  // Farmer form state (Mobile number only)
  const [phone, setPhone] = useState('9826014522');
  const [otpSent, setOtpSent] = useState(false);
  const [otp, setOtp] = useState('');
  const [farmerError, setFarmerError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Admin form state
  const [adminStateFilter, setAdminStateFilter] = useState('ALL');
  const [officerId, setOfficerId] = useState('IND-APMC-ADMIN-01');
  const [selectedMandiId, setSelectedMandiId] = useState('mandi-sehore');
  const [adminPasscode, setAdminPasscode] = useState('Admin@India2026');
  const [adminError, setAdminError] = useState('');

  if (!isOpen) return null;

  const cleanPhone = phone.replace(/\D/g, '');

  const availableMandis = adminStateFilter === 'ALL'
    ? ALL_INDIA_MANDIS
    : ALL_INDIA_MANDIS.filter((m) => {
        const stateObj = INDIAN_STATES.find((s) => s.code === adminStateFilter);
        return stateObj && m.state === stateObj.name;
      });

  const handleFastFarmerLoginWithPhone = (mobile: string) => {
    const validClean = mobile.replace(/\D/g, '').slice(0, 10);
    if (validClean.length < 10) {
      setFarmerError(
        language === 'hi' ? 'कृपया मान्य 10-अंकीय मोबाइल नंबर दर्ज करें' : 'Please enter valid 10-digit mobile number'
      );
      return;
    }

    setFarmerError('');
    const matched = DEMO_FARMERS.find((f) => f.phone === validClean);
    const last4 = validClean.slice(-4);
    const farmerData = matched
      ? {
          name: matched.name,
          phone: matched.phone,
          aadharNumber: `71048821${matched.phone.slice(-4)}`,
          maskedAadhar: `XXXX-XXXX-${matched.phone.slice(-4)}`,
          district: matched.district,
          village: matched.village,
          role: 'FARMER' as UserRole,
        }
      : {
          name: `Kisan (+91 ${validClean})`,
          phone: validClean,
          aadharNumber: `71048821${last4}`,
          maskedAadhar: `XXXX-XXXX-${last4}`,
          district: 'Ludhiana',
          village: 'Gram Panchayat',
          role: 'FARMER' as UserRole,
        };

    const token = `ks-token-${validClean}-${Date.now()}`;
    setAuthToken(token);
    onLoginSuccess(farmerData);

    fetch(apiUrl('/api/v1/auth/verify-otp'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: validClean, otp: '4826' }),
    }).catch(() => {});
  };

  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (cleanPhone.length < 10) {
      setFarmerError(language === 'hi' ? 'कृपया मान्य 10-अंकीय मोबाइल नंबर दर्ज करें' : 'Please enter valid 10-digit mobile number');
      return;
    }

    setFarmerError('');
    setOtpSent(true);
    setOtp('4826');

    fetch(apiUrl('/api/v1/auth/send-otp'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: cleanPhone }),
    }).catch(() => {});
  };

  const quickFarmerLogin = (farmer: typeof DEMO_FARMERS[0]) => {
    setFarmerError('');
    const token = `ks-token-${farmer.id}-${Date.now()}`;
    setAuthToken(token);
    onLoginSuccess({
      name: farmer.name,
      phone: farmer.phone,
      aadharNumber: `71048821${farmer.phone.slice(-4)}`,
      maskedAadhar: `XXXX-XXXX-${farmer.phone.slice(-4)}`,
      district: farmer.district,
      village: farmer.village,
      role: 'FARMER',
    });
  };

  const quickAdminLogin = (targetMandiId?: string) => {
    setAdminError('');
    const mandiIdToUse = targetMandiId || selectedMandiId || 'mandi-sehore';
    const mandi = ALL_INDIA_MANDIS.find((m) => m.id === mandiIdToUse) || ALL_INDIA_MANDIS[0];
    const token = `ks-adm-${Date.now()}`;
    setAuthToken(token);
    onLoginSuccess({
      id: `admin-${officerId || 'OFFICER-01'}`,
      name: `Officer (${mandi.name.split(' ')[0]} APMC)`,
      phone: mandi.phone || '1800-180-1551',
      district: mandi.district,
      role: 'ADMIN',
      mandiId: mandi.id,
    });
  };

  const handleVerifyFarmerOtp = (e: React.FormEvent) => {
    e.preventDefault();
    if (otp.trim().length < 4) {
      setFarmerError(language === 'hi' ? 'अमान्य सत्यापन कोड!' : 'Please enter valid verification code received via SMS');
      return;
    }

    handleFastFarmerLoginWithPhone(cleanPhone);
  };

  const handleAdminLogin = (e: React.FormEvent) => {
    e.preventDefault();
    setAdminError('');

    const validPasscodes = [
      'Admin@India2026',
      'Admin@ENAM2026',
      'Admin@MPMandi2026',
      'Admin@2026',
      'admin',
      'admin123',
    ];

    if (!validPasscodes.includes(adminPasscode.trim())) {
      setAdminError('Invalid passcode! Use Admin@India2026 or Admin@MPMandi2026');
      return;
    }

    quickAdminLogin(selectedMandiId);

    fetch(apiUrl('/api/v1/auth/admin-login'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: officerId,
        password: adminPasscode,
        mandiId: selectedMandiId,
      }),
    }).catch(() => {});
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
      <div className="bg-white rounded-3xl shadow-2xl max-w-md w-full overflow-hidden border border-slate-200 animate-in fade-in zoom-in-95 duration-200">
        {/* Header Branding */}
        <div className="bg-[#1B4332] text-white p-6 relative">
          {onClose && (
            <button
              onClick={onClose}
              className="absolute top-4 right-4 w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          )}

          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#D4E09B] text-[#1B4332] flex items-center justify-center shadow-xs">
              <Tractor className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white tracking-tight">KisanSarthi India • किसान सारथी</h2>
              <p className="text-xs text-[#D4E09B]/90">
                National e-NAM & State APMC Mandi Network
              </p>
            </div>
          </div>

          {/* Role Tabs */}
          <div className="grid grid-cols-2 gap-2 mt-5 p-1 bg-white/10 rounded-2xl backdrop-blur-xs">
            <button
              type="button"
              onClick={() => {
                setActiveTab('FARMER');
                setFarmerError('');
              }}
              className={`py-2 text-xs font-bold uppercase tracking-wider rounded-xl flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                activeTab === 'FARMER'
                  ? 'bg-white text-[#1B4332] shadow-xs'
                  : 'text-white/80 hover:text-white'
              }`}
            >
              <Tractor className="w-3.5 h-3.5" />
              <span>{language === 'hi' ? 'किसान (मोबाइल)' : 'Farmer (Mobile)'}</span>
            </button>
            <button
              type="button"
              onClick={() => {
                setActiveTab('ADMIN');
                setAdminError('');
              }}
              className={`py-2 text-xs font-bold uppercase tracking-wider rounded-xl flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                activeTab === 'ADMIN'
                  ? 'bg-white text-[#1B4332] shadow-xs'
                  : 'text-white/80 hover:text-white'
              }`}
            >
              <Shield className="w-3.5 h-3.5" />
              <span>{language === 'hi' ? 'विभागीय प्रशासक' : 'Admin'}</span>
            </button>
          </div>
        </div>

        {/* Modal Form Body */}
        <div className="p-6">
          {activeTab === 'FARMER' ? (
            <div>
              {farmerError && (
                <div className="mb-4 p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{farmerError}</span>
                </div>
              )}

              {!otpSent ? (
                <form onSubmit={handleSendOtp} className="space-y-4">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                      {language === 'hi' ? 'किसान का मोबाइल नंबर' : 'Farmer Mobile Number'}
                    </label>
                    <div className="relative">
                      <span className="absolute left-3.5 top-3.5 text-xs font-bold text-slate-500">+91</span>
                      <input
                        type="tel"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value.replace(/\D/g, '').slice(0, 10))}
                        placeholder="9826014522"
                        maxLength={10}
                        className="w-full pl-12 pr-4 py-3 bg-[#F3F6F1] border border-slate-300 rounded-xl text-base font-bold text-slate-900 font-mono focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                        required
                        autoFocus
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <button
                      type="button"
                      onClick={() => handleFastFarmerLoginWithPhone(cleanPhone)}
                      disabled={cleanPhone.length < 10}
                      className="w-full py-3 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold uppercase tracking-wider rounded-xl text-xs flex items-center justify-center gap-2 cursor-pointer transition-colors shadow-sm disabled:opacity-50"
                    >
                      <Sparkles className="w-4 h-4 text-[#D4E09B]" />
                      <span>⚡ {language === 'hi' ? 'त्वरित प्रवेश (0 Delay)' : 'Instant Fast Login (0 Delay)'}</span>
                      <ArrowRight className="w-4 h-4 text-[#D4E09B]" />
                    </button>

                    <button
                      type="submit"
                      disabled={cleanPhone.length < 10}
                      className="w-full py-2.5 bg-[#F3F6F1] hover:bg-slate-200 border border-slate-300 text-slate-700 font-bold uppercase tracking-wider rounded-xl text-xs flex items-center justify-center gap-2 cursor-pointer transition-colors disabled:opacity-50"
                    >
                      <Send className="w-3.5 h-3.5 text-[#2D6A4F]" />
                      <span>Get SMS OTP</span>
                    </button>
                  </div>
                </form>
              ) : (
                <form onSubmit={handleVerifyFarmerOtp} className="space-y-4">
                  <div className="p-3 bg-[#F3F6F1] rounded-xl text-xs text-[#1B4332] flex items-center justify-between">
                    <div>
                      <p className="font-bold">SMS OTP sent to +91 {cleanPhone}</p>
                      <p className="text-[11px] text-[#2D6A4F]">Demo Code: <strong className="font-mono text-sm text-[#1B4332]">4826</strong></p>
                    </div>
                    <CheckCircle2 className="w-5 h-5 text-[#2D6A4F]" />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1 uppercase tracking-wider text-center">
                      Enter 4-Digit OTP
                    </label>
                    <input
                      type="text"
                      value={otp}
                      onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 4))}
                      placeholder="4826"
                      maxLength={4}
                      className="w-full py-2.5 text-center tracking-[0.5em] font-mono text-xl font-bold bg-[#F3F6F1] border border-slate-300 rounded-xl"
                      autoFocus
                    />
                  </div>

                  <button
                    type="submit"
                    className="w-full py-3 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold uppercase tracking-wider rounded-xl text-xs flex items-center justify-center gap-2 cursor-pointer shadow-sm"
                  >
                    <CheckCircle2 className="w-4 h-4 text-[#D4E09B]" />
                    <span>Verify & Enter Portal</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setOtpSent(false)}
                    className="w-full text-center text-xs text-slate-500 hover:text-slate-800 font-semibold cursor-pointer py-1 flex items-center justify-center gap-1"
                  >
                    <RotateCcw className="w-3 h-3" />
                    <span>Change Mobile</span>
                  </button>
                </form>
              )}

              {/* Quick demo profiles */}
              <div className="mt-5 pt-4 border-t border-slate-100">
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2 flex items-center justify-between">
                  <span className="flex items-center gap-1">
                    <Sparkles className="w-3 h-3 text-[#2D6A4F]" />
                    <span>Pan-India 1-Click Farmers</span>
                  </span>
                  <span className="text-[9px] text-[#2D6A4F] font-bold">0 Delay</span>
                </p>
                <div className="grid grid-cols-3 gap-2">
                  {DEMO_FARMERS.slice(0, 6).map((f) => (
                    <button
                      key={f.id}
                      type="button"
                      onClick={() => quickFarmerLogin(f)}
                      className="p-2 rounded-xl bg-[#F3F6F1] hover:bg-[#D4E09B]/40 text-left border border-slate-200 transition-colors cursor-pointer"
                    >
                      <div className="flex items-center justify-between">
                        <p className="font-bold text-[10px] text-slate-900 truncate">{f.name.split(' ')[0]}</p>
                        <span className="text-[8px] font-bold px-0.5 rounded bg-white text-slate-500">{f.state?.slice(0, 2).toUpperCase()}</span>
                      </div>
                      <p className="text-[9px] text-slate-500 font-mono truncate">{f.phone}</p>
                    </button>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <form onSubmit={handleAdminLogin} className="space-y-3.5">
              {adminError && (
                <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{adminError}</span>
                </div>
              )}

              <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex items-start gap-2">
                <Lock className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" />
                <div>
                  <span className="font-bold">Passcode:</span>{' '}
                  <span className="font-mono bg-amber-100 px-1 py-0.5 rounded font-bold">Admin@India2026</span>
                  <span className="text-[10px] text-amber-800 ml-1">(or Admin@MPMandi2026)</span>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1 uppercase tracking-wider">
                  Officer ID
                </label>
                <input
                  type="text"
                  value={officerId}
                  onChange={(e) => setOfficerId(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-[#F3F6F1] border border-slate-300 rounded-xl text-xs font-mono font-bold text-slate-900"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1 uppercase tracking-wider">
                  Passcode
                </label>
                <input
                  type="password"
                  value={adminPasscode}
                  onChange={(e) => setAdminPasscode(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-[#F3F6F1] border border-slate-300 rounded-xl text-xs font-mono font-bold text-slate-900"
                  required
                />
              </div>

              {/* State Filter */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1 uppercase tracking-wider">
                  Select State
                </label>
                <select
                  value={adminStateFilter}
                  onChange={(e) => {
                    setAdminStateFilter(e.target.value);
                    const stateObj = INDIAN_STATES.find((s) => s.code === e.target.value);
                    const mandisInState = e.target.value === 'ALL'
                      ? ALL_INDIA_MANDIS
                      : ALL_INDIA_MANDIS.filter((m) => stateObj && m.state === stateObj.name);
                    if (mandisInState.length > 0) {
                      setSelectedMandiId(mandisInState[0].id);
                    }
                  }}
                  className="w-full p-2 bg-[#F3F6F1] border border-slate-300 rounded-xl text-xs font-bold text-slate-900"
                >
                  {INDIAN_STATES.map((s) => (
                    <option key={s.code} value={s.code}>
                      {s.name} ({s.code})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1 uppercase tracking-wider">
                  Procurement Mandi ({availableMandis.length})
                </label>
                <select
                  value={selectedMandiId}
                  onChange={(e) => setSelectedMandiId(e.target.value)}
                  className="w-full p-2.5 bg-[#F3F6F1] border border-slate-300 rounded-xl text-xs font-bold text-slate-900"
                >
                  {availableMandis.map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.name} ({m.district}, {m.state})
                    </option>
                  ))}
                </select>
              </div>

              <button
                type="submit"
                className="w-full py-3 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold uppercase tracking-wider rounded-xl text-xs flex items-center justify-center gap-2 cursor-pointer shadow-sm"
              >
                <Building2 className="w-4 h-4 text-[#D4E09B]" />
                <span>Administer Mandi (Instant)</span>
              </button>

              <div className="pt-2 border-t border-slate-200">
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1.5">⚡ 1-Click Fast Officer Logins</p>
                <div className="grid grid-cols-3 gap-1.5">
                  {[
                    { id: 'mandi-sehore', label: 'Sehore (MP)' },
                    { id: 'mandi-khanna', label: 'Khanna (PB)' },
                    { id: 'mandi-karnal', label: 'Karnal (HR)' },
                    { id: 'mandi-lasalgaon', label: 'Nashik (MH)' },
                    { id: 'mandi-kota', label: 'Kota (RJ)' },
                    { id: 'mandi-unjha', label: 'Unjha (GJ)' },
                  ].map((btn) => (
                    <button
                      key={btn.id}
                      type="button"
                      onClick={() => quickAdminLogin(btn.id)}
                      className="p-1.5 bg-[#D4E09B]/40 hover:bg-[#D4E09B]/80 text-[#1B4332] font-bold rounded-lg text-[10px] text-center border border-[#2D6A4F]/30 cursor-pointer truncate"
                    >
                      {btn.label}
                    </button>
                  ))}
                </div>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};
