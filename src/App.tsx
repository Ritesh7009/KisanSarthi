import React, { useCallback } from 'react';
import { useAuthSession, AuthUser } from './hooks/useAuthSession';
import { useBookingFlow } from './hooks/useBookingFlow';
import { useNotifications } from './hooks/useNotifications';
import { NotificationItem } from './types';
import { Header } from './components/Header';
import { SyntheticDataBanner } from './components/SyntheticDataBanner';
import { FarmerDashboard } from './components/FarmerDashboard';
import { AdminDashboard } from './components/AdminDashboard';
import { LoginModal } from './components/LoginModal';
import { LoginPage } from './components/LoginPage';
import { NotificationDrawer } from './components/NotificationDrawer';
import { TokenPassModal } from './components/TokenPassModal';
import { JFormReceiptModal } from './components/JFormReceiptModal';
import { Phone, Shield, Tractor } from 'lucide-react';

export function App() {
  // Notifications hook
  const {
    notifications,
    isNotifDrawerOpen,
    setIsNotifDrawerOpen,
    addNotification,
    handleMarkAllRead,
    handleSendTestSms,
    unreadCount,
  } = useNotifications();

  // Auth & Session hook
  const handleLoginNotification = useCallback((user: AuthUser) => {
    // Push welcome SMS notification
    const welcomeNotif: NotificationItem = {
      id: `notif-${Date.now()}`,
      type: 'SMS',
      title: `Welcome to KisanSarthi MP, ${user.name}`,
      message: `Your login is verified with Mobile +91 ${user.phone}${user.maskedAadhar ? ` & Aadhaar ${user.maskedAadhar}` : ''}. Mandi slot tracking active for ${user.district}.`,
      hindiTitle: `किसान सारथी म.प्र. में स्वागत है, ${user.name}`,
      hindiMessage: `आपका लॉगिन मोबाइल +91 ${user.phone} एवं आधार द्वारा सत्यापित है। ${user.district} हेतु मंडी टोकन शेड्यूलिंग सक्रिय है।`,
      timestamp: new Date().toISOString(),
      read: false,
      senderTag: 'VK-EUPARJAN',
      category: 'QUEUE',
    };
    addNotification(welcomeNotif);
  }, [addNotification]);

  const {
    language,
    handleLanguageChange,
    role,
    handleRoleSwitch,
    currentUser,
    handleLoginSuccess: baseLoginSuccess,
    handleLogout,
    isLoginModalOpen,
  } = useAuthSession({ onLoginSuccessCallback: handleLoginNotification });

  // Booking, Mandi & Sync hook
  const {
    isOnline,
    pendingSyncCount,
    crops,
    mandis,
    bookings,
    weatherAlerts,
    viewingTokenPass,
    setViewingTokenPass,
    viewingJForm,
    setViewingJForm,
    triggerSync,
    refreshUserBookings,
    handleBookSlot,
    handleCallNextToken,
    handleUpdateBookingStatus,
    handleUpdateMsp,
    handleUpdateSlotCapacity,
  } = useBookingFlow({
    currentUser,
    onNotification: addNotification,
  });

  const handleLoginSuccess = useCallback((user: AuthUser) => {
    baseLoginSuccess(user);
    refreshUserBookings(user.phone);
  }, [baseLoginSuccess, refreshUserBookings]);

  // When anyone opens the website there is a login page with mobile number & Aadhaar first
  if (!currentUser) {
    return (
      <LoginPage
        language={language}
        onLanguageChange={handleLanguageChange}
        onLoginSuccess={handleLoginSuccess}
      />
    );
  }

  return (
    <div className="min-h-screen bg-[#F3F6F1] text-slate-800 font-sans flex flex-col selection:bg-[#D4E09B] selection:text-[#1B4332]">
      {/* Header */}
      <Header
        language={language}
        onLanguageChange={handleLanguageChange}
        role={role}
        onRoleSwitch={handleRoleSwitch}
        isOnline={isOnline}
        pendingSyncCount={pendingSyncCount}
        onManualSync={triggerSync}
        unreadNotifsCount={unreadCount}
        onOpenNotifications={() => setIsNotifDrawerOpen(true)}
        currentUser={currentUser}
        onLogout={handleLogout}
      />

      {/* Prototype Notice Banner */}
      <SyntheticDataBanner language={language} />

      {/* Main App Canvas */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {role === 'FARMER' ? (
          <FarmerDashboard
            language={language}
            crops={crops}
            mandis={mandis}
            activeBookings={bookings}
            onBookSlot={handleBookSlot}
            onViewTokenPass={(b) => setViewingTokenPass(b)}
            onViewJForm={(b) => setViewingJForm(b)}
            weatherAlerts={weatherAlerts}
            currentUser={currentUser}
            isOnline={isOnline}
          />
        ) : (
          <AdminDashboard
            language={language}
            mandis={mandis}
            crops={crops}
            bookings={bookings}
            onCallNextToken={handleCallNextToken}
            onUpdateBookingStatus={handleUpdateBookingStatus}
            onUpdateMsp={handleUpdateMsp}
            onUpdateSlotCapacity={handleUpdateSlotCapacity}
            onViewJForm={(b) => setViewingJForm(b)}
            currentUser={currentUser}
          />
        )}
      </main>

      {/* Footnote & Government Helpline Bar */}
      <footer className="bg-white text-slate-600 text-xs border-t border-slate-200 mt-12 py-8 shadow-xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 pb-6 border-b border-slate-200">
            <div>
              <div className="flex items-center gap-2 text-[#1B4332] font-bold text-sm mb-2">
                <div className="w-6 h-6 rounded bg-[#2D6A4F] text-white flex items-center justify-center shadow-xs">
                  <Tractor className="w-3.5 h-3.5 text-white" />
                </div>
                <span>KisanSarthi MP • किसान सारथी म.प्र.</span>
              </div>
              <p className="text-slate-500 text-[11px] leading-relaxed">
                Empowering farmers across Madhya Pradesh with real-time APMC Mandi queue visibility, AI-optimized slot scheduling, moisture testing transparency, and direct DBT payouts.
              </p>
            </div>

            <div>
              <p className="font-bold text-slate-400 uppercase text-[10px] tracking-widest mb-2.5">
                Kisan Helpline & Emergency Support
              </p>
              <ul className="space-y-1.5 text-[11px]">
                <li className="flex items-center gap-1.5">
                  <Phone className="w-3.5 h-3.5 text-[#2D6A4F]" />
                  <span>Kisan Call Center: <strong className="text-slate-800">1800-180-1551</strong> (Toll Free)</span>
                </li>
                <li className="flex items-center gap-1.5">
                  <Phone className="w-3.5 h-3.5 text-[#2D6A4F]" />
                  <span>MP e-Uparjan Helpdesk: <strong className="text-slate-800">0755-2551477</strong></span>
                </li>
                <li className="flex items-center gap-1.5">
                  <Shield className="w-3.5 h-3.5 text-[#2D6A4F]" />
                  <span>CM Helpline: <strong className="text-slate-800">181</strong> (24x7 Farmer Grievance)</span>
                </li>
              </ul>
            </div>

            <div>
              <p className="font-bold text-slate-400 uppercase text-[10px] tracking-widest mb-2.5">
                APMC Statutory Mandates
              </p>
              <p className="text-[11px] text-slate-500 leading-relaxed">
                Operating under the Madhya Pradesh Krishi Upaj Mandi Adhiniyam, 1972. Real-time slot allocation ensures minimum waiting times and elimination of middlemen in procurement.
              </p>
            </div>
          </div>

          <div className="pt-4 flex flex-col sm:flex-row items-center justify-between gap-2 text-[10px] text-slate-400 font-medium">
            <div className="flex items-center gap-4">
              <span>Server Status: <strong className="text-[#2D6A4F]">Online</strong></span>
              <span>Database: <strong className="text-[#2D6A4F]">Cloud-Synced</strong></span>
              <span>Last Update: 2 mins ago</span>
            </div>
            <div>
              &copy; 2026 Madhya Pradesh State Agricultural Marketing Board (MPSAMB)
            </div>
          </div>
        </div>
      </footer>

      {/* Modals & Overlays */}
      <LoginModal
        isOpen={isLoginModalOpen}
        language={language}
        onLoginSuccess={handleLoginSuccess}
      />

      <NotificationDrawer
        isOpen={isNotifDrawerOpen}
        onClose={() => setIsNotifDrawerOpen(false)}
        notifications={notifications}
        onMarkAllRead={handleMarkAllRead}
        onSendTestSms={handleSendTestSms}
        language={language}
      />

      <TokenPassModal
        booking={viewingTokenPass}
        isOpen={!!viewingTokenPass}
        onClose={() => setViewingTokenPass(null)}
        language={language}
      />

      <JFormReceiptModal
        booking={viewingJForm}
        isOpen={!!viewingJForm}
        onClose={() => setViewingJForm(null)}
        language={language}
      />
    </div>
  );
}

export default App;
