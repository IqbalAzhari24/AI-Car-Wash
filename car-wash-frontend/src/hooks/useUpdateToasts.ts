import { useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from '../context/AuthContext';

export interface UpdateToast {
  id: string;
  kind: 'BOOKING' | 'VALET';
  status: string;
  message: string;
}

// Same STOMP endpoint Timah uses — the broker fans out per-user queues.
const WS_URL =
  new URL(import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api').origin +
  '/ws/timah/websocket';
const MAX_RETRY_MS = 30000;

/**
 * Subscribes to the customer's private STOMP queue `/user/queue/updates` and
 * surfaces booking/valet status changes as auto-dismissing toasts. Also fires
 * a window `app-update` CustomEvent so list pages can refresh themselves.
 */
export function useUpdateToasts() {
  const { token, user } = useAuth();
  const [toasts, setToasts] = useState<UpdateToast[]>([]);
  const retryRef = useRef(1000);

  useEffect(() => {
    // Only customers receive these pushes; staff act, customers get notified.
    if (!token || user?.role !== 'CUSTOMER') return;
    // `alive` is per-effect-invocation (NOT a shared ref): under StrictMode's
    // double-mount a shared ref lets the first socket's onclose see alive=true
    // again and spawn a zombie reconnect — resulting in duplicate toasts.
    let alive = true;
    let socket: WebSocket | undefined;
    let timer: ReturnType<typeof setTimeout> | undefined;

    const connect = () => {
      const ws = new WebSocket(WS_URL);
      socket = ws;

      ws.onopen = () => {
        retryRef.current = 1000;
        ws.send(`CONNECT\naccept-version:1.2\nheart-beat:0,0\nAuthorization:Bearer ${token}\n\n\0`);
        ws.send(`SUBSCRIBE\nid:sub-updates\ndestination:/user/queue/updates\n\n\0`);
      };

      ws.onmessage = (event: MessageEvent) => {
        const raw: string = event.data;
        const bodyStart = raw.indexOf('\n\n');
        if (bodyStart === -1) return;
        const body = raw.substring(bodyStart + 2).replace(/\0$/, '');
        if (!body) return;
        try {
          const p = JSON.parse(body) as { kind?: string; status?: string; message?: string };
          if (p.kind !== 'BOOKING' && p.kind !== 'VALET') return;
          const toast: UpdateToast = {
            id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
            kind: p.kind,
            status: p.status ?? '',
            message: p.message ?? 'Update received.',
          };
          setToasts(prev => [...prev, toast]);
          window.dispatchEvent(new CustomEvent('app-update', { detail: p }));
          setTimeout(() => setToasts(prev => prev.filter(t => t.id !== toast.id)), 6000);
        } catch {
          // CONNECTED / RECEIPT frames — ignore
        }
      };

      ws.onclose = () => {
        if (!alive) return;
        timer = setTimeout(() => {
          retryRef.current = Math.min(retryRef.current * 2, MAX_RETRY_MS);
          connect();
        }, retryRef.current);
      };

      ws.onerror = () => ws.close();
    };

    connect();
    return () => {
      alive = false;
      if (timer) clearTimeout(timer);
      socket?.close();
    };
  }, [token, user?.role]);

  const dismiss = useCallback((id: string) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  return { toasts, dismiss };
}
