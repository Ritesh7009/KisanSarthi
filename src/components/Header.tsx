import React from 'react';
import { motion } from 'motion/react';
import {
  Bell,
  Globe,
  Wifi,
  WifiOff,
  UserCheck,
  Shield,
  RefreshCw,
  LogOut,
  Tractor,
} from 'lucide-react';
import { Language, UserRole } from '../types';
import { translations } from '../i18n/translations';

interface Props {
  language: Language;
  onLanguageChange: (lang: Language) => void;
  role: UserRole;
  onRoleSwitch: (role: UserRole) => void;
  isOnline: boolean;
  pendingSyncCount: number;
  onManualSync: () => void;
  unreadNotifsCount: number;
  onOpenNotifications: () => void;
  currentUser: { name: string; phone: string; district: string } | null;
  onLogout: () => void;
}

export const Header: React.FC<Props> = ({
  language,
  onLanguageChange,
  role,
  onRoleSwitch,
  isOnline,
  pendingSyncCount,
  onManualSync,
  unreadNotifsCount,
  onOpenNotifications,
  currentUser,
  onLogout,
}) => {
  const t = translations[language];

  return (
    <motion.header
      initial={{ opacity: 0, y: -10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.2 }}
      className="sticky top-0 z-40 bg-white border-b border-slate-200 shadow-sm"
    >
      {/* Top micro-bar with MP Govt Branding */}
      <div className="bg-gradient-to-r from-[#1B4332] via-[#2D6A4F] to-[#1B4332] text-emerald-50 px-4 py-1 text-[11px] font-medium flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#D4E09B] opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-[#D4E09B]"></span>
          </span>
          <span className="tracking-tight">{t.govtTag}</span>
        </div>
        <div className="flex items-center gap-3">
          <span className="hidden md:inline text-emerald-100/80">{t.tagline}</span>
          <div className="flex items-center gap-1.5">
            {isOnline ? (
              <span className="inline-flex items-center gap-1 text-emerald-100 bg-emerald-950/40 px-2 py-0.5 rounded-full text-[10px] font-medium">
                <Wifi className="w-3 h-3 text-[#D4E09B]" />
                <span className="hidden sm:inline">{t.onlineSynced}</span>
              </span>
            ) : (
              <motion.button
                whileHover={{ scale: 1.04 }}
                whileTap={{ scale: 0.96 }}
                onClick={onManualSync}
                className="inline-flex items-center gap-1 text-amber-100 bg-amber-950/50 px-2.5 py-0.5 rounded-full text-[10px] font-semibold hover:bg-amber-900 transition-colors cursor-pointer"
                title="Click to attempt sync"
              >
                <WifiOff className="w-3 h-3 text-amber-300" />
                <span>{t.offlineBadge}</span>
                {pendingSyncCount > 0 && (
                  <span className="bg-[#D4E09B] text-[#1B4332] px-1.5 rounded-full text-[9px] font-bold">
                    {pendingSyncCount}
                  </span>
                )}
              </motion.button>
            )}
          </div>
        </div>
      </div>

      {/* Main Navigation Bar */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3.5">
        <div className="flex items-center justify-between gap-3">
          {/* Logo & Brand */}
          <div className="flex items-center gap-3">
            <motion.div
              whileHover={{ rotate: [0, -6, 6, 0] }}
              transition={{ duration: 0.4 }}
              className="w-10 h-10 bg-[#2D6A4F] rounded-xl flex items-center justify-center text-white shadow-sm flex-shrink-0"
            >
              <Tractor className="w-5 h-5 text-white" />
            </motion.div>
            <div className="flex flex-col">
              <div className="flex items-center gap-2">
                <span className="text-xl font-bold tracking-tight text-[#1B4332] leading-tight">
                  {t.appTitle}
                </span>
                <span className="text-[10px] font-bold px-1.5 py-0.5 rounded-full bg-emerald-100 text-[#1B4332] uppercase tracking-wider">
                  MP
                </span>
              </div>
              <span className="text-[10px] uppercase tracking-widest font-semibold text-slate-400">
                {t.appSubtitle}
              </span>
            </div>
          </div>

          {/* Controls: Language, Center status, Role, User, Notifications */}
          <div className="flex items-center gap-2 sm:gap-4">
            {/* Center Status indicator */}
            <div className="hidden lg:flex items-center gap-1.5 text-xs font-medium text-slate-600 bg-slate-50 px-3 py-1.5 rounded-full border border-slate-200/70">
              <span className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"></span>
              <span>{currentUser?.district ? `MP-${currentUser.district} Center` : 'MP-Sehore Center'}</span>
            </div>

            {/* Role Switcher Pill with Animated Layout Transition */}
            <div className="flex bg-slate-100 p-1 rounded-full text-xs font-medium border border-slate-200/80 relative">
              <button
                id="role-switch-farmer"
                onClick={() => onRoleSwitch('FARMER')}
                className={`relative px-3.5 sm:px-4 py-1.5 rounded-full text-xs transition-colors cursor-pointer flex items-center gap-1.5 z-10 ${
                  role === 'FARMER'
                    ? 'text-[#1B4332] font-bold'
                    : 'text-slate-500 hover:text-slate-900'
                }`}
              >
                {role === 'FARMER' && (
                  <motion.div
                    layoutId="activeRolePill"
                    className="absolute inset-0 bg-white rounded-full shadow-xs -z-10"
                    transition={{ type: 'spring', stiffness: 450, damping: 32 }}
                  />
                )}
                <UserCheck className="w-3.5 h-3.5" />
                <span>{language === 'hi' ? 'किसान दृश्य' : language === 'mal' ? 'किसान दृश्य' : 'Farmer View'}</span>
              </button>
              <button
                id="role-switch-admin"
                onClick={() => onRoleSwitch('ADMIN')}
                className={`relative px-3.5 sm:px-4 py-1.5 rounded-full text-xs transition-colors cursor-pointer flex items-center gap-1.5 z-10 ${
                  role === 'ADMIN'
                    ? 'text-[#1B4332] font-bold'
                    : 'text-slate-500 hover:text-slate-900'
                }`}
              >
                {role === 'ADMIN' && (
                  <motion.div
                    layoutId="activeRolePill"
                    className="absolute inset-0 bg-white rounded-full shadow-xs -z-10"
                    transition={{ type: 'spring', stiffness: 450, damping: 32 }}
                  />
                )}
                <Shield className="w-3.5 h-3.5" />
                <span>{language === 'hi' ? 'प्रशासन' : language === 'mal' ? 'मंडी साहेब' : 'Admin Console'}</span>
              </button>
            </div>

            {/* Language Switcher Pill */}
            <div className="flex items-center bg-slate-100 p-1 rounded-full border border-slate-200/80 text-xs font-medium">
              <button
                onClick={() => onLanguageChange('en')}
                className={`px-2.5 py-1 text-xs rounded-full transition-all cursor-pointer ${
                  language === 'en'
                    ? 'bg-white text-[#1B4332] font-bold shadow-xs'
                    : 'text-slate-500 hover:text-slate-900'
                }`}
              >
                EN
              </button>
              <button
                onClick={() => onLanguageChange('hi')}
                className={`px-2.5 py-1 text-xs rounded-full transition-all cursor-pointer ${
                  language === 'hi'
                    ? 'bg-white text-[#1B4332] font-bold shadow-xs'
                    : 'text-slate-500 hover:text-slate-900'
                }`}
              >
                हिन्दी
              </button>
              <button
                onClick={() => onLanguageChange('mal')}
                className={`px-2.5 py-1 text-xs rounded-full transition-all cursor-pointer ${
                  language === 'mal'
                    ? 'bg-white text-[#1B4332] font-bold shadow-xs'
                    : 'text-slate-500 hover:text-slate-900'
                }`}
                title="मालवी बोली"
              >
                माळवी
              </button>
            </div>

            {/* Notification Bell with Badge & tactile bounce */}
            <motion.button
              whileHover={{ scale: 1.08 }}
              whileTap={{ scale: 0.92 }}
              id="notification-bell-btn"
              onClick={onOpenNotifications}
              className="relative p-2 rounded-full text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-colors cursor-pointer border border-slate-200/80"
              aria-label="View notifications"
            >
              <Bell className="w-4 h-4 text-slate-700" />
              {unreadNotifsCount > 0 && (
                <motion.span
                  initial={{ scale: 0 }}
                  animate={{ scale: [1, 1.25, 1] }}
                  transition={{ repeat: Infinity, repeatDelay: 3, duration: 0.6 }}
                  className="absolute -top-0.5 -right-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-rose-600 text-[9px] font-bold text-white ring-2 ring-white"
                >
                  {unreadNotifsCount}
                </motion.span>
              )}
            </motion.button>

            {/* Avatar Circle & User Details */}
            {currentUser && (
              <div className="flex items-center gap-2 pl-2 border-l border-slate-200">
                <div
                  className="w-8 h-8 rounded-full bg-[#D4E09B] border border-[#A3B18A] flex items-center justify-center text-[#1B4332] font-bold text-xs shadow-xs select-none"
                  title={currentUser.name}
                >
                  {currentUser.name ? currentUser.name.charAt(0).toUpperCase() : 'R'}
                </div>
                <div className="hidden xl:block text-left">
                  <p className="text-xs font-bold text-slate-800 leading-tight">
                    {currentUser.name}
                  </p>
                  <p className="text-[10px] text-slate-400 font-medium">
                    {currentUser.district}
                  </p>
                </div>
                <motion.button
                  whileHover={{ scale: 1.03 }}
                  whileTap={{ scale: 0.97 }}
                  id="logout-btn"
                  onClick={onLogout}
                  className="px-2.5 py-1 rounded-xl text-slate-500 hover:text-rose-700 hover:bg-rose-50 border border-slate-200 hover:border-rose-200 transition-colors cursor-pointer flex items-center gap-1.5 text-[11px] font-bold"
                  title={t.logout}
                >
                  <LogOut className="w-3.5 h-3.5 text-rose-500" />
                  <span className="hidden sm:inline">{language === 'hi' ? 'लॉगआउट' : 'Switch / Logout'}</span>
                </motion.button>
              </div>
            )}
          </div>
        </div>
      </div>
    </motion.header>
  );
};
