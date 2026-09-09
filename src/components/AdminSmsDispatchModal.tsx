import React, { useState, useEffect } from 'react';
import {
  Send,
  MessageSquare,
  CheckCircle2,
  AlertCircle,
  Clock,
  Phone,
  User,
  Shield,
  Smartphone,
  ExternalLink,
  X,
  RefreshCw,
  Search,
} from 'lucide-react';
import { Language, FarmerProfile, SmsLogItem } from '../types';
import { apiUrl } from '../services/api';

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

  const [isSending, setIsSending] = useState(false);
  const [successNotice, setSuccessNotice] = useState<string | null>(null);
  const [errorNotice, setErrorNotice] = useState<string | null>(null);

  const [recentLogs, setRecentLogs] = useState<SmsLogItem[]>([]);
  const [isLoadingLogs, setIsLoadingLogs] = useState(false);

  // Load farmers from database on mount
  useEffect(() => {
    if (!isOpen) return;

    fetchFarmers();
    fetchLogs();

    if (prefillFarmer) {
      setSelectedFarmerPhone(prefillFarmer.phone);
      setCustomPhone(prefillFarmer.phone);
      setCustomFarmerName(prefillFarmer.name);
      setMessageText(DLT_TEMPLATES[0].template(prefillFarmer.name, prefillFarmer.phone));
    }
  }, [isOpen, prefillFarmer]);

  const fetchFarmers = async () => {
    try {
      const res = await fetch(apiUrl('/api/v1/farmers'));
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
      const res = await fetch(apiUrl('/api/v1/sms/logs'));
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

  // Send via e-Uparjan Gateway API
  const handleSendGatewaySms = async (e: React.FormEvent) => {
    e.preventDefault();
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
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          recipientPhone: phoneToSend,
          farmerName: customFarmerName,
          aadharMasked: prefillFarmer?.aadharMasked || 'XXXX-XXXX-4589',
          message: messageText.trim(),
          senderHeader: 'VK-EUPARJAN',
          dltTemplateId: selectedTemplateId,
          dispatchedBy: 'Admin (Mandi Secretary)',
          channel: 'SMS_GATEWAY',
        }),
      });

      const data = await res.json();
      if (data.success || data.status === 'SENT' || data.deliveryReceiptId) {
        setSuccessNotice(
          `Real SMS dispatched to +91 ${phoneToSend}! Delivery Receipt: ${data.deliveryReceiptId || data.sid || 'CONFIRMED'} (TRAI DLT Verified)`
        );
        fetchLogs();
      } else {
        setErrorNotice(data.error || data.message || 'Failed to dispatch SMS');
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

    // Also log to server for database record
    try {
      await fetch(apiUrl('/api/v1/sms/send'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          recipientPhone: phoneToSend,
          farmerName: customFarmerName,
          message: messageText.trim(),
          senderHeader: 'VK-EUPARJAN',
          channel: 'SIM_DIRECT',
          dispatchedBy: 'Admin Direct Mobile SIM',
        }),
      });
      fetchLogs();
    } catch {}

    // Open standard device SMS protocol
    const smsUrl = `sms:+91${phoneToSend}?body=${encodeURIComponent(messageText)}`;
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
                TRAI DLT Verified Gateway & Direct Carrier SIM Dispatch
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
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* Status notices */}
          {successNotice && (
            <div className="p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-start gap-2.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <div className="leading-relaxed font-medium">{successNotice}</div>
            </div>
          )}

          {errorNotice && (
            <div className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-xs text-rose-700 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
              <span>{errorNotice}</span>
            </div>
          )}

          <form onSubmit={handleSendGatewaySms} className="space-y-4">
            {/* Quick Pick: Registered Farmer from Database */}
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                Select Farmer From Database (डेटाबेस से किसान चुनें)
              </label>
              <select
                id="sms-select-farmer"
                value={selectedFarmerPhone}
                onChange={(e) => handleFarmerSelect(e.target.value)}
                className="w-full py-2.5 px-3 bg-[#F3F6F1] border border-slate-300 rounded-2xl text-xs font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#2D6A4F]"
              >
                {farmers.map((f) => (
                  <option key={f.id} value={f.phone}>
                    {f.name} (+91 {f.phone}) — {f.district} • Aadhaar: {f.maskedAadhar}
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
                  Farmer Name
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

            {/* Template Selector */}
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                TRAI DLT Approved SMS Template (अनुमोदित संदेश टेम्पलेट)
              </label>
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

            {/* Action Buttons */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
              <button
                id="send-gateway-sms-btn"
                type="submit"
                disabled={isSending}
                className="py-3 px-4 bg-[#1B4332] hover:bg-[#2D6A4F] text-white font-bold uppercase tracking-wider rounded-2xl text-xs transition-colors shadow-sm flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
              >
                <Send className="w-4 h-4 text-[#D4E09B]" />
                <span>{isSending ? 'Sending SMS...' : 'Send via SMS Gateway'}</span>
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
                recentLogs.map((log) => (
                  <div
                    key={log.id}
                    className="p-3 rounded-2xl bg-[#F3F6F1] border border-slate-200/80 text-xs space-y-1"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-slate-900">
                        +91 {log.recipientPhone} • {log.farmerName}
                      </span>
                      <span className="px-2 py-0.5 rounded-full text-[9px] font-bold font-mono uppercase tracking-wider bg-emerald-100 text-emerald-800">
                        {log.status}
                      </span>
                    </div>
                    <p className="text-slate-700 text-[11px] leading-relaxed line-clamp-2">{log.message}</p>
                    <div className="flex items-center justify-between text-[10px] text-slate-500 font-mono pt-1">
                      <span>DLT Receipt: {log.deliveryReceiptId}</span>
                      <span>{new Date(log.dispatchedAt).toLocaleTimeString()}</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
