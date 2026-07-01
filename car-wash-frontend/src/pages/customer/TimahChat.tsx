import React, { useEffect, useRef, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useWebSocket } from '../../hooks/useWebSocket';
import timahAvatar from '../../assets/timah_avatar.png';

export const TimahChat: React.FC = () => {
  const { user, token } = useAuth();
  const { messages, isConnected, isTyping, sendMessage } = useWebSocket({
    token,
    userId: user?.id ?? null,
  });
  const [input, setInput] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isTyping]);

  const handleSend = () => {
    if (!input.trim()) return;
    sendMessage(input.trim());
    setInput('');
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="flex min-h-0 flex-1 flex-col bg-base">

      {/* Header */}
      <div className="hex-grid border-b hex-border">
        <div className="mx-auto flex w-full max-w-3xl items-center gap-3 px-4 py-3 sm:px-6">
          <div className="relative flex-shrink-0">
            <img
              src={timahAvatar}
              alt=""
              className="h-11 w-11 rounded-full object-cover ring-2 ring-cyan/40"
            />
            <span
              className={`absolute bottom-0 right-0 h-3 w-3 rounded-full border-2 border-surface ${
                isConnected ? 'bg-success shadow-success-glow animate-pulse' : 'bg-muted'
              }`}
              aria-hidden="true"
            />
          </div>
          <div>
            <h1 className="text-base font-semibold text-primary">Timah</h1>
            <p className="text-xs text-secondary">
              {isConnected ? (
                <span className="flex items-center gap-1.5">
                  <span className="h-1.5 w-1.5 rounded-full bg-success" aria-hidden="true" />
                  Online · AI Receptionist
                </span>
              ) : (
                'Connecting…'
              )}
            </p>
          </div>
        </div>
      </div>

      {/* Message thread */}
      <div className="min-h-0 flex-1 overflow-y-auto bg-base">
        <div className="mx-auto w-full max-w-3xl space-y-4 px-4 py-6 sm:px-6">

          {/* Welcome */}
          {messages.length === 0 && (
            <div className="flex max-w-lg items-end gap-2.5">
              <img
                src={timahAvatar}
                alt=""
                className="h-8 w-8 flex-shrink-0 rounded-full object-cover"
              />
              <div className="hex-grid hex-border rounded-2xl rounded-bl-sm px-4 py-3 text-sm text-primary">
                <p>
                  Hi there! I'm <strong className="text-cyan">Timah</strong>, your personal car wash
                  booking assistant.
                </p>
                <p className="mt-1 text-secondary">
                  Tell me what vehicle you're bringing in and when you'd like to come — I'll handle
                  the rest!
                </p>
              </div>
            </div>
          )}

          {messages.map((msg) => (
            <div
              key={msg.id}
              className={`flex items-end gap-2.5 ${msg.role === 'user' ? 'flex-row-reverse' : ''}`}
            >
              {msg.role === 'assistant' && (
                <img
                  src={timahAvatar}
                  alt=""
                  className="h-8 w-8 flex-shrink-0 rounded-full object-cover"
                />
              )}
              <div
                className={`max-w-xs px-4 py-3 text-sm sm:max-w-md lg:max-w-lg rounded-2xl ${
                  msg.role === 'user'
                    ? 'rounded-br-sm bg-cyan/20 border border-cyan/40 text-primary'
                    : 'rounded-bl-sm hex-grid hex-border text-primary'
                }`}
              >
                <p className="whitespace-pre-wrap break-words">
                  {msg.content}
                  {msg.isStreaming && (
                    <span
                      className="ml-0.5 inline-block h-4 w-1.5 animate-pulse rounded-sm bg-cyan align-middle motion-reduce:animate-none"
                      aria-hidden="true"
                    />
                  )}
                </p>
              </div>
            </div>
          ))}

          {/* Typing indicator */}
          {isTyping && (
            <div className="flex items-end gap-2.5">
              <img
                src={timahAvatar}
                alt=""
                className="h-8 w-8 flex-shrink-0 rounded-full object-cover"
              />
              <div className="flex items-center gap-1 hex-grid hex-border rounded-2xl rounded-bl-sm px-4 py-3">
                <span className="h-2 w-2 animate-bounce rounded-full bg-secondary [animation-delay:-0.3s] motion-reduce:animate-none" aria-hidden="true" />
                <span className="h-2 w-2 animate-bounce rounded-full bg-secondary [animation-delay:-0.15s] motion-reduce:animate-none" aria-hidden="true" />
                <span className="h-2 w-2 animate-bounce rounded-full bg-secondary motion-reduce:animate-none" aria-hidden="true" />
                <span className="sr-only">Timah is typing</span>
              </div>
            </div>
          )}

          <div ref={bottomRef} />
        </div>
      </div>

      {/* Input bar */}
      <div className="border-t border-border bg-base safe-bottom">
        <div className="mx-auto w-full max-w-3xl px-4 pb-4 pt-3 sm:px-6">
          <div className="flex items-center gap-2 hex-grid-subtle hex-border rounded-2xl px-4 py-2 transition-all duration-150 focus-within:hex-border-active">
            <label htmlFor="timah-chat-input" className="sr-only">
              Message Timah
            </label>
            <textarea
              id="timah-chat-input"
              rows={1}
              className="max-h-24 flex-1 resize-none bg-transparent text-sm text-primary outline-none placeholder:text-muted"
              placeholder="Ask Timah to book a slot…"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={!isConnected}
            />
            <button
              id="timah-send-btn"
              onClick={handleSend}
              disabled={!isConnected || !input.trim()}
              className="flex-shrink-0 min-h-[44px] min-w-[44px] inline-flex items-center justify-center rounded-xl border border-cyan px-4 text-sm font-medium text-cyan shadow-cyan-glow transition-all duration-150 hover:bg-cyan hover:text-base active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-40 disabled:pointer-events-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan focus-visible:ring-offset-2 focus-visible:ring-offset-base"
            >
              Send
            </button>
          </div>
          <p className="mt-2 text-center text-xs text-muted">
            Timah operates 09:00–18:00. Closed Fridays.
          </p>
        </div>
      </div>
    </div>
  );
};
