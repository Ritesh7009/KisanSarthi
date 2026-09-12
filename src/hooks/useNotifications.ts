import { useState, useCallback } from 'react';
import { NotificationItem } from '../types';
import { INITIAL_NOTIFICATIONS } from '../data/mpMandiData';
import { getStoredNotifications, saveNotifications } from '../utils/offlineStorage';

export function useNotifications() {
  const [notifications, setNotifications] = useState<NotificationItem[]>(() => {
    const stored = getStoredNotifications();
    return stored.length > 0 ? stored : INITIAL_NOTIFICATIONS;
  });
  const [isNotifDrawerOpen, setIsNotifDrawerOpen] = useState(false);

  const addNotification = useCallback((notif: NotificationItem) => {
    setNotifications((prev) => {
      const updated = [notif, ...prev];
      saveNotifications(updated);
      return updated;
    });
  }, []);

  const handleMarkAllRead = useCallback(() => {
    setNotifications((prev) => {
      const updated = prev.map((n) => ({ ...n, read: true }));
      saveNotifications(updated);
      return updated;
    });
  }, []);

  const handleSendTestSms = useCallback((type: 'SLOT' | 'QUEUE' | 'PAYMENT' | 'WEATHER') => {
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

    addNotification(item);
  }, [addNotification]);

  const unreadCount = notifications.filter((n) => !n.read).length;

  return {
    notifications,
    isNotifDrawerOpen,
    setIsNotifDrawerOpen,
    addNotification,
    handleMarkAllRead,
    handleSendTestSms,
    unreadCount,
  };
}
