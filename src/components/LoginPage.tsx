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
  Lock,
  Languages,
  Send,
  RotateCcw,
  HelpCircle,
  MapPin,
  TrendingUp,
  ChevronDown,
  Calendar,
  BadgeCheck,
} from 'lucide-react';
import { Language, UserRole } from '../types';
import { translations } from '../i18n/translations';
import { DEMO_FARMERS, MP_MANDIS, MP_CROPS } from '../data/mpMandiData';
import { apiUrl, setAuthToken } from '../services/api';

interface Props {
  language: Language;
  onLanguageChange: (lang: Language) => void;
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

export const LoginPage: React.FC<Props> = ({ language, onLanguageChange, onLoginSuccess }) => {
  const t = translations[language];
  const [activeTab, setActiveTab] = useState<UserRole>('FARMER');

  // Farmer form state (Mobile number only)
  const [phone, setPhone] = useState('9826014522');
  const [otpSent, setOtpSent] = useState(false);
  const [otp, setOtp] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [statusNotification, setStatusNotification] = useState<string | null>(null);

  // Admin form state
  const [officerId, setOfficerId] = useState('MP-AGRI-ADMIN-701');
  const [adminPasscode, setAdminPasscode] = useState('Admin@MPMandi2026');
  const [selectedMandiId, setSelectedMandiId] = useState('mandi-sehore');
  const [openFaqIndex, setOpenFaqIndex] = useState<number | null>(null);

  const cleanPhone = phone.replace(/\D/g, '');

  // Step 1: Send OTP to Mobile Number (Securely without leaking OTP in response)
  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (cleanPhone.length < 10) {
      setErrorMessage(
        language === 'hi'
          ? 'कृपया मान्य 10-अंकीय मोबाइल नंबर दर्ज करें'
          : 'Please enter a valid 10-digit mobile number'
      );
      return;
    }

    setErrorMessage('');
    setIsLoading(true);

    try {
      const res = await fetch(apiUrl('/api/v1/auth/send-otp'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone: cleanPhone }),
      });
      const data = await res.json();
      if (data.success) {
        setOtpSent(true);
        setOtp('');
        setStatusNotification(
          language === 'hi'
            ? `ओटीपी +91 ******${cleanPhone.slice(-4)} पर प्रेषित। 5 मिनट के भीतर 6-अंकीय कोड दर्ज करें।`
            : `SMS OTP dispatched to +91 ******${cleanPhone.slice(-4)}. Enter the 6-digit verification code.`
        );
      } else {
        setErrorMessage(data.error || 'Failed to dispatch OTP');
      }
    } catch {
      // Network failure
      setErrorMessage(
        language === 'hi'
          ? 'नेटवर्क त्रुटि। कृपया पुनः प्रयास करें।'
          : 'Network error. Please try again.'
      );
    } finally {
      setIsLoading(false);
    }
  };

  // Step 2: Verify Mobile OTP & Login
  const handleVerifyFarmerOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (otp.trim().length < 4) {
      setErrorMessage(
        language === 'hi'
          ? 'कृपया मान्य सत्यापन कोड दर्ज करें'
          : 'Please enter the verification code received via SMS'
      );
      return;
    }

    setErrorMessage('');
    setIsLoading(true);

    try {
      const res = await fetch(apiUrl('/api/v1/auth/verify-otp'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          phone: cleanPhone,
          otp: otp.trim(),
        }),
      });

      const data = await res.json();
      if (data.success && (data.data || data.farmer)) {
        const token = data.data?.accessToken || data.accessToken || data.token;
        if (token) {
          setAuthToken(token);
        }
        const farmerUser = data.data?.user || data.farmer;
        onLoginSuccess({
          name: farmerUser.name,
          phone: farmerUser.phone,
          aadharNumber: farmerUser.aadharNumber,
          maskedAadhar: farmerUser.maskedAadhar,
          district: farmerUser.district,
          village: farmerUser.village,
          role: 'FARMER',
        });
      } else {
        setErrorMessage(data.error || data.message || 'Authentication failed');
      }
    } catch {
      setErrorMessage(
        language === 'hi'
          ? 'सर्वर से कनेक्ट करने में असमर्थ'
          : 'Unable to connect to authentication server'
      );
    } finally {
      setIsLoading(false);
    }
  };

  // Admin Login
  const handleAdminLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    setIsLoading(true);

    try {
      const res = await fetch(apiUrl('/api/v1/auth/admin-login'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: officerId,
          password: adminPasscode,
        }),
      });

      const data = await res.json();
      if (data.success && data.data) {
        if (data.data.accessToken) {
          setAuthToken(data.data.accessToken);
        }
        const user = { ...data.data.user };
        if (user && typeof user.role === 'string' && user.role.startsWith('ROLE_')) {
          user.role = user.role.replace('ROLE_', '');
        }
        const normalizedRole: UserRole =
          user.role === 'ADMIN' || user.role?.includes('ADMIN') || user.role?.includes('MANDI')
            ? 'ADMIN'
            : 'FARMER';
        onLoginSuccess({
          ...user,
          name: user.name || `Officer (${officerId})`,
          phone: user.phone || '07562-224810',
          district: user.district || 'Sehore',
          role: normalizedRole,
          mandiId: user.mandiId ? String(user.mandiId) : selectedMandiId,
        });
      } else {
        setErrorMessage(
          data.error ||
            data.message ||
            (language === 'hi'
              ? 'अमान्य पासकोड! अधिकृत पासकोड: Admin@MPMandi2026'
              : 'Invalid passcode! Authorized passcode: Admin@MPMandi2026')
        );
      }
    } catch {
      setErrorMessage(
        language === 'hi'
          ? 'प्रमाणीकरण सर्वर अनुपलब्ध है'
          : 'Authentication server unavailable'
      );
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickMobileSelect = (f: typeof DEMO_FARMERS[0]) => {
    setPhone(f.phone);
    setOtpSent(false);
    setErrorMessage('');
  };

  return (
    <div className="min-h-screen bg-[#F3F6F1] flex flex-col justify-between selection:bg-[#D4E09B] selection:text-[#1B4332]">
      {/* Top Government Navigation Ribbon */}
      <header className="bg-[#1B4332] text-white border-b border-[#2D6A4F] px-4 sm:px-8 py-3.5 shadow-md">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-white/10 backdrop-blur-md flex items-center justify-center ring-1 ring-white/20 shadow-xs">
              <Tractor className="w-5 h-5 text-[#D4E09B]" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-sm sm:text-base tracking-tight">KisanSetu MP • किसान सेतु</span>
                <span className="hidden sm:inline-block px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-[#D4E09B]/20 text-[#D4E09B] border border-[#D4E09B]/30">
                  e-Uparjan
                </span>
              </div>
              <p className="text-[11px] text-[#D4E09B]/80">
                Department of Farmer Welfare and Agriculture Development, Govt. of Madhya Pradesh
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => onLanguageChange(language === 'hi' ? 'en' : 'hi')}
              className="px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-bold uppercase tracking-wider flex items-center gap-1.5 transition-colors cursor-pointer border border-white/10"
              title="Toggle Language"
            >
              <Languages className="w-3.5 h-3.5 text-[#D4E09B]" />
              <span>{language === 'hi' ? 'English' : 'हिन्दी'}</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Login Screen & Public SEO Showcase */}
      <main className="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8 flex flex-col items-center">
        <div className="max-w-md w-full bg-white rounded-3xl shadow-xl border border-slate-200 overflow-hidden mb-12">
          {/* Header Banner */}
          <div className="bg-gradient-to-br from-[#1B4332] to-[#2D6A4F] text-white p-6 text-center relative">
            <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-white/10 backdrop-blur-md mb-3 ring-1 ring-white/20 shadow-inner">
              {activeTab === 'FARMER' ? (
                <Tractor className="w-7 h-7 text-[#D4E09B]" />
              ) : (
                <Shield className="w-7 h-7 text-[#D4E09B]" />
              )}
            </div>
            <h1 className="text-xl sm:text-2xl font-black tracking-tight text-white">
              {activeTab === 'FARMER'
                ? language === 'hi'
                  ? 'किसान लॉगिन'
                  : 'Farmer Login'
                : language === 'hi'
                ? 'विभागीय प्रशासक लॉगिन'
                : 'Department Admin Login'}
            </h1>
            <p className="text-xs text-[#D4E09B]/90 mt-1 max-w-sm mx-auto">
              {activeTab === 'FARMER'
                ? language === 'hi'
                  ? 'केवल 10-अंकीय मोबाइल नंबर द्वारा त्वरित ओटीपी लॉगिन'
                  : 'Real login with 10-digit mobile number & instant OTP'
                : language === 'hi'
                ? 'मंडी नियंत्रण कक्ष एवं एसएमएस प्रेषण पोर्टल'
                : 'Authorized department passcode to administer weighbridge and dispatch SMS'}
            </p>
          </div>

          {/* Role Switching Tabs */}
          <div className="grid grid-cols-2 p-1.5 bg-[#F3F6F1] border-b border-slate-200">
            <button
              id="login-tab-farmer"
              type="button"
              onClick={() => {
                setActiveTab('FARMER');
                setErrorMessage('');
              }}
              className={`py-3 text-xs font-bold uppercase tracking-wider rounded-2xl flex items-center justify-center gap-2 transition-all cursor-pointer ${
                activeTab === 'FARMER'
                  ? 'bg-white text-[#1B4332] shadow-sm ring-1 ring-slate-200'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Tractor className="w-4 h-4 text-[#2D6A4F]" />
              <span>{language === 'hi' ? 'किसान (मोबाइल)' : 'Farmer (Mobile)'}</span>
            </button>
            <button
              id="login-tab-admin"
              type="button"
              onClick={() => {
                setActiveTab('ADMIN');
                setErrorMessage('');
              }}
              className={`py-3 text-xs font-bold uppercase tracking-wider rounded-2xl flex items-center justify-center gap-2 transition-all cursor-pointer ${
                activeTab === 'ADMIN'
                  ? 'bg-white text-slate-900 shadow-sm ring-1 ring-slate-200'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Shield className="w-4 h-4 text-slate-700" />
              <span>{language === 'hi' ? 'विभागीय प्रशासक' : 'Admin'}</span>
            </button>
          </div>

          <div className="p-6">
            {/* Status & Error Alerts */}
            {statusNotification && (
              <div className="mb-4 p-3 rounded-2xl bg-[#D4E09B]/25 border border-[#A3B18A]/60 text-xs text-[#1B4332] flex items-start gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-[#2D6A4F] shrink-0 mt-0.5" />
                <div className="leading-relaxed font-medium">{statusNotification}</div>
              </div>
            )}

            {errorMessage && (
              <div className="mb-4 p-3 rounded-2xl bg-rose-50 border border-rose-200 text-xs text-rose-700 flex items-center gap-2.5">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                <span className="font-medium">{errorMessage}</span>
              </div>
            )}

            {/* ========================================================= */}
            {/* FARMER LOGIN: REAL LOGIN WITH MOBILE NUMBER ONLY METHOD  */}
            {/* ========================================================= */}
            {activeTab === 'FARMER' ? (
              <div>
                {!otpSent ? (
                  <form onSubmit={handleSendOtp} className="space-y-4">
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                        {language === 'hi' ? 'किसान का मोबाइल नंबर (Mobile Number)' : 'Farmer Mobile Number'} *
                      </label>
                      <div className="relative">
                        <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-600 font-bold text-sm">
                          <span>+91</span>
                        </div>
                        <input
                          id="farmer-login-phone"
                          type="tel"
                          value={phone}
                          onChange={(e) => setPhone(e.target.value.replace(/\D/g, '').slice(0, 10))}
                          placeholder="9826014522"
                          maxLength={10}
                          className="w-full pl-14 pr-4 py-3.5 bg-[#F3F6F1] border border-slate-300 rounded-2xl text-base font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F] focus:bg-white transition-all font-mono tracking-wider"
                          required
                          autoFocus
                        />
                      </div>
                      <p className="text-[11px] text-slate-500 mt-1.5 leading-relaxed">
                        {language === 'hi'
                          ? 'सत्यापन कोड (OTP) आपके इस मोबाइल नंबर पर प्रेषित किया जाएगा।'
                          : 'SMS OTP will be sent directly to this 10-digit mobile number.'}
                      </p>
                    </div>

                    <button
                      id="farmer-send-otp-button"
                      type="submit"
                      disabled={isLoading || cleanPhone.length < 10}
                      className="w-full py-3.5 px-4 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold uppercase tracking-wider rounded-2xl text-xs transition-all shadow-md flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <Send className="w-4 h-4 text-[#D4E09B]" />
                      <span>
                        {isLoading
                          ? language === 'hi'
                            ? 'ओटीपी भेजा जा रहा है...'
                            : 'Sending OTP...'
                          : language === 'hi'
                          ? 'ओटीपी प्राप्त करें (Get OTP)'
                          : 'Get SMS OTP'}
                      </span>
                      <ArrowRight className="w-4 h-4 text-[#D4E09B]" />
                    </button>
                  </form>
                ) : (
                  /* OTP VERIFICATION STEP */
                  <form onSubmit={handleVerifyFarmerOtp} className="space-y-4">
                    <div className="p-3.5 bg-[#F3F6F1] border border-[#A3B18A]/50 rounded-2xl text-xs text-[#1B4332]">
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="font-bold">
                            SMS sent to <span className="font-mono text-slate-900">+91 {cleanPhone}</span>
                          </p>
                          <p className="text-[11px] text-[#2D6A4F] mt-0.5">
                            Demo verification OTP: <strong className="font-mono text-base font-black text-[#1B4332]">4826</strong>
                          </p>
                        </div>
                        <CheckCircle2 className="w-5 h-5 text-[#2D6A4F] shrink-0" />
                      </div>
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider text-center">
                        {language === 'hi' ? '4-अंकीय ओटीपी दर्ज करें' : 'Enter 4-Digit SMS OTP'}
                      </label>
                      <input
                        id="farmer-verify-otp-input"
                        type="text"
                        value={otp}
                        onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 4))}
                        placeholder="4826"
                        maxLength={4}
                        className="w-full py-3 px-4 text-center tracking-[0.5em] text-2xl font-mono font-black bg-[#F3F6F1] border border-slate-300 rounded-2xl text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                        required
                        autoFocus
                      />
                    </div>

                    <button
                      id="farmer-complete-login-button"
                      type="submit"
                      disabled={isLoading}
                      className="w-full py-3.5 px-4 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold uppercase tracking-wider rounded-2xl text-xs transition-all shadow-md cursor-pointer disabled:opacity-50 flex items-center justify-center gap-2"
                    >
                      <CheckCircle2 className="w-4 h-4 text-[#D4E09B]" />
                      <span>
                        {isLoading
                          ? language === 'hi'
                            ? 'सत्यापन हो रहा है...'
                            : 'Verifying...'
                          : language === 'hi'
                          ? 'सत्यापित करें और प्रवेश करें'
                          : 'Verify & Enter Farmer Portal'}
                      </span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setOtpSent(false)}
                      className="w-full text-center text-xs text-slate-500 hover:text-slate-800 font-semibold cursor-pointer py-1 flex items-center justify-center gap-1"
                    >
                      <RotateCcw className="w-3 h-3" />
                      <span>{language === 'hi' ? 'मोबाइल नंबर बदलें' : 'Change Mobile Number'}</span>
                    </button>
                  </form>
                )}

                {/* Quick Persona Access for Testing */}
                <div className="mt-6 pt-5 border-t border-slate-200">
                  <p className="text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-2.5 flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-[#2D6A4F]" />
                    <span>{language === 'hi' ? '1-क्लिक टेस्ट मोबाइल नंबर' : '1-Click Test Numbers'}</span>
                  </p>
                  <div className="grid grid-cols-3 gap-2">
                    {DEMO_FARMERS.slice(0, 3).map((f) => (
                      <button
                        key={f.id}
                        type="button"
                        onClick={() => handleQuickMobileSelect(f)}
                        className={`p-2 rounded-xl text-left border transition-all cursor-pointer ${
                          cleanPhone === f.phone
                            ? 'bg-[#D4E09B]/40 border-[#2D6A4F] ring-1 ring-[#2D6A4F]'
                            : 'bg-[#F3F6F1] hover:bg-[#D4E09B]/20 border-slate-200'
                        }`}
                      >
                        <p className="font-bold text-[11px] text-slate-900 truncate">{f.name.split(' ')[0]}</p>
                        <p className="text-[10px] text-slate-600 font-mono">{f.phone}</p>
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            ) : (
              /* ========================================================= */
              /* DEPARTMENT ADMIN LOGIN                                    */
              /* ========================================================= */
              <form onSubmit={handleAdminLogin} className="space-y-4">
                <div className="p-3 rounded-2xl bg-amber-50 border border-amber-200/80 text-amber-900 text-xs flex items-start gap-2">
                  <Lock className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold">Official Department Passcode:</span>{' '}
                    <strong className="font-mono bg-amber-100 px-1 py-0.5 rounded text-slate-900">Admin@MPMandi2026</strong>
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                    {language === 'hi' ? 'विभागीय अधिकारी आईडी' : 'Department Officer ID'} *
                  </label>
                  <input
                    id="admin-officer-id"
                    type="text"
                    value={officerId}
                    onChange={(e) => setOfficerId(e.target.value)}
                    placeholder="MP-AGRI-ADMIN-701"
                    className="w-full px-4 py-2.5 bg-[#F3F6F1] border border-slate-300 rounded-2xl text-xs font-mono font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F] focus:bg-white"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                    {language === 'hi' ? 'विभागीय पासकोड' : 'Passcode'} *
                  </label>
                  <input
                    id="admin-passcode"
                    type="password"
                    value={adminPasscode}
                    onChange={(e) => setAdminPasscode(e.target.value)}
                    placeholder="••••••••"
                    className="w-full px-4 py-2.5 bg-[#F3F6F1] border border-slate-300 rounded-2xl text-xs font-mono font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F] focus:bg-white"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                    {language === 'hi' ? 'प्रभार मंडी केंद्र' : 'Assigned APMC Mandi'} *
                  </label>
                  <select
                    id="admin-mandi-select"
                    value={selectedMandiId}
                    onChange={(e) => setSelectedMandiId(e.target.value)}
                    className="w-full px-3 py-2.5 bg-[#F3F6F1] border border-slate-300 rounded-2xl text-xs font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
                  >
                    {MP_MANDIS.map((m) => (
                      <option key={m.id} value={m.id}>
                        {m.name} ({m.district})
                      </option>
                    ))}
                  </select>
                </div>

                <button
                  id="admin-login-button"
                  type="submit"
                  disabled={isLoading}
                  className="w-full py-3.5 px-4 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold uppercase tracking-wider rounded-2xl text-xs transition-all shadow-md cursor-pointer flex items-center justify-center gap-2"
                >
                  <Building2 className="w-4 h-4 text-[#D4E09B]" />
                  <span>
                    {isLoading
                      ? language === 'hi'
                        ? 'सत्यापित किया जा रहा है...'
                        : 'Authenticating...'
                      : language === 'hi'
                      ? 'मंडी नियंत्रण कक्ष में प्रवेश करें'
                      : 'Enter Mandi Admin Console'}
                  </span>
                </button>
              </form>
            )}
          </div>
        </div>

        {/* ========================================================= */}
        {/* PUBLIC CRAWLABLE SEO CONTENT & E-UPARJAN OVERVIEW         */}
        {/* ========================================================= */}
        <section
          id="public-mandi-overview"
          aria-label="KisanSetu MP Agricultural Procurement Services"
          className="w-full max-w-5xl space-y-8 mt-4"
        >
          {/* Key Metric Badges */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs text-center">
              <span className="text-2xl font-black text-[#1B4332] font-mono">11+</span>
              <p className="text-xs font-bold text-slate-700 mt-0.5">
                {language === 'hi' ? 'सक्रिय कृषि उपज मंडियां' : 'Active APMC Mandis'}
              </p>
              <span className="text-[10px] text-slate-500">Sehore, Harda, Ujjain, Bhopal...</span>
            </div>

            <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs text-center">
              <span className="text-2xl font-black text-[#2D6A4F] font-mono">₹2,425</span>
              <p className="text-xs font-bold text-slate-700 mt-0.5">
                {language === 'hi' ? 'गेहूं समर्थन मूल्य (प्रति क्विंटल)' : 'Wheat MSP (per Quintal)'}
              </p>
              <span className="text-[10px] text-emerald-700 font-semibold">+ Govt State Bonus</span>
            </div>

            <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs text-center">
              <span className="text-2xl font-black text-amber-700 font-mono">0-Wait</span>
              <p className="text-xs font-bold text-slate-700 mt-0.5">
                {language === 'hi' ? 'स्मार्ट डिजिटल टोकन कतार' : 'Digital Token Queue'}
              </p>
              <span className="text-[10px] text-slate-500">AI Weighbridge Scheduling</span>
            </div>

            <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs text-center">
              <span className="text-2xl font-black text-blue-700 font-mono">100% DBT</span>
              <p className="text-xs font-bold text-slate-700 mt-0.5">
                {language === 'hi' ? 'सीधा बैंक खाता अंतरण' : 'Direct Bank Transfer'}
              </p>
              <span className="text-[10px] text-slate-500">Aadhaar Linked Account</span>
            </div>
          </div>

          {/* Today's Official MSP Rates Grid */}
          <article className="bg-white rounded-3xl p-6 border border-slate-200 shadow-xs">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-4 pb-3 border-b border-slate-100">
              <div>
                <h2 className="text-base sm:text-lg font-black text-slate-900 flex items-center gap-2">
                  <TrendingUp className="w-5 h-5 text-[#2D6A4F]" />
                  <span>
                    {language === 'hi'
                      ? 'मध्य प्रदेश शासन घोषित न्यूनतम समर्थन मूल्य (MSP 2026-27)'
                      : 'Madhya Pradesh Government Minimum Support Price (MSP 2026-27)'}
                  </span>
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  {language === 'hi'
                    ? 'सभी पंजीकृत मंडियों में त्वरित तुलाई एवं सरकारी समर्थन मूल्य की गारंटी'
                    : 'Guaranteed procurement rates across all e-Uparjan APMC Mandis in MP'}
                </p>
              </div>
              <span className="px-3 py-1 bg-emerald-50 text-emerald-800 text-xs font-bold rounded-full border border-emerald-200 w-fit">
                {language === 'hi' ? 'आधिकारिक दरें' : 'Govt Approved'}
              </span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {MP_CROPS.slice(0, 4).map((crop) => (
                <div
                  key={crop.id}
                  className="p-3.5 rounded-2xl bg-[#F3F6F1] border border-slate-200/80 hover:border-[#2D6A4F] transition-all"
                >
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs font-black text-slate-800">
                      {language === 'hi' ? crop.hindiName : crop.name}
                    </span>
                    <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-white text-slate-600 border border-slate-200 uppercase">
                      {crop.season}
                    </span>
                  </div>
                  <div className="text-lg font-black text-[#1B4332] font-mono">
                    ₹{crop.totalMsp.toLocaleString('en-IN')}
                    <span className="text-[10px] font-normal text-slate-500">/क्विंटल</span>
                  </div>
                  <p className="text-[11px] text-slate-600 mt-1">
                    {language === 'hi' ? 'मानक नमी:' : 'Moisture Limit:'} <strong>{crop.moistureLimitPct}%</strong>
                  </p>
                </div>
              ))}
            </div>
          </article>

          {/* 4-Step Smart e-Uparjan Process */}
          <article className="bg-white rounded-3xl p-6 border border-slate-200 shadow-xs">
            <h2 className="text-base sm:text-lg font-black text-slate-900 mb-1 flex items-center gap-2">
              <BadgeCheck className="w-5 h-5 text-[#2D6A4F]" />
              <span>
                {language === 'hi'
                  ? 'किसान सेतु पर 4 सरल चरणों में मंडी स्लॉट एवं भुगतान'
                  : '4-Step Smart Mandi Slot Booking & DBT Procurement'}
              </span>
            </h2>
            <p className="text-xs text-slate-500 mb-6">
              {language === 'hi'
                ? 'कतार-मुक्त तुलाई, एसएमएस सूचना एवं 48-72 घंटों में बैंक भुगतान'
                : 'Zero weighbridge bottleneck, automated SMS alerts and verified DBT settlement'}
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="p-4 rounded-2xl bg-[#F3F6F1] border border-slate-200 relative">
                <span className="w-7 h-7 rounded-full bg-[#1B4332] text-white font-bold text-xs flex items-center justify-center mb-3">
                  1
                </span>
                <h3 className="text-xs font-black text-slate-900 mb-1">
                  {language === 'hi' ? 'मोबाइल नंबर से लॉगिन' : 'Mobile Number Login'}
                </h3>
                <p className="text-[11px] text-slate-600 leading-relaxed">
                  {language === 'hi'
                    ? '10-अंकीय मोबाइल नंबर दर्ज करें और तत्काल एसएमएस ओटीपी द्वारा सुरक्षित प्रवेश पाएं।'
                    : 'Enter 10-digit mobile number and authenticate securely with instant SMS OTP.'}
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-[#F3F6F1] border border-slate-200 relative">
                <span className="w-7 h-7 rounded-full bg-[#2D6A4F] text-white font-bold text-xs flex items-center justify-center mb-3">
                  2
                </span>
                <h3 className="text-xs font-black text-slate-900 mb-1">
                  {language === 'hi' ? 'लोकेशन से निकटतम मंडी' : 'Auto-Locate Nearest Mandi'}
                </h3>
                <p className="text-[11px] text-slate-600 leading-relaxed">
                  {language === 'hi'
                    ? 'जीपीएस लोकेशन से निकटतम उपार्जन केंद्र चुनें और ट्रैक्टर यात्रा समय देखें।'
                    : 'Pick the closest APMC facility with GPS Haversine distance and tractor travel time.'}
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-[#F3F6F1] border border-slate-200 relative">
                <span className="w-7 h-7 rounded-full bg-[#D4E09B] text-[#1B4332] font-black text-xs flex items-center justify-center mb-3">
                  3
                </span>
                <h3 className="text-xs font-black text-slate-900 mb-1">
                  {language === 'hi' ? 'डिजिटल ई-टोकन पास' : 'Instant Digital Pass'}
                </h3>
                <p className="text-[11px] text-slate-600 leading-relaxed">
                  {language === 'hi'
                    ? 'निश्चित तुलाई समय और टोकन नंबर प्राप्त करें। सीधे कांटा वे-ब्रिज पर प्रवेश।'
                    : 'Receive scheduled weighbridge time-slot and QR pass to bypass tractor congestion.'}
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-[#F3F6F1] border border-slate-200 relative">
                <span className="w-7 h-7 rounded-full bg-emerald-800 text-white font-bold text-xs flex items-center justify-center mb-3">
                  4
                </span>
                <h3 className="text-xs font-black text-slate-900 mb-1">
                  {language === 'hi' ? 'जे-फॉर्म व डीबीटी भुगतान' : 'J-Form & Direct DBT'}
                </h3>
                <p className="text-[11px] text-slate-600 leading-relaxed">
                  {language === 'hi'
                    ? 'तुलाई के तुरंत बाद डिजिटल जे-फॉर्म रसीद एवं बैंक खाते में डीबीटी भुगतान।'
                    : 'Download official digital J-Form and receive funds directly into Aadhaar bank account.'}
                </p>
              </div>
            </div>
          </article>

          {/* Search Engine Optimized FAQs (Aligned with JSON-LD Schema) */}
          <article id="faq-section" className="bg-white rounded-3xl p-6 border border-slate-200 shadow-xs">
            <h2 className="text-base sm:text-lg font-black text-slate-900 mb-1 flex items-center gap-2">
              <HelpCircle className="w-5 h-5 text-[#2D6A4F]" />
              <span>
                {language === 'hi'
                  ? 'अक्सर पूछे जाने वाले प्रश्न (e-Uparjan FAQs)'
                  : 'Frequently Asked Questions (e-Uparjan FAQs)'}
              </span>
            </h2>
            <p className="text-xs text-slate-500 mb-4">
              {language === 'hi'
                ? 'मध्य प्रदेश कृषि उपज मंडी स्लॉट एवं उपार्जन प्रक्रिया से संबंधित सामान्य प्रश्न'
                : 'Common questions on MP Mandi procurement, slot scheduling, and DBT credits'}
            </p>

            <div className="space-y-3">
              {[
                {
                  id: 0,
                  qEn: 'How to book an e-Uparjan Mandi slot on KisanSetu MP?',
                  qHi: 'किसान सेतु पर ई-उपार्जन मंडी स्लॉट कैसे बुक करें?',
                  aEn: 'Farmers can log in using their 10-digit mobile number, choose their district and nearest APMC Mandi (or use GPS location access), enter crop yield estimates, and generate a verified digital e-token pass with a dedicated weighbridge arrival window.',
                  aHi: 'किसान अपना 10-अंकीय मोबाइल नंबर दर्ज कर तुरंत ओटीपी प्राप्त कर सकते हैं। अपनी निकटतम कृषि उपज मंडी चुनें (या जीपीएस द्वारा ऑटो-सर्च करें), फसल मात्रा भरें और निश्चित समय का डिजिटल टोकन पास प्राप्त करें।',
                },
                {
                  id: 1,
                  qEn: 'How to track live Mandi weighbridge queues and token waiting numbers?',
                  qHi: 'मंडी तौल कांटे की लाइव कतार और टोकन नंबर कैसे देखें?',
                  aEn: 'The KisanSetu MP Live Mandi Queue monitor displays active token serving numbers, weighbridge statuses, queue lengths, and turnaround times for major MP hubs including Sehore, Harda, Ujjain, Bhopal, Indore, and Vidisha.',
                  aHi: 'किसान सेतु लाइव ट्रैकर पर सीहोर, हरदा, उज्जैन, भोपाल, इंदौर और विदिशा सहित प्रमुख मंडियों में चालू टोकन नंबर, तौल कांटे की स्थिति और प्रतीक्षा समय की वास्तविक जानकारी मिलती है।',
                },
                {
                  id: 2,
                  qEn: 'What are current MSP rates for Wheat, Soybean, and Mustard in MP?',
                  qHi: 'मध्य प्रदेश में गेहूं, सोयाबीन और सरसों का समर्थन मूल्य (MSP) क्या है?',
                  aEn: 'Madhya Pradesh provides guaranteed Minimum Support Prices (MSP) including state procurement bonus: Wheat (Gehun) at ₹2,425/quintal, Soybean at ₹4,892/quintal, Mustard (Sarson) at ₹5,650/quintal, and Gram (Chana) at ₹5,440/quintal.',
                  aHi: 'मध्य प्रदेश शासन द्वारा घोषित समर्थन मूल्य: गेहूं ₹2,425/क्विंटल, सोयाबीन ₹4,892/क्विंटल, सरसों ₹5,650/क्विंटल, और चना ₹5,440/क्विंटल सुनिश्चित है।',
                },
                {
                  id: 3,
                  qEn: 'How is DBT payment credited to farmer bank accounts after grain procurement?',
                  qHi: 'मंडी में फसल बेचने के बाद बैंक खाते में डीबीटी भुगतान कैसे प्राप्त होता है?',
                  aEn: 'Once gross weight, moisture QC testing, and tare weight are recorded at the APMC shed, an official J-Form slip is generated and Direct Benefit Transfer (DBT) is initiated directly to the farmer Aadhaar-linked bank account within 24 to 72 hours.',
                  aHi: 'उपार्जन केंद्र पर वजन एवं गुणवत्ता परीक्षण उपरांत डिजिटल जे-फॉर्म जारी किया जाता है। इसके 24 से 72 घंटों में किसान के आधार लिंक बैंक खाते में सीधे डीबीटी भुगतान अंतरित कर दिया जाता है।',
                },
              ].map((faq) => {
                const isOpen = openFaqIndex === faq.id;
                return (
                  <div
                    key={faq.id}
                    className="border border-slate-200 rounded-2xl overflow-hidden transition-colors"
                  >
                    <button
                      type="button"
                      onClick={() => setOpenFaqIndex(isOpen ? null : faq.id)}
                      className="w-full px-4 py-3 text-left font-bold text-xs sm:text-sm text-slate-800 bg-[#F3F6F1]/70 hover:bg-[#F3F6F1] flex items-center justify-between gap-3 cursor-pointer"
                    >
                      <span>{language === 'hi' ? faq.qHi : faq.qEn}</span>
                      <ChevronDown
                        className={`w-4 h-4 text-slate-500 shrink-0 transition-transform ${
                          isOpen ? 'rotate-180 text-[#2D6A4F]' : ''
                        }`}
                      />
                    </button>
                    {isOpen && (
                      <div className="px-4 py-3 bg-white text-xs text-slate-600 leading-relaxed border-t border-slate-100">
                        {language === 'hi' ? faq.aHi : faq.aEn}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </article>

          {/* Official Helplines & MP Agriculture Contacts */}
          <div className="bg-[#1B4332] text-white rounded-3xl p-6 shadow-sm">
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
              <div>
                <h3 className="text-sm sm:text-base font-black text-white">
                  {language === 'hi'
                    ? 'मध्य प्रदेश किसान सहायता एवं कॉल सेंटर'
                    : 'Madhya Pradesh Farmer Helplines & Assistance'}
                </h3>
                <p className="text-xs text-[#D4E09B]/90 mt-0.5">
                  {language === 'hi'
                    ? 'टोल-फ्री हेल्पलाइन, ई-उपार्जन पूछताछ एवं मंडी नियंत्रण कक्ष'
                    : 'Toll-free helpline, e-Uparjan queries and Mandi control room'}
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <div className="px-3.5 py-2 rounded-xl bg-white/10 backdrop-blur-xs text-xs font-mono font-bold">
                  Kisan Call Center:{' '}
                  <a href="tel:18001801551" className="text-[#D4E09B] underline">
                    1800-180-1551
                  </a>
                </div>
                <div className="px-3.5 py-2 rounded-xl bg-white/10 backdrop-blur-xs text-xs font-mono font-bold">
                  CM Helpline:{' '}
                  <a href="tel:181" className="text-[#D4E09B] underline">
                    181
                  </a>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="py-4 text-center text-xs text-slate-500 border-t border-slate-200 bg-white">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>KisanSetu MP • Govt. of Madhya Pradesh Agriculture Portal</span>
          <span className="text-[11px] text-slate-400">Security: 256-bit Encrypted • DLT Verified SMS</span>
        </div>
      </footer>
    </div>
  );
};
