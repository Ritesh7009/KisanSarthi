import React, { useState, useEffect } from 'react';
import {
  Send,
  MessageSquare,
  CheckCircle2,
  AlertCircle,
  Clock,
  Smartphone,
  X,
  RefreshCw,
  AlertTriangle,
  Info,
  Check,
  ShieldAlert,
} from 'lucide-react';
import { Language, FarmerProfile, SmsLogItem } from '../types';
import { apiUrl, getAuthToken } from '../services/api';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  language: Language;
  prefillFarmer?: {
    name: string;
    phone: string;
    aadharMasked?: string;
  };
}

const DLT_TEMPLATES = [
  {
    id: 'DLT-TE-1107161',
    title: 'Mandi Gate & Weighbridge Call (टोकन बुलावा)',
    template: (name: string, phone: string) =>
      `[MP-EUPARJAN] Priy Kisan ${name}, Aapka Token tol hetu Kanta Bay 1 par aamantrit hai. Kripya trolley ke sath entry gate par report karein.`,
  },
  {
    id: 'DLT-TE-1107162',
    title: 'Weighment & Moisture Verified (तौल एवं नमी प्रमाणीकरण)',
    template: (name: string, phone: string) =>
      `[MP-EUPARJAN] Priy Kisan ${name}, Tol Pranamit: Shuddh Tol 65.0 Qtl, Grade A, Nami 10.4%. Payout Rs 1,56,000 prakriya me hai. J-Form swikrit.`,
  },
  {
    id: 'DLT-TE-1107163',
    title: 'DBT Bank Transfer Credit (खाते में राशि अंतरण)',
    template: (name: string, phone: string) =>
      `[MP-EUPARJAN] Priy Kisan ${name}, Mandi J-Form rashi aapke Aadhaar linked bank khate me DBT dwara safaltapoorvak bhej di gayi hai. UTR: MPDBT${Date.now().toString().slice(-6)}.`,
  },
  {
    id: 'DLT-TE-1107164',
    title: 'Weather & Mandi Advisory (मौसम एवं तिरपाल सूचना)',
    template: (name: string, phone: string) =>
      `[MP-EUPARJAN] Mandi Suchna: Kripya trolley ko tarpaulin se dhak kar layein. Nami 12% se kam hone par turant tol kiya jayega.`,
  },
];

const TWILIO_TRIAL_DEFAULT_MESSAGE =
  'Your 1234 order of 1 items has shipped and should be delivered on tomorrow. Details: https://twilio.com';

export const AdminSmsDispatchModal: React.FC<Props> = ({
  isOpen,
  onClose,
  language,
  prefillFarmer,
}) => {
  const [farmers, setFarmers] = useState<FarmerProfile[]>([]);
  const [selectedFarmerPhone, setSelectedFarmerPhone] = useState(prefillFarmer?.phone || '');
  const [customPhone, setCustomPhone] = useState(prefillFarmer?.phone || '');
  const [customFarmerName, setCustomFarmerName] = useState(prefillFarmer?.name || 'Kisan');
  const [messageText, setMessageText] = useState('');
  const [selectedTemplateId, setSelectedTemplateId] = useState(DLT_TEMPLATES[0].id);

  // Dispatch mode: 'TRIAL_TEST' vs 'PRODUCTION_DLT'
  const [dispatchMode, setDispatchMode] = useState<'TRIAL_TEST' | 'PRODUCTION_DLT'>('TRIAL_TEST');
  const [trialConfig, setTrialConfig] = useState<{
    provider: string;
    trialMode: boolean;
    predefinedTemplateMessage: string;
    predefinedTemplateName: string;
  }>({
    provider: 'twilio',
    trialMode: true,
    predefinedTemplateMessage: TWILIO_TRIAL_DEFAULT_MESSAGE,
    predefinedTemplateName: 'Order Confirmations',
  });

  const [isSending, setIsSending] = useState(false);
  const [successNotice, setSuccessNotice] = useState<string | null>(null);
  const [errorNotice, setErrorNotice] = useState<string | null>(null);

  const [recentLogs, setRecentLogs] = useState<SmsLogItem[]>([]);
  const [isLoadingLogs, setIsLoadingLogs] = useState(false);

  // Load config, farmers, and logs from database on mount
  useEffect(() => {
    if (!isOpen) return;

    fetchConfig();
    fetchFarmers();
    fetchLogs();

    if (prefillFarmer) {
      setSelectedFarmerPhone(prefillFarmer.phone);
      setCustomPhone(prefillFarmer.phone);
      setCustomFarmerName(prefillFarmer.name);
      setMessageText(DLT_TEMPLATES[0].template(prefillFarmer.name, prefillFarmer.phone));
    }
  }, [isOpen, prefillFarmer]);

  const fetchConfig = async () => {
    try {
      const res = await fetch(apiUrl('/api/v1/sms/config'), {
        headers: {
          Authorization: `Bearer ${getAuthToken()}`,
        },
      });
      const json = await res.json();
      const cfg = json.data || json;
      if (cfg) {
        setTrialConfig({
          provider: cfg.provider || 'twilio',
          trialMode: cfg.trialMode !== false,
          predefinedTemplateMessage: cfg.predefinedTemplateMessage || TWILIO_TRIAL_DEFAULT_MESSAGE,
          predefinedTemplateName: cfg.predefinedTemplateName || 'Order Confirmations',
        });
        if (cfg.trialMode) {
          setDispatchMode('TRIAL_TEST');
        }
      }
    } catch (e) {
      console.warn('Failed to load SMS gateway config, defaulting to trial safe mode', e);
    }
  };

  const fetchFarmers = async () => {
    try {
      const res = await fetch(apiUrl('/api/v1/farmers'), {
        headers: {
          Authorization: `Bearer ${getAuthToken()}`,
        },
      });
      const data = await res.json();
      if (data.success && data.farmers) {
        setFarmers(data.farmers);
        if (!selectedFarmerPhone && data.farmers.length > 0) {
          const first = data.farmers[0];
          setSelectedFarmerPhone(first.phone);
          setCustomPhone(first.phone);
          setCustomFarmerName(first.name);
          setMessageText(DLT_TEMPLATES[0].template(first.name, first.phone));
        }
      } else if (Array.isArray(data)) {
        setFarmers(data);
        if (!selectedFarmerPhone && data.length > 0) {
          const first = data[0];
          setSelectedFarmerPhone(first.phone);
          setCustomPhone(first.phone);
          setCustomFarmerName(first.name);
          setMessageText(DLT_TEMPLATES[0].template(first.name, first.phone));
        }
      }
    } catch (e) {
      console.error('Failed to load farmers', e);
    }
  };

  const fetchLogs = async () => {
    setIsLoadingLogs(true);
    try {
      const res = await fetch(apiUrl('/api/v1/sms/logs'), {
        headers: {
          Authorization: `Bearer ${getAuthToken()}`,
        },
      });
      const data = await res.json();
      if (data.success && data.logs) {
        setRecentLogs(data.logs);
      } else if (Array.isArray(data)) {
        setRecentLogs(data);
      } else if (data.data && Array.isArray(data.data)) {
        setRecentLogs(data.data);
      }
    } catch (e) {
      console.error('Failed to load sms logs', e);
    } finally {
      setIsLoadingLogs(false);
    }
  };

  // Farmer selection change
  const handleFarmerSelect = (phone: string) => {
    setSelectedFarmerPhone(phone);
    setCustomPhone(phone);
    const found = farmers.find((f) => f.phone === phone);
    if (found) {
      setCustomFarmerName(found.name);
      const tmpl = DLT_TEMPLATES.find((t) => t.id === selectedTemplateId) || DLT_TEMPLATES[0];
      setMessageText(tmpl.template(found.name, found.phone));
    }
  };

  // Template select change
  const handleTemplateSelect = (templateId: string) => {
    setSelectedTemplateId(templateId);
    const tmpl = DLT_TEMPLATES.find((t) => t.id === templateId);
    if (tmpl) {
      setMessageText(tmpl.template(customFarmerName, customPhone));
    }
  };

  // Send Twilio Trial Test SMS
  const handleSendTrialTestSms = async () => {
    const phoneToSend = customPhone.replace(/\D/g, '');
    if (phoneToSend.length < 10) {
      setErrorNotice('Please enter a valid 10-digit mobile number');
      return;
    }

    setIsSending(true);
    setErrorNotice(null);
    setSuccessNotice(null);

    try {
      const res = await fetch(apiUrl('/api/v1/sms/trial-test'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${getAuthToken()}`,
        },
        body: JSON.stringify({
          recipientPhone: phoneToSend,
          farmerName: customFarmerName || 'Verified Test Recipient',
          message: trialConfig.predefinedTemplateMessage,
          channel: 'TWILIO_TRIAL_TEST',
          trialTest: true,
        }),
      });

      const data = await res.json();
      const payload = data.data || data;
      const isFailed = data.success === false || payload.status === 'FAILED';

      if (!isFailed && (payload.sid || payload.deliveryReceiptId || payload.status === 'ACCEPTED' || payload.status === 'QUEUED' || data.success)) {
        const sid = payload.sid || payload.deliveryReceiptId || 'Accepted';
        const status = payload.status || 'ACCEPTED';
        setSuccessNotice(
          `Twilio accepted Trial Test SMS for +91 ${phoneToSend}! SID: ${sid} (Status: ${status})`
        );
        fetchLogs();
      } else {
        const errorMsg =
          data.error?.message ||
          payload.errorMessage ||
          data.message ||
          payload.message ||
          'Twilio Trial Test SMS failed';
        setErrorNotice(typeof errorMsg === 'string' ? errorMsg : JSON.stringify(errorMsg));
      }
    } catch (err) {
      setErrorNotice('Network error sending Trial SMS');
    } finally {
      setIsSending(false);
    }
  };

  // Send via e-Uparjan Gateway API (Production / DLT)
  const handleSendGatewaySms = async (e: React.FormEvent) => {
    e.preventDefault();

    if (dispatchMode === 'TRIAL_TEST') {
      await handleSendTrialTestSms();
      return;
    }

    const phoneToSend = customPhone.replace(/\D/g, '');
    if (phoneToSend.length < 10) {
      setErrorNotice('Please enter a valid 10-digit mobile number');
      return;
    }
    if (!messageText.trim()) {
      setErrorNotice('Message text cannot be empty');
      return;
    }

    setIsSending(true);
    setErrorNotice(null);
    setSuccessNotice(null);

    try {
      const res = await fetch(apiUrl('/api/v1/sms/send'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${getAuthToken()}`,
        },
        body: JSON.stringify({
          recipientPhone: phoneToSend,
          farmerName: customFarmerName,
          aadharMasked: prefillFarmer?.aadharMasked || 'XXXX-XXXX-4589',
          message: messageText.trim(),
          senderHeader: 'VK-EUPARJAN',
          dltTemplateId: selectedTemplateId,
          dispatchedBy: 'Admin (Mandi Secretary)',
          channel: 'SMS_GATEWAY',
          trialTest: false,
        }),
      });

      const data = await res.json();
      const payload = data.data || data;
      const isFailed = data.success === false || payload.status === 'FAILED';
      const isAccepted =
        !isFailed &&
        (payload.sid || payload.deliveryReceiptId || payload.status === 'ACCEPTED' || payload.status === 'QUEUED' || data.success);

      if (isAccepted && !isFailed) {
        const sid = payload.sid || payload.deliveryReceiptId || 'Accepted';
        const status = payload.status || 'ACCEPTED';
        setSuccessNotice(
          `Twilio accepted SMS for +91 ${phoneToSend}! SID: ${sid} (Status: ${status})`
        );
        fetchLogs();
      } else {
        const errorMsg =
          data.error?.message ||
          payload.errorMessage ||
          data.message ||
          payload.message ||
          'Twilio SMS dispatch failed';
        setErrorNotice(typeof errorMsg === 'string' ? errorMsg : JSON.stringify(errorMsg));
      }
    } catch (err) {
      setErrorNotice('Network error sending SMS');
    } finally {
      setIsSending(false);
    }
  };

  // Send via Device SIM (Opens Phone SMS App)
  const handleSendDirectSimSms = async () => {
    const phoneToSend = customPhone.replace(/\D/g, '');
    if (phoneToSend.length < 10) {
      setErrorNotice('Valid 10-digit mobile number required');
      return;
    }

    const textToSend =
      dispatchMode === 'TRIAL_TEST'
        ? trialConfig.predefinedTemplateMessage
        : messageText.trim();

    try {
      await fetch(apiUrl('/api/v1/sms/send'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${getAuthToken()}`,
        },
        body: JSON.stringify({
          recipientPhone: phoneToSend,
          farmerName: customFarmerName,
          message: textToSend,
          senderHeader: 'VK-EUPARJAN',
          channel: 'SIM_DIRECT',
          dispatchedBy: 'Admin Direct Mobile SIM',
        }),
      });
      fetchLogs();
    } catch {}

    const smsUrl = `sms:+91${phoneToSend}?body=${encodeURIComponent(textToSend)}`;
    window.location.href = smsUrl;
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
      <div className="bg-white rounded-3xl shadow-2xl max-w-2xl w-full max-h-[90vh] flex flex-col overflow-hidden border border-slate-200 animate-in fade-in zoom-in-95 duration-150">
        {/* Modal Header */}
        <div className="bg-[#1B4332] text-white p-6 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-white/10 backdrop-blur-md flex items-center justify-center ring-1 ring-white/20">
              <MessageSquare className="w-5 h-5 text-[#D4E09B]" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">
                Send Real SMS to Farmer (वास्तविक एसएमएस प्रेषण)
              </h3>
              <p className="text-xs text-[#D4E09B]/80">
                Twilio Gateway • Live Status Tracking • Verified Recipient Delivery
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-5">
          {/* Mode Switcher Tabs */}
          <div className="flex items-center bg-[#F3F6F1] p-1.5 rounded-2xl border border-slate-200">
            <button
              type="button"
              id="mode-tab-trial"
              onClick={() => {
                setDispatchMode('TRIAL_TEST');
                setErrorNotice(null);
                setSuccessNotice(null);
              }}
              className={`flex-1 py-2.5 px-3 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center justify-center gap-2 ${
                dispatchMode === 'TRIAL_TEST'
                  ? 'bg-[#1B4332] text-white shadow-sm'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Check className="w-3.5 h-3.5 text-[#D4E09B]" />
              <span>Twilio Trial Test (Connectivity)</span>
              {trialConfig.trialMode && (
                <span className="text-[10px] bg-[#D4E09B] text-[#1B4332] px-2 py-0.5 rounded-full font-extrabold uppercase">
                  Active
                </span>
              )}
            </button>

            <button
              type="button"
              id="mode-tab-dlt"
              onClick={() => {
                setDispatchMode('PRODUCTION_DLT');
                setErrorNotice(null);
                setSuccessNotice(null);
              }}
              className={`flex-1 py-2.5 px-3 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center justify-center gap-2 ${
                dispatchMode === 'PRODUCTION_DLT'
                  ? 'bg-[#1B4332] text-white shadow-sm'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <span>Production / DLT SMS</span>
              {trialConfig.trialMode && (
                <span className="text-[10px] bg-amber-200 text-amber-900 px-2 py-0.5 rounded-full font-extrabold uppercase">
                  Restricted
                </span>
              )}
            </button>
          </div>

          {/* Trial Environment Notice Banner */}
          {trialConfig.trialMode && (
            <div className="p-3.5 rounded-2xl bg-amber-50 border border-amber-200 text-xs text-amber-900 space-y-1">
              <div className="flex items-center gap-2 font-bold">
                <AlertTriangle className="w-4 h-4 text-amber-700 shrink-0" />
                <span>Twilio Trial Account Mode</span>
              </div>
              <p className="text-[11px] text-amber-800 leading-relaxed">
                Twilio Trial accounts only permit SMS delivery to verified phone numbers using Twilio pre-approved templates (e.g., Order Confirmations). Custom message bodies are rejected by Twilio until the account is upgraded.
              </p>
            </div>
          )}

          {/* Status notices */}
          {successNotice && (
            <div className="p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-start gap-2.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <div className="leading-relaxed font-medium">{successNotice}</div>
            </div>
          )}

          {errorNotice && (
            <div className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-xs text-rose-700 flex flex-col gap-2">
              <div className="flex items-start gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span className="leading-relaxed font-medium">{errorNotice}</span>
              </div>
              {errorNotice.includes('Twilio Trial accounts cannot send this custom message') && (
                <button
                  type="button"
                  onClick={() => {
                    setDispatchMode('TRIAL_TEST');
                    setErrorNotice(null);
                  }}
                  className="self-start text-[11px] font-bold text-emerald-800 bg-emerald-100 hover:bg-emerald-200 px-3 py-1.5 rounded-xl transition-colors cursor-pointer"
                >
                  Switch to Twilio Trial Test Mode →
                </button>
              )}
            </div>
          )}

          <form onSubmit={handleSendGatewaySms} className="space-y-4">
            {/* Quick Pick: Registered Farmer from Database */}
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                Select Recipient / Farmer (प्राप्तकर्ता चुनें)
              </label>
              <select
                id="sms-select-farmer"
                value={selectedFarmerPhone}
                onChange={(e) => handleFarmerSelect(e.target.value)}
                className="w-full py-2.5 px-3 bg-[#F3F6F1] border border-slate-300 rounded-2xl text-xs font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
              >
                {farmers.map((f) => (
                  <option key={f.id} value={f.phone}>
                    {f.name} (+91 {f.phone}) — {f.district}
                  </option>
                ))}
              </select>
            </div>

            {/* Recipient Number and Name */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                  Recipient Mobile (+91)
                </label>
                <div className="relative">
                  <span className="absolute left-3.5 top-2.5 text-xs font-bold text-slate-500">+91</span>
                  <input
                    id="sms-phone-input"
                    type="tel"
                    value={customPhone}
                    onChange={(e) => setCustomPhone(e.target.value.replace(/\D/g, '').slice(0, 10))}
                    placeholder="98260XXXXX"
                    maxLength={10}
                    className="w-full pl-12 pr-3 py-2.5 bg-[#F3F6F1] border border-slate-300 rounded-2xl text-xs font-mono font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F] focus:bg-white"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                  Recipient / Farmer Name
                </label>
                <input
                  id="sms-farmer-name-input"
                  type="text"
                  value={customFarmerName}
                  onChange={(e) => setCustomFarmerName(e.target.value)}
                  placeholder="Farmer Name"
                  className="w-full py-2.5 px-3 bg-[#F3F6F1] border border-slate-300 rounded-2xl text-xs font-semibold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F] focus:bg-white"
                  required
                />
              </div>
            </div>

            {/* If in TRIAL_TEST mode, display the Twilio pre-approved template info */}
            {dispatchMode === 'TRIAL_TEST' ? (
              <div className="p-4 rounded-2xl bg-emerald-50/70 border border-emerald-200 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Info className="w-4 h-4 text-emerald-700" />
                    <span className="text-xs font-bold text-emerald-950 uppercase tracking-wider">
                      Pre-approved Trial Template: {trialConfig.predefinedTemplateName}
                    </span>
                  </div>
                  <span className="text-[10px] font-mono bg-emerald-200/80 text-emerald-900 px-2 py-0.5 rounded-full font-bold">
                    TWILIO-VERIFIED
                  </span>
                </div>
                <div className="p-3 bg-white rounded-xl border border-emerald-200 text-xs font-mono text-slate-800 leading-relaxed select-all">
                  {trialConfig.predefinedTemplateMessage}
                </div>
                <p className="text-[11px] text-emerald-800">
                  This message is accepted and delivered by Twilio Trial accounts to verified numbers.
                </p>
              </div>
            ) : (
              <>
                {/* Production DLT Template Selector */}
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                      TRAI DLT Approved SMS Template (अनुमोदित संदेश टेम्पलेट)
                    </label>
                    {trialConfig.trialMode && (
                      <span className="text-[10px] text-amber-700 font-bold flex items-center gap-1">
                        <ShieldAlert className="w-3 h-3" />
                        Requires Upgraded Account
                      </span>
                    )}
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {DLT_TEMPLATES.map((tmpl) => (
                      <button
                        key={tmpl.id}
                        type="button"
                        onClick={() => handleTemplateSelect(tmpl.id)}
                        className={`p-2.5 rounded-xl border text-left transition-all cursor-pointer ${
                          selectedTemplateId === tmpl.id
                            ? 'bg-[#D4E09B]/30 border-[#2D6A4F] ring-1 ring-[#2D6A4F]'
                            : 'bg-[#F3F6F1] border-slate-200 hover:bg-slate-100'
                        }`}
                      >
                        <p className="text-xs font-bold text-slate-900">{tmpl.title}</p>
                        <p className="text-[10px] text-slate-500 font-mono mt-0.5">{tmpl.id}</p>
                      </button>
                    ))}
                  </div>
                </div>

                {/* Message Body */}
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                      Message Content (SMS बॉडी)
                    </label>
                    <span className="text-[10px] font-mono text-slate-500">
                      {messageText.length} chars • Sender: <strong className="text-[#1B4332]">VK-EUPARJAN</strong>
                    </span>
                  </div>
                  <textarea
                    id="sms-message-text"
                    rows={4}
                    value={messageText}
                    onChange={(e) => setMessageText(e.target.value)}
                    placeholder="Enter SMS text..."
                    className="w-full p-3 bg-[#F3F6F1] border border-slate-300 rounded-2xl text-xs font-medium text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F] focus:bg-white"
                    required
                  />
                </div>
              </>
            )}

            {/* Action Buttons */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
              <button
                id="send-gateway-sms-btn"
                type="submit"
                disabled={isSending}
                className="py-3 px-4 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold uppercase tracking-wider rounded-2xl text-xs transition-colors shadow-sm flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
              >
                <Send className="w-4 h-4 text-[#D4E09B]" />
                <span>
                  {isSending
                    ? 'Dispatching SMS...'
                    : dispatchMode === 'TRIAL_TEST'
                    ? 'Send Twilio Trial Test SMS'
                    : 'Send via SMS Gateway'}
                </span>
              </button>

              <button
                id="send-sim-sms-btn"
                type="button"
                onClick={handleSendDirectSimSms}
                className="py-3 px-4 bg-emerald-600 hover:bg-emerald-700 text-white font-bold uppercase tracking-wider rounded-2xl text-xs transition-colors shadow-sm flex items-center justify-center gap-2 cursor-pointer"
                title="Opens the SMS App on your phone/computer with pre-filled message"
              >
                <Smartphone className="w-4 h-4" />
                <span>Send via Device SIM (Direct)</span>
              </button>
            </div>
          </form>

          {/* Recent Live SMS Dispatch Logs */}
          <div className="pt-4 border-t border-slate-200">
            <div className="flex items-center justify-between mb-3">
              <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-[#2D6A4F]" />
                <span>Live SMS Dispatch History (प्रेषित संदेश रिकॉर्ड)</span>
              </h4>
              <button
                type="button"
                onClick={fetchLogs}
                className="text-[11px] text-[#2D6A4F] hover:underline flex items-center gap-1 cursor-pointer"
              >
                <RefreshCw className={`w-3 h-3 ${isLoadingLogs ? 'animate-spin' : ''}`} />
                <span>Refresh</span>
              </button>
            </div>

            <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
              {recentLogs.length === 0 ? (
                <p className="text-xs text-slate-400 italic text-center py-4">No SMS records found</p>
              ) : (
                recentLogs.map((log) => {
                  const isAccepted =
                    log.status === 'ACCEPTED' ||
                    log.status === 'QUEUED' ||
                    log.status === 'SENT' ||
                    log.status === 'DELIVERED';
                  return (
                    <div
                      key={log.id}
                      className="p-3 rounded-2xl bg-[#F3F6F1] border border-slate-200/80 text-xs space-y-1.5"
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-slate-900">
                            +91 {log.recipientPhone} • {log.farmerName}
                          </span>
                          {log.channel === 'TWILIO_TRIAL_TEST' && (
                            <span className="px-1.5 py-0.5 rounded text-[9px] font-bold bg-amber-100 text-amber-900">
                              Trial Test
                            </span>
                          )}
                        </div>
                        <span
                          className={`px-2 py-0.5 rounded-full text-[9px] font-bold font-mono uppercase tracking-wider ${
                            isAccepted
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-rose-100 text-rose-800'
                          }`}
                        >
                          {log.status}
                        </span>
                      </div>
                      <p className="text-slate-700 text-[11px] leading-relaxed line-clamp-2">{log.message}</p>
                      <div className="flex items-center justify-between text-[10px] text-slate-500 font-mono pt-0.5">
                        <span>
                          {log.deliveryReceiptId ? `SID: ${log.deliveryReceiptId}` : 'Ref: Provider Dispatched'}
                        </span>
                        <span>{new Date(log.dispatchedAt).toLocaleTimeString()}</span>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
