import { useEffect, useRef, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  isStreaming?: boolean;
}

interface UseWebSocketOptions {
  token: string | null;
  userId: string | null;
}

const WS_URL = new URL(import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api').origin + '/ws/timah/websocket';
const MAX_RECONNECT_DELAY_MS = 30000;

export function useWebSocket({ token, userId }: UseWebSocketOptions) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectDelayRef = useRef(1000);
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const navigate = useNavigate();

  const connect = useCallback(() => {
    if (!token || !userId) return;
    if (wsRef.current?.readyState === WebSocket.OPEN) return;

    const ws = new WebSocket(WS_URL);
    wsRef.current = ws;

    ws.onopen = () => {
      setIsConnected(true);
      reconnectDelayRef.current = 1000;
      console.log('[Timah WS] Connected');

      // STOMP CONNECT frame — JWT in Authorization header (not URL, to avoid log exposure)
      ws.send(
        `CONNECT\naccept-version:1.2\nheart-beat:0,0\nAuthorization:Bearer ${token}\n\n\0`
      );
      // Subscribe to personal reply queue
      ws.send(
        `SUBSCRIBE\nid:sub-0\ndestination:/user/queue/timah-reply\n\n\0`
      );
    };

    ws.onmessage = (event: MessageEvent) => {
      const raw: string = event.data;
      // Parse STOMP MESSAGE frame body
      const bodyStart = raw.indexOf('\n\n');
      if (bodyStart === -1) return;
      const body = raw.substring(bodyStart + 2).replace(/\0$/, '');
      if (!body) return;

      try {
        const payload = JSON.parse(body) as { type: string; content: string };

        if (payload.type === 'TOKEN') {
          setIsTyping(false);
          setMessages(prev => {
            const last = prev[prev.length - 1];
            if (last?.role === 'assistant' && last.isStreaming) {
              return [
                ...prev.slice(0, -1),
                { ...last, content: last.content + payload.content },
              ];
            }
            return [
              ...prev,
              { id: Date.now().toString(), role: 'assistant', content: payload.content, isStreaming: true },
            ];
          });
        }

        if (payload.type === 'DONE') {
          setMessages(prev => {
            const last = prev[prev.length - 1];
            if (last?.role === 'assistant') {
              try {
                const parsed = JSON.parse(last.content);
                if (parsed.action === 'REDIRECT_CHECKOUT' && parsed.bookingId) {
                  navigate(`/checkout/${parsed.bookingId}`);
                  return prev;
                }
              } catch (_) { /* normal text */ }
              return [...prev.slice(0, -1), { ...last, isStreaming: false }];
            }
            return prev;
          });
        }

        if (payload.type === 'ERROR') {
          setIsTyping(false);
          setMessages(prev => [
            ...prev,
            { id: Date.now().toString(), role: 'assistant', content: payload.content, isStreaming: false },
          ]);
        }
      } catch (_) {
        // Non-JSON frame (CONNECTED, RECEIPT, etc.) — ignore
      }
    };

    ws.onclose = () => {
      setIsConnected(false);
      console.log(`[Timah WS] Disconnected — retrying in ${reconnectDelayRef.current}ms`);
      reconnectTimerRef.current = setTimeout(() => {
        reconnectDelayRef.current = Math.min(reconnectDelayRef.current * 2, MAX_RECONNECT_DELAY_MS);
        connect();
      }, reconnectDelayRef.current);
    };

    ws.onerror = (err) => {
      console.error('[Timah WS] Error', err);
      ws.close();
    };
  }, [token, userId, navigate]);

  useEffect(() => {
    connect();
    return () => {
      if (reconnectTimerRef.current) clearTimeout(reconnectTimerRef.current);
      wsRef.current?.close();
    };
  }, [connect]);

  const sendMessage = useCallback((text: string) => {
    if (wsRef.current?.readyState !== WebSocket.OPEN || !text.trim()) return;

    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      role: 'user',
      content: text,
    };
    setMessages(prev => [...prev, userMsg]);
    setIsTyping(true);

    // STOMP SEND frame
    const body = JSON.stringify({ message: text });
    const frame = `SEND\ndestination:/app/timah/chat\ncontent-type:application/json\ncontent-length:${body.length}\n\n${body}\0`;
    wsRef.current.send(frame);
  }, []);

  return { messages, isConnected, isTyping, sendMessage };
}
