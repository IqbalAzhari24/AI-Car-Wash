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

  // Auto-scroll to latest message
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
    // Fills the height the Layout reserves for it (h-dvh minus the nav).
    <div className="flex min-h-0 flex-1 flex-col bg-[#0D0D11]">
      {/* Header */}
      <div className="hex-grid-subtle border-b border-[#1E1E2D]">
        <div className="mx-auto flex w-full max-w-3xl items-center gap-3 px-4 py-3 sm:px-6">
          <div className="relative flex-shrink-0">
            <img
              src={timahAvatar}
              alt=""
              className="h-11 w-11 rounded-full object-cover ring-2 ring-[#00F0FF]"
            />
            <span
              className={`absolute bottom-0 right-0 h-3 w-3 rounded-full border-2 border-[#13131A] ${
                isConnected ? 'bg-[#00E5A0]' : 'bg-[#5A5A72]'
              }`}
            />
          </div>
          <div>
            <h1 className="font-display text-base font-semibold tracking-tight text-[#E8E8F0]">Timah</h1>
            <p className="font-mono text-xs text-[#9090A8]">
              {isConnected ? 'Online · AI Receptionist' : 'Connecting…'}
            </p>
          </div>
        </div>
      </div>

      {/* Message Thread */}
      <div className="min-h-0 flex-1 overflow-y-auto">
        <div className="mx-auto w-full max-w-3xl space-y-4 px-4 py-6 sm:px-6">
          {/* Welcome message */}
          {messages.length === 0 && (
            <div className="flex max-w-lg items-end gap-2.5">
              <img
                src={timahAvatar}
                alt=""
                className="h-8 w-8 flex-shrink-0 rounded-full object-cover"
              />
              <div className="hex-border rounded-2xl rounded-bl-sm bg-[#1A1A24] px-4 py-3 text-sm text-[#E8E8F0]">
                <p>
                  👋 Hi there! I'm <strong>Timah</strong>, your personal car wash booking
                  assistant.
                </p>
                <p className="mt-1">
                  Tell me what vehicle you're bringing in and when you'd like to come — I'll
                  handle the rest!
                </p>
              </div>
            </div>
          )}

          {messages.map(msg => (
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
                className={`max-w-xs px-4 py-3 text-sm sm:max-w-md lg:max-w-lg rounded-2xl
                  ${msg.role === 'user'
                    ? 'rounded-br-sm bg-[#00F0FF] text-[#0D0D11]'
                    : 'hex-border rounded-bl-sm bg-[#1A1A24] text-[#E8E8F0]'
                  }
                `}
              >
                <p className="whitespace-pre-wrap break-words">
                  {msg.content}
                  {msg.isStreaming && (
                    <span className="ml-0.5 inline-block h-4 w-1.5 animate-pulse rounded-sm bg-[#00F0FF] align-middle motion-reduce:animate-none" />
                  )}
                </p>
              </div>
            </div>
          ))}

          {/* Typing Indicator */}
          {isTyping && (
            <div className="flex items-end gap-2.5">
              <img
                src={timahAvatar}
                alt=""
                className="h-8 w-8 flex-shrink-0 rounded-full object-cover"
              />
              <div className="hex-border flex items-center gap-1 rounded-2xl rounded-bl-sm bg-[#1A1A24] px-4 py-3">
                <span className="h-2 w-2 animate-bounce rounded-full bg-[#00F0FF] [animation-delay:-0.3s] motion-reduce:animate-none" />
                <span className="h-2 w-2 animate-bounce rounded-full bg-[#00F0FF] [animation-delay:-0.15s] motion-reduce:animate-none" />
                <span className="h-2 w-2 animate-bounce rounded-full bg-[#00F0FF] motion-reduce:animate-none" />
              </div>
            </div>
          )}

          <div ref={bottomRef} />
        </div>
      </div>

      {/* Input Bar */}
      <div className="border-t border-[#1E1E2D] bg-[#0D0D11]">
        <div className="mx-auto w-full max-w-3xl px-4 pb-4 pt-3 sm:px-6">
          <div className="flex items-center gap-2 rounded-2xl border border-[#2A2A3D] bg-[#13131A] px-4 py-2 transition-colors duration-150 focus-within:border-[#00F0FF] focus-within:ring-1 focus-within:ring-[#00F0FF]">
            <label htmlFor="timah-chat-input" className="sr-only">
              Message Timah
            </label>
            <textarea
              id="timah-chat-input"
              rows={1}
              className="max-h-24 flex-1 resize-none bg-transparent text-sm text-[#E8E8F0] outline-none placeholder:text-[#5A5A72]"
              placeholder="Ask Timah to book a slot…"
              value={input}
              onChange={e => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={!isConnected}
            />
            <button
              id="timah-send-btn"
              onClick={handleSend}
              disabled={!isConnected || !input.trim()}
              className="flex min-h-[44px] flex-shrink-0 items-center rounded-xl bg-[#00F0FF] px-4 py-2 text-sm font-semibold text-[#0D0D11] shadow-cyan-glow transition-colors duration-150 hover:bg-[#00B8C4] disabled:cursor-not-allowed disabled:opacity-50"
            >
              Send
            </button>
          </div>
          <p className="mt-2 text-center font-mono text-xs text-[#5A5A72]">
            Timah operates Mon–Thu, 09:00–17:00. Closed Fridays.
          </p>
        </div>
      </div>
    </div>
  );
};
