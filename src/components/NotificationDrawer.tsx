import React, { useState } from 'react';
import {
  X,
  Bell,
  MessageSquare,
  CloudRain,
  Banknote,
  Clock,
  CheckCheck,
  Send,
  Sparkles,
} from 'lucide-react';
import { NotificationItem, Language } from '../types';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  notifications: NotificationItem[];
  onMarkAllRead: () => void;
  onSendTestSms: (type: 'SLOT' | 'QUEUE' | 'PAYMENT' | 'WEATHER') => void;
  language: Language;
}

export const NotificationDrawer: React.FC<Props> = ({
  isOpen,
  onClose,
  notifications,
  onMarkAllRead,
  onSendTestSms,
  language,
}) => {
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'SMS' | 'WEATHER' | 'PAYMENT'>('ALL');

  if (!isOpen) return null;

  const filtered = notifications.filter((n) => {
    if (activeFilter === 'ALL') return true;
    if (activeFilter === 'SMS') return n.type === 'SMS';
    if (activeFilter === 'WEATHER') return n.category === 'WEATHER';
    if (activeFilter === 'PAYMENT') return n.category === 'PAYMENT';
    return true;
  });

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-slate-900/60 backdrop-blur-xs flex justify-end">
      <div className="w-full max-w-md bg-white h-full shadow-2xl flex flex-col animate-in slide-in-from-right duration-200">
        {/* Drawer Header */}
        <div className="p-5 bg-[#1B4332] text-white flex items-center justify-between border-b border-[#2D6A4F]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-white/10 text-[#D4E09B] flex items-center justify-center shadow-xs">
              <Bell className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white">
                {language === 'hi' ? 'एसएमएस एवं अलर्ट केंद्र' : 'SMS & Real-Time Alerts'}
              </h3>
              <p className="text-[11px] text-[#D4E09B]/90 mt-0.5">
                {language === 'hi' ? 'मंडी शेड्यूलिंग, टोकन एवं मौसम सूचनाएं' : 'Mandi scheduling, tokens & weather'}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl hover:bg-white/10 text-white/70 hover:text-white cursor-pointer transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Quick Test SMS Simulator Buttons */}
        <div className="p-4 bg-[#F3F6F1] border-b border-slate-200/80">
          <div className="flex items-center justify-between mb-2.5">
            <span className="text-[10px] font-bold text-[#1B4332] uppercase tracking-wider flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-[#2D6A4F]" />
              Simulate Live Indian Telecom SMS
            </span>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <button
              onClick={() => onSendTestSms('QUEUE')}
              className="px-3 py-2 bg-white hover:bg-[#D4E09B]/30 text-slate-800 text-[11px] font-bold rounded-xl border border-slate-200 text-left truncate cursor-pointer transition-colors shadow-2xs"
            >
              📢 Token Call-In SMS
            </button>
            <button
              onClick={() => onSendTestSms('PAYMENT')}
              className="px-3 py-2 bg-white hover:bg-[#D4E09B]/30 text-slate-800 text-[11px] font-bold rounded-xl border border-slate-200 text-left truncate cursor-pointer transition-colors shadow-2xs"
            >
              💰 DBT ₹ Credited SMS
            </button>
            <button
              onClick={() => onSendTestSms('WEATHER')}
              className="px-3 py-2 bg-white hover:bg-[#D4E09B]/30 text-slate-800 text-[11px] font-bold rounded-xl border border-slate-200 text-left truncate cursor-pointer transition-colors shadow-2xs"
            >
              🌧️ Rain / IMD Alert
            </button>
            <button
              onClick={() => onSendTestSms('SLOT')}
              className="px-3 py-2 bg-white hover:bg-[#D4E09B]/30 text-slate-800 text-[11px] font-bold rounded-xl border border-slate-200 text-left truncate cursor-pointer transition-colors shadow-2xs"
            >
              🎟️ Slot Confirmed Pass
            </button>
          </div>
        </div>

        {/* Category Pills & Mark as Read */}
        <div className="px-4 py-3 bg-white border-b border-slate-100 flex items-center justify-between text-xs">
          <div className="flex items-center gap-1.5">
            <button
              onClick={() => setActiveFilter('ALL')}
              className={`px-3 py-1 rounded-xl text-[11px] font-bold uppercase tracking-wider transition-colors ${
                activeFilter === 'ALL' ? 'bg-[#1B4332] text-white shadow-xs' : 'text-slate-600 hover:bg-[#F3F6F1]'
              }`}
            >
              All
            </button>
            <button
              onClick={() => setActiveFilter('SMS')}
              className={`px-3 py-1 rounded-xl text-[11px] font-bold uppercase tracking-wider transition-colors ${
                activeFilter === 'SMS' ? 'bg-[#1B4332] text-white shadow-xs' : 'text-slate-600 hover:bg-[#F3F6F1]'
              }`}
            >
              SMS
            </button>
            <button
              onClick={() => setActiveFilter('WEATHER')}
              className={`px-3 py-1 rounded-xl text-[11px] font-bold uppercase tracking-wider transition-colors ${
                activeFilter === 'WEATHER' ? 'bg-[#1B4332] text-white shadow-xs' : 'text-slate-600 hover:bg-[#F3F6F1]'
              }`}
            >
              Weather
            </button>
            <button
              onClick={() => setActiveFilter('PAYMENT')}
              className={`px-3 py-1 rounded-xl text-[11px] font-bold uppercase tracking-wider transition-colors ${
                activeFilter === 'PAYMENT' ? 'bg-[#1B4332] text-white shadow-xs' : 'text-slate-600 hover:bg-[#F3F6F1]'
              }`}
            >
              Payments
            </button>
          </div>
          {notifications.length > 0 && (
            <button
              onClick={onMarkAllRead}
              className="text-[11px] text-[#1B4332] hover:text-[#2D6A4F] font-bold uppercase tracking-wider flex items-center gap-1 cursor-pointer transition-colors"
            >
              <CheckCheck className="w-3.5 h-3.5" />
              <span>Mark read</span>
            </button>
          )}
        </div>

        {/* Notification List */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3">
          {filtered.length === 0 ? (
            <div className="text-center py-12 text-slate-400">
              <MessageSquare className="w-10 h-10 mx-auto mb-2 opacity-40" />
              <p className="text-xs font-semibold">No notifications in this category</p>
              <p className="text-[11px] mt-1 text-slate-500">Tap one of the simulator buttons above to generate alerts</p>
            </div>
          ) : (
            filtered.map((notif) => (
              <div
                key={notif.id}
                className={`p-4 rounded-2xl border transition-all ${
                  notif.read
                    ? 'bg-white border-slate-200 text-slate-700'
                    : 'bg-[#F3F6F1] border-[#A3B18A]/60 text-slate-900 shadow-xs'
                }`}
              >
                {/* SMS Header Tag */}
                <div className="flex items-center justify-between text-[10px] text-slate-500 font-mono mb-2">
                  <span className="font-bold text-[#1B4332] bg-white px-2.5 py-0.5 rounded-full border border-slate-200">
                    {notif.senderTag}
                  </span>
                  <span>{new Date(notif.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                </div>

                <h4 className="text-xs font-bold text-slate-900 flex items-center gap-1.5">
                  {notif.category === 'WEATHER' && <CloudRain className="w-3.5 h-3.5 text-blue-600 shrink-0" />}
                  {notif.category === 'PAYMENT' && <Banknote className="w-3.5 h-3.5 text-[#2D6A4F] shrink-0" />}
                  {notif.category === 'QUEUE' && <Clock className="w-3.5 h-3.5 text-amber-600 shrink-0" />}
                  <span>{language === 'hi' ? notif.hindiTitle : notif.title}</span>
                </h4>

                <p className="text-xs text-slate-700 mt-2 leading-relaxed bg-white p-3 rounded-xl border border-slate-200/70 font-mono text-[11px]">
                  {language === 'hi' ? notif.hindiMessage : notif.message}
                </p>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
