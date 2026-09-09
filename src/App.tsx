import React, { useState, useEffect, useCallback } from 'react';
import {
  Language,
  UserRole,
  CropInfo,
  MandiCenter,
  SlotBooking,
  WeatherAlert,
  NotificationItem,
} from './types';
import {
  MP_MANDIS,
  MP_CROPS,
  SAMPLE_BOOKINGS,
  WEATHER_ALERTS,
  INITIAL_NOTIFICATIONS,
} from './data/mpMandiData';
import {
  getStoredBookings,
  saveBookings,
  getStoredNotifications,
  saveNotifications,
  getPendingBookings,
  savePendingBooking,
  clearPendingBookings,
  getStoredLanguage,
  setStoredLanguage,
  getStoredUser,
  setStoredUser,
} from './utils/offlineStorage';
import { translations } from './i18n/translations';
import { Header } from './components/Header';
import { SyntheticDataBanner } from './components/SyntheticDataBanner';
import { FarmerDashboard } from './components/FarmerDashboard';
import { AdminDashboard } from './components/AdminDashboard';
import { LoginModal } from './components/LoginModal';
import { LoginPage } from './components/LoginPage';
import { NotificationDrawer } from './components/NotificationDrawer';
import { TokenPassModal } from './components/TokenPassModal';
import { JFormReceiptModal } from './components/JFormReceiptModal';
import { Phone, Shield, Tractor, Info, ExternalLink } from 'lucide-react';

export function App() {
  // App State
  const [language, setLanguage] = useState<Language>(() => getStoredLanguage());
  const [role, setRole] = useState<UserRole>('FARMER');
  const [currentUser, setCurrentUser] = useState<{
    name: string;
    phone: string;
    aadharNumber?: string;
    maskedAadhar?: string;
    district: string;
    village?: string;
    role: UserRole;
    mandiId?: string;
  } | null>(null);

  const [isOnline, setIsOnline] = useState<boolean>(navigator.onLine);
  const [pendingSyncCount, setPendingSyncCount] = useState<number>(0);

  // Core Data
  const [crops, setCrops] = useState<CropInfo[]>(MP_CROPS);
  const [mandis, setMandis] = useState<MandiCenter[]>(MP_MANDIS);
  const [bookings, setBookings] = useState<SlotBooking[]>(() => {
    const stored = getStoredBookings();
    return stored.length > 0 ? stored : SAMPLE_BOOKINGS;
  });
  const [weatherAlerts] = useState<WeatherAlert[]>(WEATHER_ALERTS);
  const [notifications, setNotifications] = useState<NotificationItem[]>(() => {
    const stored = getStoredNotifications();
    return stored.length > 0 ? stored : INITIAL_NOTIFICATIONS;
  });

  // Modal & Drawer visibility
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [isNotifDrawerOpen, setIsNotifDrawerOpen] = useState(false);
  const [viewingTokenPass, setViewingTokenPass] = useState<SlotBooking | null>(null);
  const [viewingJForm, setViewingJForm] = useState<SlotBooking | null>(null);

  // Monitor network status
  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      triggerSync();
    };
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Initial pending count
    const pendings = getPendingBookings();
    setPendingSyncCount(pendings.length);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // Sync with backend on mount
  useEffect(() => {
    async function fetchData() {
      try {
        const [cropsRes, mandisRes, bookingsRes] = await Promise.all([
          fetch('/api/crops'),
          fetch('/api/mandis'),
          fetch('/api/bookings'),
        ]);

        if (cropsRes.ok) {
          const cropsData = await cropsRes.json();
          if (cropsData.crops) setCrops(cropsData.crops);
        }
        if (mandisRes.ok) {
          const mandisData = await mandisRes.json();
          if (mandisData.mandis) setMandis(mandisData.mandis);
        }
        if (bookingsRes.ok) {
          const bookingsData = await bookingsRes.json();
          if (bookingsData.bookings && bookingsData.bookings.length > 0) {
            setBookings(bookingsData.bookings);
            saveBookings(bookingsData.bookings);
          }
        }
      } catch (err) {
        console.warn('Using local cached mandi data', err);
      }
    }
    fetchData();
  }, []);

  // Sync pending items
  const triggerSync = useCallback(async () => {
    const pendings = getPendingBookings();
    if (pendings.length === 0) return;

    for (const item of pendings) {
      try {
        await fetch('/api/bookings', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(item),
        });
      } catch (e) {
        console.error('Pending sync item failed', e);
      }
    }

    clearPendingBookings();
    setPendingSyncCount(0);

    // Refresh bookings
    try {
      const res = await fetch('/api/bookings');
      if (res.ok) {
        const data = await res.json();
        if (data.bookings) {
          setBookings(data.bookings);
          saveBookings(data.bookings);
        }
      }
    } catch (e) {
      // offline fallback
    }
  }, []);

  // Update language
  const handleLanguageChange = (lang: Language) => {
    setLanguage(lang);
    setStoredLanguage(lang);
  };

  // Switch role
  const handleRoleSwitch = (newRole: UserRole) => {
    setRole(newRole);
    if (currentUser) {
      const updated = { ...currentUser, role: newRole };
      setCurrentUser(updated);
      setStoredUser(updated);
    }
  };

  // Login handler
  const handleLoginSuccess = (user: {
    name: string;
    phone: string;
    aadharNumber?: string;
    maskedAadhar?: string;
    district: string;
    village?: string;
    role: UserRole;
    mandiId?: string;
  }) => {
    setCurrentUser(user);
    setRole(user.role);
    setStoredUser(user);
    setIsLoginModalOpen(false);

    // Refresh bookings from server
    fetch('/api/bookings')
      .then((res) => res.json())
      .then((data) => {
        if (data.bookings) {
          setBookings(data.bookings);
          saveBookings(data.bookings);
        }
      })
      .catch(() => {});

    // Push welcome SMS
    const welcomeNotif: NotificationItem = {
      id: `notif-${Date.now()}`,
      type: 'SMS',
      title: `Welcome to KisanSetu MP, ${user.name}`,
      message: `Your login is verified with Mobile +91 ${user.phone}${user.maskedAadhar ? ` & Aadhaar ${user.maskedAadhar}` : ''}. Mandi slot tracking active for ${user.district}.`,
      hindiTitle: `किसान सेतु म.प्र. में स्वागत है, ${user.name}`,
      hindiMessage: `आपका लॉगिन मोबाइल +91 ${user.phone} एवं आधार द्वारा सत्यापित है। ${user.district} हेतु मंडी टोकन शेड्यूलिंग सक्रिय है।`,
      timestamp: new Date().toISOString(),
      read: false,
      senderTag: 'VK-EUPARJAN',
      category: 'QUEUE',
    };
    const updated = [welcomeNotif, ...notifications];
    setNotifications(updated);
    saveNotifications(updated);
  };

  const handleLogout = () => {
    setCurrentUser(null);
    setStoredUser(null);
  };

  // Book a new slot
  const handleBookSlot = async (bookingData: any): Promise<SlotBooking | null> => {
    let created: SlotBooking | null = null;

    if (isOnline) {
      try {
        const res = await fetch('/api/bookings', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(bookingData),
        });
        const data = await res.json();
        if (data.success && data.booking) {
          created = data.booking;
        }
      } catch (e) {
        console.warn('Network error, saving offline', e);
      }
    }

    if (!created) {
      // Offline fallback creation
      const seq = bookings.length + 42;
      const numStr = String(seq).padStart(3, '0');
      created = {
        id: `booking-${Date.now()}`,
        tokenNumber: `MP-${bookingData.district.slice(0, 3).toUpperCase()}-${numStr}`,
        tokenSequence: seq,
        farmerId: bookingData.farmerId || 'farmer-01',
        farmerName: bookingData.farmerName,
        farmerPhone: bookingData.farmerPhone,
        district: bookingData.district,
        village: bookingData.village || 'Demo Village',
        mandiCenterId: bookingData.mandiCenterId,
        mandiCenterName: bookingData.mandiCenterName,
        cropId: bookingData.cropId,
        cropName: bookingData.cropName,
        estimatedYieldQuintals: bookingData.estimatedYieldQuintals,
        acreage: bookingData.acreage,
        harvestDate: bookingData.harvestDate,
        scheduledDate: bookingData.scheduledDate,
        timeSlot: bookingData.timeSlot,
        vehicleType: bookingData.vehicleType,
        vehicleNumber: bookingData.vehicleNumber,
        status: 'BOOKED',
        qrCodeData: `https://euparjan.mp.gov.in/gate-pass?t=MP-${bookingData.district.slice(0, 3).toUpperCase()}-${numStr}`,
        createdAt: new Date().toISOString(),
        paymentStatus: 'PENDING',
        waitTimeEstimateMins: 20,
      };

      savePendingBooking(bookingData);
      setPendingSyncCount((c) => c + 1);
    }

    const updated = [created, ...bookings];
    setBookings(updated);
    saveBookings(updated);

    // Generate SMS notification
    const smsNotif: NotificationItem = {
      id: `notif-${Date.now()}`,
      type: 'SMS',
      title: `e-Token Generated: ${created.tokenNumber}`,
      message: `Dear ${created.farmerName}, your slot at ${created.mandiCenterName} is confirmed for ${created.scheduledDate} (${created.timeSlot}). Token: ${created.tokenNumber}. Vehicle: ${created.vehicleNumber}. Show QR at Gate.`,
      hindiTitle: `ई-टोकन जारी: ${created.tokenNumber}`,
      hindiMessage: `प्रिय ${created.farmerName}, ${created.mandiCenterName} में आपका स्लॉट दिनांक ${created.scheduledDate} (${created.timeSlot}) हेतु पुष्ट है। टोकन: ${created.tokenNumber}। गेट पर क्यूआर दिखाएं।`,
      timestamp: new Date().toISOString(),
      read: false,
      senderTag: 'VK-EUPARJAN',
      category: 'SLOT',
    };
    const updatedNotifs = [smsNotif, ...notifications];
    setNotifications(updatedNotifs);
    saveNotifications(updatedNotifs);

    return created;
  };

  // Admin calls next token
  const handleCallNextToken = async (mandiId: string) => {
    let nextNum = 39;
    if (isOnline) {
      try {
        const res = await fetch(`/api/mandis/${mandiId}/call-next`, {
          method: 'POST',
        });
        const data = await res.json();
        if (data.success && data.currentTokenServing) {
          nextNum = data.currentTokenServing;
        }
      } catch (e) {
        console.warn('API call next error', e);
      }
    }

    setMandis((prev) =>
      prev.map((m) =>
        m.id === mandiId
          ? {
              ...m,
              currentTokenServing: m.currentTokenServing + 1,
              activeTokensWaiting: Math.max(0, m.activeTokensWaiting - 1),
            }
          : m
      )
    );

    // Trigger SMS to farmer
    const smsNotif: NotificationItem = {
      id: `notif-${Date.now()}`,
      type: 'SMS',
      title: `📢 Gate Call: Token #${nextNum} Please Enter Gate #2`,
      message: `ATTENTION: Token #${nextNum} is called to Weighbridge Bay 1 at Sehore Krishi Mandi. Please proceed immediately with tractor trolley.`,
      hindiTitle: `📢 मंडी गेट बुलावा: टोकन #${nextNum} कृपया गेट 2 पर आएं`,
      hindiMessage: `सूचना: टोकन #${nextNum} को तौल कांटा 1 पर बुलाया गया है। कृपया अपने ट्रैक्टर के साथ तुरंत गेट 2 से प्रवेश करें।`,
      timestamp: new Date().toISOString(),
      read: false,
      senderTag: 'MD-KMSGOV',
      category: 'QUEUE',
    };
    const updatedNotifs = [smsNotif, ...notifications];
    setNotifications(updatedNotifs);
    saveNotifications(updatedNotifs);

    // Play subtle audio / vibrate feedback
    if (navigator.vibrate) {
      navigator.vibrate([100, 50, 100]);
    }
  };

  // Update booking status
  const handleUpdateBookingStatus = async (bookingId: string, updates: Partial<SlotBooking>) => {
    if (isOnline) {
      try {
        await fetch(`/api/bookings/${bookingId}`, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(updates),
        });
      } catch (e) {
        console.warn('Booking patch error', e);
      }
    }

    const updated = bookings.map((b) => (b.id === bookingId ? { ...b, ...updates } : b));
    setBookings(updated);
    saveBookings(updated);

    // If payment DBT completed, generate SMS
    if (updates.status === 'COMPLETED' || updates.paymentStatus === 'DBT_INITIATED') {
      const target = updated.find((b) => b.id === bookingId);
      if (target) {
        const dbtNotif: NotificationItem = {
          id: `notif-${Date.now()}`,
          type: 'SMS',
          title: `DBT Payment Initiated: ₹${(target.totalPayoutRs || 156000).toLocaleString('en-IN')}`,
          message: `MP e-Uparjan: ₹${(target.totalPayoutRs || 156000).toLocaleString('en-IN')} has been transferred to your Aadhaar-linked Bank A/C ending ****4589 for Token ${target.tokenNumber}. UTR: ${target.utrNumber || 'MPDBT99214'}.`,
          hindiTitle: `डीबीटी भुगतान राशि अंतरित: ₹${(target.totalPayoutRs || 156000).toLocaleString('en-IN')}`,
          hindiMessage: `म.प्र. ई-उपार्जन: टोकन ${target.tokenNumber} का उपार्जन भुगतान ₹${(target.totalPayoutRs || 156000).toLocaleString('en-IN')} आपके आधार लिंक बैंक खाते में भेज दिया गया है।`,
          timestamp: new Date().toISOString(),
          read: false,
          senderTag: 'VK-EUPARJAN',
          category: 'PAYMENT',
        };
        setNotifications((prev) => [dbtNotif, ...prev]);
      }
    }
  };

  // Update MSP
  const handleUpdateMsp = async (cropId: string, standardMsp: number, mpBonus: number) => {
    if (isOnline) {
      try {
        await fetch(`/api/crops/${cropId}/msp`, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ standardMsp, mpBonus }),
        });
      } catch (e) {
        console.warn('MSP update API error', e);
      }
    }

    setCrops((prev) =>
      prev.map((c) =>
        c.id === cropId
          ? {
              ...c,
              standardMspPerQuintal: standardMsp,
              mpBonusPerQuintal: mpBonus,
              totalMsp: standardMsp + mpBonus,
            }
          : c
      )
    );
  };

  // Update slot capacity
  const handleUpdateSlotCapacity = async (timeSlot: string, maxVehicles: number, status: any) => {
    if (isOnline) {
      try {
        await fetch('/api/slots/capacity', {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ timeSlot, maxVehicles, status }),
        });
      } catch (e) {
        console.warn('Slot capacity API error', e);
      }
    }
  };

  // Simulate custom SMS test
  const handleSendTestSms = (type: 'SLOT' | 'QUEUE' | 'PAYMENT' | 'WEATHER') => {
    const id = `sms-test-${Date.now()}`;
    let item: NotificationItem;

    if (type === 'QUEUE') {
      item = {
        id,
        type: 'SMS',
        title: '📢 Mandi Token Call-In: Token #39',
        message: 'APMC Sehore Alert: Token #39 report to Gate #2 immediately for gross weighbridge inspection.',
        hindiTitle: '📢 टोकन बुलावा: टोकन #39',
        hindiMessage: 'कृषि उपज मंडी सीहोर: टोकन #39 तुरंत गेट 2 पर तौल हेतु उपस्थित हों।',
        timestamp: new Date().toISOString(),
        read: false,
        senderTag: 'MD-KMSGOV',
        category: 'QUEUE',
      };
    } else if (type === 'PAYMENT') {
      item = {
        id,
        type: 'SMS',
        title: '💰 DBT Procurement Credit: ₹1,56,000',
        message: 'Govt of MP: ₹1,56,000 credited to Bank A/C ending ****4589 for Wheat procurement under Token MP-SEH-038. Ref UTR: MPDBT20268841.',
        hindiTitle: '💰 डीबीटी भुगतान राशि जमा: ₹1,56,000',
        hindiMessage: 'म.प्र. शासन: गेहूं उपार्जन टोकन MP-SEH-038 की राशि ₹1,56,000 आपके बैंक खाते में जमा कर दी गई है।',
        timestamp: new Date().toISOString(),
        read: false,
        senderTag: 'VK-EUPARJAN',
        category: 'PAYMENT',
      };
    } else if (type === 'WEATHER') {
      item = {
        id,
        type: 'SMS',
        title: '🌧️ IMD Sehore: Heavy Rain Advisory',
        message: 'Thunderstorm with gusty winds forecast between 3:00 PM - 6:00 PM. Keep tarpaulin ready on trolleys.',
        hindiTitle: '🌧️ मौसम विभाग: वर्षा एवं आंधी चेतावनी',
        hindiMessage: 'दोपहर 3 से 6 बजे के मध्य सीहोर में तेज वर्षा का अनुमान है। ट्रालियों को तिरपाल से ढक कर रखें।',
        timestamp: new Date().toISOString(),
        read: false,
        senderTag: 'IMD-BHOPAL',
        category: 'WEATHER',
      };
    } else {
      item = {
        id,
        type: 'SMS',
        title: '🎟️ Token MP-SEH-045 Confirmed',
        message: 'Your slot at Sehore Mandi is confirmed for tomorrow 08:00 AM - 10:00 AM. Estimated wait: 15 mins.',
        hindiTitle: '🎟️ टोकन MP-SEH-045 स्वीकृत',
        hindiMessage: 'सीहोर मंडी में कल सुबह 8 से 10 बजे का टोकन स्वीकृत है। अनुमानित प्रतीक्षा: 15 मिनट।',
        timestamp: new Date().toISOString(),
        read: false,
        senderTag: 'VK-EUPARJAN',
        category: 'SLOT',
      };
    }

    const updated = [item, ...notifications];
    setNotifications(updated);
    saveNotifications(updated);
  };

  const handleMarkAllRead = () => {
    const updated = notifications.map((n) => ({ ...n, read: true }));
    setNotifications(updated);
    saveNotifications(updated);
  };

  const unreadCount = notifications.filter((n) => !n.read).length;

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
                <span>KisanSetu MP • किसान सेतु म.प्र.</span>
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
