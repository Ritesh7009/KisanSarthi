import { SlotBooking, MandiCenter, CropInfo, NotificationItem } from '../types';
import { INITIAL_BOOKINGS, MP_MANDIS, MP_CROPS } from '../data/mpMandiData';

const STORAGE_KEYS = {
  BOOKINGS: 'kisansetu_bookings',
  MANDIS: 'kisansetu_mandis',
  CROPS: 'kisansetu_crops',
  NOTIFICATIONS: 'kisansetu_notifications',
  OFFLINE_QUEUE: 'kisansetu_offline_queue',
  CURRENT_USER: 'kisansetu_current_user',
  LANGUAGE: 'kisansetu_lang',
};

export interface OfflineAction {
  id: string;
  type: 'CREATE_BOOKING' | 'UPDATE_STATUS' | 'UPDATE_MSP' | 'CALL_TOKEN';
  payload: any;
  timestamp: string;
}

export const offlineStorage = {
  // Bookings
  getBookings(): SlotBooking[] {
    try {
      const stored = localStorage.getItem(STORAGE_KEYS.BOOKINGS);
      if (stored) return JSON.parse(stored);
      // Initialize with default
      localStorage.setItem(STORAGE_KEYS.BOOKINGS, JSON.stringify(INITIAL_BOOKINGS));
      return INITIAL_BOOKINGS;
    } catch {
      return INITIAL_BOOKINGS;
    }
  },

  saveBookings(bookings: SlotBooking[]) {
    try {
      localStorage.setItem(STORAGE_KEYS.BOOKINGS, JSON.stringify(bookings));
    } catch (e) {
      console.warn('Failed to save to localStorage', e);
    }
  },

  addBooking(booking: SlotBooking) {
    const list = this.getBookings();
    const updated = [booking, ...list.filter(b => b.id !== booking.id)];
    this.saveBookings(updated);
    return updated;
  },

  updateBookingStatus(id: string, updates: Partial<SlotBooking>) {
    const list = this.getBookings();
    const updated = list.map(b => (b.id === id ? { ...b, ...updates } : b));
    this.saveBookings(updated);
    return updated;
  },

  // Mandis
  getMandis(): MandiCenter[] {
    try {
      const stored = localStorage.getItem(STORAGE_KEYS.MANDIS);
      if (stored) return JSON.parse(stored);
      localStorage.setItem(STORAGE_KEYS.MANDIS, JSON.stringify(MP_MANDIS));
      return MP_MANDIS;
    } catch {
      return MP_MANDIS;
    }
  },

  saveMandis(mandis: MandiCenter[]) {
    try {
      localStorage.setItem(STORAGE_KEYS.MANDIS, JSON.stringify(mandis));
    } catch (e) {
      console.warn('Failed to save mandis', e);
    }
  },

  // Crops
  getCrops(): CropInfo[] {
    try {
      const stored = localStorage.getItem(STORAGE_KEYS.CROPS);
      if (stored) return JSON.parse(stored);
      localStorage.setItem(STORAGE_KEYS.CROPS, JSON.stringify(MP_CROPS));
      return MP_CROPS;
    } catch {
      return MP_CROPS;
    }
  },

  saveCrops(crops: CropInfo[]) {
    try {
      localStorage.setItem(STORAGE_KEYS.CROPS, JSON.stringify(crops));
    } catch (e) {
      console.warn('Failed to save crops', e);
    }
  },

  // Notifications
  getNotifications(): NotificationItem[] {
    try {
      const stored = localStorage.getItem(STORAGE_KEYS.NOTIFICATIONS);
      if (stored) return JSON.parse(stored);
      return [];
    } catch {
      return [];
    }
  },

  addNotification(notif: NotificationItem) {
    const list = this.getNotifications();
    const updated = [notif, ...list];
    try {
      localStorage.setItem(STORAGE_KEYS.NOTIFICATIONS, JSON.stringify(updated.slice(0, 50)));
    } catch (e) {
      console.warn('Failed to save notification', e);
    }
    return updated;
  },

  // Offline Sync Queue
  getQueue(): OfflineAction[] {
    try {
      const stored = localStorage.getItem(STORAGE_KEYS.OFFLINE_QUEUE);
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  },

  addToQueue(action: OfflineAction) {
    const queue = this.getQueue();
    queue.push(action);
    localStorage.setItem(STORAGE_KEYS.OFFLINE_QUEUE, JSON.stringify(queue));
  },

  clearQueue() {
    localStorage.removeItem(STORAGE_KEYS.OFFLINE_QUEUE);
  },
};

export const getStoredBookings = (): SlotBooking[] => offlineStorage.getBookings();
export const saveBookings = (bookings: SlotBooking[]): void => offlineStorage.saveBookings(bookings);

export const getStoredNotifications = (): NotificationItem[] => offlineStorage.getNotifications();
export const saveNotifications = (notifs: NotificationItem[]): void => {
  try {
    localStorage.setItem(STORAGE_KEYS.NOTIFICATIONS, JSON.stringify(notifs));
  } catch (e) {
    console.warn('Failed to save notifications', e);
  }
};

export const getPendingBookings = (): any[] => {
  try {
    const stored = localStorage.getItem(STORAGE_KEYS.OFFLINE_QUEUE);
    return stored ? JSON.parse(stored) : [];
  } catch {
    return [];
  }
};

export const savePendingBooking = (bookingData: any): void => {
  const q = getPendingBookings();
  q.push(bookingData);
  localStorage.setItem(STORAGE_KEYS.OFFLINE_QUEUE, JSON.stringify(q));
};

export const clearPendingBookings = (): void => {
  localStorage.removeItem(STORAGE_KEYS.OFFLINE_QUEUE);
};

export const getStoredLanguage = (): 'en' | 'hi' | 'mal' => {
  try {
    const l = localStorage.getItem(STORAGE_KEYS.LANGUAGE);
    if (l === 'hi' || l === 'mal' || l === 'en') return l;
    return 'hi'; // Default Hindi for MP farmers
  } catch {
    return 'hi';
  }
};

export const setStoredLanguage = (lang: 'en' | 'hi' | 'mal'): void => {
  try {
    localStorage.setItem(STORAGE_KEYS.LANGUAGE, lang);
  } catch (e) {
    console.warn('Failed to store lang', e);
  }
};

export const getStoredUser = (): any => {
  try {
    const u = localStorage.getItem(STORAGE_KEYS.CURRENT_USER);
    return u ? JSON.parse(u) : null;
  } catch {
    return null;
  }
};

export const setStoredUser = (user: any): void => {
  try {
    if (user) {
      localStorage.setItem(STORAGE_KEYS.CURRENT_USER, JSON.stringify(user));
    } else {
      localStorage.removeItem(STORAGE_KEYS.CURRENT_USER);
    }
  } catch (e) {
    console.warn('Failed to store user', e);
  }
};
