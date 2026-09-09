/**
 * IndexedDB Offline Operations Queue
 * Conforms to offline-first requirements:
 * - Temporary client-generated operation IDs (never fake official MP tokens)
 * - Individual operation tracking (PENDING, SYNCING, SYNCED, FAILED, RETRY)
 * - Retries with exponential backoff on network failures
 * - Preserves failed records without blind clearing
 */

export type OperationStatus = 'PENDING' | 'SYNCING' | 'SYNCED' | 'FAILED' | 'RETRY';

export interface PendingBookingOperation {
  id: string; // Temporary client operation ID (e.g., temp-op-17258...)
  idempotencyKey: string; // Concurrency-safe UUID / nano key sent to server
  type: 'CREATE_BOOKING';
  payload: any;
  status: OperationStatus;
  retryCount: number;
  maxRetries: number;
  lastError?: string;
  createdAt: string;
  updatedAt: string;
  syncedBooking?: any;
}

const DB_NAME = 'KisanSarthiOfflineDB';
const DB_VERSION = 1;
const STORE_NAME = 'pending_operations';

function openDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof window === 'undefined' || !window.indexedDB) {
      reject(new Error('IndexedDB is not supported in this environment'));
      return;
    }

    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        const store = db.createObjectStore(STORE_NAME, { keyPath: 'id' });
        store.createIndex('status', 'status', { unique: false });
        store.createIndex('createdAt', 'createdAt', { unique: false });
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

// Fallback to localStorage if IndexedDB is blocked
const LS_FALLBACK_KEY = 'kisansarthi_offline_ops';

function getLocalStorageOps(): PendingBookingOperation[] {
  try {
    const raw = localStorage.getItem(LS_FALLBACK_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveLocalStorageOps(ops: PendingBookingOperation[]): void {
  try {
    localStorage.setItem(LS_FALLBACK_KEY, JSON.stringify(ops));
  } catch (e) {
    console.warn('LocalStorage save error:', e);
  }
}

export const offlineQueue = {
  async addOperation(payload: any): Promise<PendingBookingOperation> {
    const opId = `temp-op-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`;
    const idempotencyKey = `idemp-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

    const op: PendingBookingOperation = {
      id: opId,
      idempotencyKey,
      type: 'CREATE_BOOKING',
      payload,
      status: 'PENDING',
      retryCount: 0,
      maxRetries: 5,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    try {
      const db = await openDatabase();
      await new Promise<void>((resolve, reject) => {
        const tx = db.transaction(STORE_NAME, 'readwrite');
        const store = tx.objectStore(STORE_NAME);
        const req = store.add(op);
        req.onsuccess = () => resolve();
        req.onerror = () => reject(req.error);
      });
    } catch {
      // Fallback
      const list = getLocalStorageOps();
      list.push(op);
      saveLocalStorageOps(list);
    }

    return op;
  },

  async getAllOperations(): Promise<PendingBookingOperation[]> {
    try {
      const db = await openDatabase();
      return await new Promise<PendingBookingOperation[]>((resolve, reject) => {
        const tx = db.transaction(STORE_NAME, 'readonly');
        const store = tx.objectStore(STORE_NAME);
        const req = store.getAll();
        req.onsuccess = () => resolve(req.result || []);
        req.onerror = () => reject(req.error);
      });
    } catch {
      return getLocalStorageOps();
    }
  },

  async getPendingOperations(): Promise<PendingBookingOperation[]> {
    const all = await this.getAllOperations();
    return all.filter((o) => o.status === 'PENDING' || o.status === 'RETRY');
  },

  async updateOperation(
    id: string,
    updates: Partial<PendingBookingOperation>
  ): Promise<void> {
    try {
      const db = await openDatabase();
      await new Promise<void>((resolve, reject) => {
        const tx = db.transaction(STORE_NAME, 'readwrite');
        const store = tx.objectStore(STORE_NAME);
        const getReq = store.get(id);

        getReq.onsuccess = () => {
          const current = getReq.result;
          if (current) {
            const updated = {
              ...current,
              ...updates,
              updatedAt: new Date().toISOString(),
            };
            store.put(updated);
            resolve();
          } else {
            resolve();
          }
        };
        getReq.onerror = () => reject(getReq.error);
      });
    } catch {
      const list = getLocalStorageOps();
      const updated = list.map((item) =>
        item.id === id
          ? { ...item, ...updates, updatedAt: new Date().toISOString() }
          : item
      );
      saveLocalStorageOps(updated);
    }
  },

  async deleteOperation(id: string): Promise<void> {
    try {
      const db = await openDatabase();
      await new Promise<void>((resolve, reject) => {
        const tx = db.transaction(STORE_NAME, 'readwrite');
        const store = tx.objectStore(STORE_NAME);
        const req = store.delete(id);
        req.onsuccess = () => resolve();
        req.onerror = () => reject(req.error);
      });
    } catch {
      const list = getLocalStorageOps().filter((item) => item.id !== id);
      saveLocalStorageOps(list);
    }
  },

  async clearSynced(): Promise<void> {
    const all = await this.getAllOperations();
    for (const op of all) {
      if (op.status === 'SYNCED') {
        await this.deleteOperation(op.id);
      }
    }
  },
};

// Convenience named exports matching consumer usage
export async function enqueueBookingOperation(
  payload: any,
  idempotencyKey?: string
): Promise<PendingBookingOperation> {
  const op = await offlineQueue.addOperation(payload);
  if (idempotencyKey) {
    op.idempotencyKey = idempotencyKey;
    await offlineQueue.updateOperation(op.id, { idempotencyKey });
  }
  return op;
}

export async function getPendingOperations(): Promise<PendingBookingOperation[]> {
  return offlineQueue.getAllOperations();
}

export async function updateOperationStatus(
  id: string,
  status: OperationStatus,
  lastError?: string,
  syncedBooking?: any
): Promise<void> {
  const updates: Partial<PendingBookingOperation> = {
    status,
    updatedAt: new Date().toISOString(),
  };
  if (lastError !== undefined) updates.lastError = lastError;
  if (syncedBooking !== undefined) updates.syncedBooking = syncedBooking;
  if (status === 'RETRY' || status === 'FAILED') {
    const all = await offlineQueue.getAllOperations();
    const current = all.find((o) => o.id === id);
    if (current) {
      updates.retryCount = current.retryCount + 1;
    }
  }
  await offlineQueue.updateOperation(id, updates);
}

export async function clearSyncedOperations(): Promise<void> {
  await offlineQueue.clearSynced();
}
