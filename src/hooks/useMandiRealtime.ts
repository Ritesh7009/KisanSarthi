/**
 * useMandiRealtime
 * Production-ready real-time WebSocket hook for APMC Mandi queue tracking.
 * Connects to /ws, subscribes to /topic/mandi/{mandiId}/queue and /topic/mandi/{mandiId}/status,
 * handles automatic reconnect with exponential backoff, resubscribes,
 * and triggers REST state refresh upon reconnection.
 */

import { useEffect, useRef, useState, useCallback } from 'react';
import { QueueCallResult } from '../services/api';

export interface RealtimeQueueEvent {
  event: 'TOKEN_CALLED' | 'GATE_ENTERED' | 'WEIGHMENT_UPDATE' | 'QUEUE_ADVANCE';
  mandiId: string;
  bookingId?: string;
  tokenNumber: string;
  currentToken: number;
  waitingCount: number;
  status: string;
  timestamp: string;
  notes?: string;
}

export interface UseMandiRealtimeOptions {
  mandiId?: string;
  enabled?: boolean;
  onTokenCalled?: (event: RealtimeQueueEvent) => void;
  onQueueEvent?: (event: any) => void;
  onStatusChange?: (status: any) => void;
  onRefreshNeeded?: () => void;
}

export function useMandiRealtime({
  mandiId,
  enabled = true,
  onTokenCalled,
  onQueueEvent,
  onStatusChange,
  onRefreshNeeded,
}: UseMandiRealtimeOptions) {
  const [isConnected, setIsConnected] = useState(false);
  const [reconnectAttempts, setReconnectAttempts] = useState(0);
  const [lastEvent, setLastEvent] = useState<RealtimeQueueEvent | null>(null);

  const socketRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<any>(null);
  const heartbeatIntervalRef = useRef<any>(null);
  const isMountedRef = useRef(true);

  // Keep callbacks fresh in refs to avoid re-triggering connection effect
  const callbacksRef = useRef({ onTokenCalled, onQueueEvent, onStatusChange, onRefreshNeeded });
  callbacksRef.current = { onTokenCalled, onQueueEvent, onStatusChange, onRefreshNeeded };

  const connect = useCallback(() => {
    if (!enabled || !mandiId || typeof window === 'undefined') return;

    // Clean up any existing connection
    if (socketRef.current) {
      socketRef.current.close();
      socketRef.current = null;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws`;

    try {
      const ws = new WebSocket(wsUrl);
      socketRef.current = ws;

      ws.onopen = () => {
        if (!isMountedRef.current) return;
        setIsConnected(true);
        setReconnectAttempts(0);

        // STOMP-compatible / plain WebSocket subscribe message
        const subscribePayload = JSON.stringify({
          action: 'SUBSCRIBE',
          topics: [
            `/topic/mandi/${mandiId}/queue`,
            `/topic/mandi/${mandiId}/status`,
          ],
          mandiId,
        });
        ws.send(subscribePayload);

        // Heartbeat keep-alive every 25 seconds
        if (heartbeatIntervalRef.current) clearInterval(heartbeatIntervalRef.current);
        heartbeatIntervalRef.current = setInterval(() => {
          if (ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ action: 'PING' }));
          }
        }, 25000);

        // Reconnect triggered: refresh REST state from authoritative PostgreSQL server
        if (callbacksRef.current.onRefreshNeeded) {
          callbacksRef.current.onRefreshNeeded();
        }
      };

      ws.onmessage = (messageEvent) => {
        try {
          const data = JSON.parse(messageEvent.data);
          if (data.action === 'PONG') return;

          if (callbacksRef.current.onQueueEvent) {
            callbacksRef.current.onQueueEvent(data);
          }

          if (data.event === 'TOKEN_CALLED' || data.event === 'QUEUE_ADVANCE') {
            const parsedEvent: RealtimeQueueEvent = {
              event: data.event,
              mandiId: data.mandiId || mandiId,
              bookingId: data.bookingId,
              tokenNumber: data.tokenNumber || `Token #${data.currentToken || data.calledToken}`,
              currentToken: data.currentToken ?? data.calledToken ?? 0,
              waitingCount: data.waitingCount ?? data.activeWaiting ?? 0,
              status: data.status || 'GATE_CALLED',
              timestamp: data.timestamp || new Date().toISOString(),
              notes: data.notes,
            };
            setLastEvent(parsedEvent);
            if (callbacksRef.current.onTokenCalled) {
              callbacksRef.current.onTokenCalled(parsedEvent);
            }
          }

          if (data.type === 'MANDI_STATUS' && callbacksRef.current.onStatusChange) {
            callbacksRef.current.onStatusChange(data.status);
          }
        } catch (e) {
          // Non-JSON or debug frame
        }
      };

      ws.onclose = () => {
        if (!isMountedRef.current) return;
        setIsConnected(false);
        if (heartbeatIntervalRef.current) clearInterval(heartbeatIntervalRef.current);

        // Exponential backoff reconnect: 1s, 2s, 4s, capped at 15s
        const backoffMs = Math.min(1000 * Math.pow(1.8, reconnectAttempts), 15000);
        reconnectTimeoutRef.current = setTimeout(() => {
          if (isMountedRef.current) {
            setReconnectAttempts((prev) => prev + 1);
            connect();
          }
        }, backoffMs);
      };

      ws.onerror = (err) => {
        console.warn('WebSocket connection note:', err);
        // Let onclose trigger reconnect
      };
    } catch (e) {
      console.warn('WebSocket initialization error:', e);
    }
  }, [mandiId, reconnectAttempts]);

  useEffect(() => {
    isMountedRef.current = true;
    connect();

    return () => {
      isMountedRef.current = false;
      if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current);
      if (heartbeatIntervalRef.current) clearInterval(heartbeatIntervalRef.current);
      if (socketRef.current) {
        socketRef.current.close();
        socketRef.current = null;
      }
    };
  }, [connect]);

  return {
    isConnected,
    reconnectAttempts,
    lastEvent,
    reconnectManually: connect,
  };
}
