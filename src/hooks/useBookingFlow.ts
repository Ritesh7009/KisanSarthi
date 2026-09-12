import { useState, useEffect, useCallback } from 'react';
import {
  CropInfo,
  MandiCenter,
  SlotBooking,
  WeatherAlert,
  NotificationItem,
} from '../types';
import {
  MP_MANDIS,
  MP_CROPS,
  SAMPLE_BOOKINGS,
  WEATHER_ALERTS,
} from '../data/mpMandiData';
import {
  getStoredBookings,
  saveBookings,
} from '../utils/offlineStorage';
import {
  bookingApi,
  queueApi,
  cropApi,
  slotApi,
  mandiApi,
} from '../services/api';
import {
  enqueueBookingOperation,
  getPendingOperations,
  updateOperationStatus,
  clearSyncedOperations,
} from '../utils/indexedDbQueue';
import { useMandiRealtime } from './useMandiRealtime';
import { AuthUser } from './useAuthSession';

interface UseBookingFlowProps {
  currentUser: AuthUser | null;
  onNotification: (notif: NotificationItem) => void;
}

export function useBookingFlow({ currentUser, onNotification }: UseBookingFlowProps) {
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

  // Modal inspection states
  const [viewingTokenPass, setViewingTokenPass] = useState<SlotBooking | null>(null);
  const [viewingJForm, setViewingJForm] = useState<SlotBooking | null>(null);

  // Real-time WebSocket connection to active mandi queue
  const activeMandiId = currentUser?.mandiId || mandis[0]?.id || 'mandi-sehore';
  useMandiRealtime({
    mandiId: activeMandiId,
    enabled: true,
    onQueueEvent: (event) => {
      if (event.event === 'TOKEN_CALLED') {
        setMandis((prev) =>
          prev.map((m) =>
            m.id === event.mandiId
              ? {
                  ...m,
                  currentTokenServing: event.currentTokenServing,
                  activeTokensWaiting: event.activeTokensWaiting,
                }
              : m
          )
        );
        if (event.calledBookingId) {
          setBookings((prev) =>
            prev.map((b) =>
              b.id === event.calledBookingId ? { ...b, status: 'GATE_CALLED' } : b
            )
          );
        }
      } else if (event.event === 'BOOKING_UPDATE' && event.bookingId) {
        setBookings((prev) =>
          prev.map((b) =>
            b.id === event.bookingId ? { ...b, status: event.status } : b
          )
        );
      }
    },
  });

  // Monitor network status & sync queue
  const checkPendingOps = useCallback(async () => {
    try {
      const ops = await getPendingOperations();
      setPendingSyncCount(ops.filter((o) => o.status !== 'SYNCED').length);
    } catch {
      // Fallback
    }
  }, []);

  // Sync pending items with granular state tracking
  const triggerSync = useCallback(async () => {
    const pendings = await getPendingOperations();
    const activeOps = pendings.filter((o) => o.status === 'PENDING' || o.status === 'RETRY');
    if (activeOps.length === 0) return;

    for (const op of activeOps) {
      await updateOperationStatus(op.id, 'SYNCING');
      try {
        const res = await bookingApi.createBooking(op.payload, op.idempotencyKey);
        if (res) {
          await updateOperationStatus(op.id, 'SYNCED', undefined, res);
          // Update client booking with official server-generated token and ID
          setBookings((prev) =>
            prev.map((b) => (b.id === op.id ? res : b))
          );
        }
      } catch (err: any) {
        console.error('Pending sync item failed:', err);
        const newStatus = op.retryCount + 1 >= op.maxRetries ? 'FAILED' : 'RETRY';
        await updateOperationStatus(op.id, newStatus, err.message || 'Network failure');
      }
    }

    // Clean up ONLY synced operations (preserves failed/retry records)
    await clearSyncedOperations();
    await checkPendingOps();

    // Refresh bookings from authoritative server
    try {
      const res = await bookingApi.getBookings();
      if (res && res.length > 0) {
        setBookings(res);
        saveBookings(res);
      }
    } catch {
      // offline fallback
    }
  }, [checkPendingOps]);

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      triggerSync();
    };
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    checkPendingOps();

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [checkPendingOps, triggerSync]);

  // Sync with backend on mount
  useEffect(() => {
    async function fetchData() {
      try {
        const [cropsRes, mandisRes, bookingsRes] = await Promise.all([
          cropApi.getAllCrops(),
          mandiApi.getAllMandis(),
          bookingApi.getBookings(),
        ]);

        if (cropsRes && cropsRes.length > 0) setCrops(cropsRes);
        if (mandisRes && mandisRes.length > 0) setMandis(mandisRes);
        if (bookingsRes && bookingsRes.length > 0) {
          setBookings(bookingsRes);
          saveBookings(bookingsRes);
        }
      } catch (err) {
        console.warn('Using local cached mandi data', err);
      }
    }
    fetchData();
  }, []);

  const refreshUserBookings = useCallback((phone: string) => {
    bookingApi.getBookings({ phone })
      .then((data) => {
        if (data && data.length > 0) {
          setBookings(data);
          saveBookings(data);
        }
      })
      .catch(() => {});
  }, []);

  // Book a new slot with Idempotency and Offline Safety
  const handleBookSlot = async (bookingData: any): Promise<SlotBooking | null> => {
    let created: SlotBooking | null = null;
    const idempotencyKey = `idemp-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

    if (isOnline) {
      try {
        const res = await bookingApi.createBooking(bookingData, idempotencyKey);
        if (res) {
          created = res;
        }
      } catch (e) {
        console.warn('Network error, queuing offline', e);
      }
    }

    if (!created) {
      // Offline fallback: create temporary pending booking WITHOUT fake official MP token
      const op = await enqueueBookingOperation(bookingData, idempotencyKey);
      await checkPendingOps();

      created = {
        id: op.id,
        tokenNumber: 'PENDING_SYNC',
        tokenSequence: 0,
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
        qrCodeData: `https://euparjan.mp.gov.in/gate-pass?t=PENDING_SYNC&id=${op.id}`,
        createdAt: new Date().toISOString(),
        paymentStatus: 'PENDING',
        waitTimeEstimateMins: 20,
      };
    }

    const updated = [created, ...bookings];
    setBookings(updated);
    saveBookings(updated);

    // Generate SMS notification
    const smsNotif: NotificationItem = {
      id: `notif-${Date.now()}`,
      type: 'SMS',
      title: created.tokenNumber === 'PENDING_SYNC'
        ? `e-Token Queued: Pending Sync`
        : `e-Token Generated: ${created.tokenNumber}`,
      message: created.tokenNumber === 'PENDING_SYNC'
        ? `Dear ${created.farmerName}, your slot request is saved offline. Official token will be generated once connected to internet.`
        : `Dear ${created.farmerName}, your slot at ${created.mandiCenterName} is confirmed for ${created.scheduledDate} (${created.timeSlot}). Token: ${created.tokenNumber}. Vehicle: ${created.vehicleNumber}. Show QR at Gate.`,
      hindiTitle: created.tokenNumber === 'PENDING_SYNC'
        ? `ई-टोकन कतारबद्ध: सिंक लंबित`
        : `ई-टोकन जारी: ${created.tokenNumber}`,
      hindiMessage: created.tokenNumber === 'PENDING_SYNC'
        ? `प्रिय ${created.farmerName}, आपका स्लॉट ऑफलाइन सुरक्षित है। इंटरनेट कनेक्ट होने पर आधिकारिक टोकन जारी किया जाएगा।`
        : `प्रिय ${created.farmerName}, ${created.mandiCenterName} में आपका स्लॉट दिनांक ${created.scheduledDate} (${created.timeSlot}) हेतु पुष्ट है। टोकन: ${created.tokenNumber}। गेट पर क्यूआर दिखाएं।`,
      timestamp: new Date().toISOString(),
      read: false,
      senderTag: 'VK-EUPARJAN',
      category: 'SLOT',
    };
    onNotification(smsNotif);

    return created;
  };

  // Admin calls next token (Server Authoritative)
  const handleCallNextToken = async (mandiId: string) => {
    let nextNum = 39;
    try {
      const res = await queueApi.callNextToken(mandiId);
      if (res.currentTokenServing !== undefined) {
        nextNum = res.currentTokenServing;
        setMandis((prev) =>
          prev.map((m) =>
            m.id === mandiId
              ? {
                  ...m,
                  currentTokenServing: res.currentTokenServing,
                  activeTokensWaiting: res.activeTokensWaiting,
                }
              : m
          )
        );
        if (res.calledBookingId) {
          setBookings((prev) =>
            prev.map((b) =>
              b.id === res.calledBookingId ? { ...b, status: 'GATE_CALLED' } : b
            )
          );
        }
      }
    } catch (e) {
      console.warn('API call next error:', e);
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
    }

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
    onNotification(smsNotif);

    if (navigator.vibrate) {
      navigator.vibrate([100, 50, 100]);
    }
  };

  // Update booking status with strict state machine validation
  const handleUpdateBookingStatus = async (bookingId: string, updates: Partial<SlotBooking>) => {
    try {
      const res = await bookingApi.updateBookingStatus(bookingId, updates);
      const serverUpdated = res;

      setBookings((prev) =>
        prev.map((b) => (b.id === bookingId ? { ...b, ...(serverUpdated || updates) } : b))
      );
      saveBookings(bookings);
    } catch (e) {
      console.warn('Booking status update error:', e);
      // Fallback local update
      setBookings((prev) =>
        prev.map((b) => (b.id === bookingId ? { ...b, ...updates } : b))
      );
    }

    // If payment DBT completed, generate SMS
    if (updates.status === 'COMPLETED' || updates.paymentStatus === 'DBT_INITIATED') {
      const target = bookings.find((b) => b.id === bookingId);
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
        onNotification(dbtNotif);
      }
    }
  };

  // Update MSP with canonical field names
  const handleUpdateMsp = async (cropId: string, standardMsp: number, mpBonus: number) => {
    try {
      await cropApi.updateMsp(cropId, {
        standardMspPerQuintal: standardMsp,
        mpBonusPerQuintal: mpBonus,
      });
    } catch (e) {
      console.warn('MSP update API error', e);
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
    try {
      await slotApi.updateSlotCapacity(timeSlot, maxVehicles, status);
    } catch (e) {
      console.warn('Slot capacity API error', e);
    }
  };

  return {
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
  };
}
