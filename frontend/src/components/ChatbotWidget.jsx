import React, { useMemo, useState } from 'react';
import axios from 'axios';

const ChatbotWidget = () => {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState([
    { role: 'bot', text: 'Hi! I am Assistly Bot. Ask me about login, requests, communities, maps, or password reset.' }
  ]);

  const apiUrl = useMemo(() => 'http://localhost:8080/api/chatbot/message', []);

  const sendMessage = async () => {
    const message = input.trim();
    if (!message || loading) return;

    const currentUser = JSON.parse(localStorage.getItem('user') || 'null');
    const headers = currentUser?.token
      ? { Authorization: `Bearer ${currentUser.token}` }
      : undefined;

    setMessages((prev) => [...prev, { role: 'user', text: message }]);
    setInput('');
    setLoading(true);

    try {
      const res = await axios.post(apiUrl, { message }, { headers });
      setMessages((prev) => [...prev, { role: 'bot', text: res.data?.reply || 'I could not process that yet.' }]);
    } catch (err) {
      const fallback = err.response?.data?.message || 'Chat service is currently unavailable.';
      setMessages((prev) => [...prev, { role: 'bot', text: fallback }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="chatbot-root">
      {open && (
        <div className="chatbot-panel">
          <div className="chatbot-header">
            <strong>Assistly Chat</strong>
            <button className="chatbot-close" onClick={() => setOpen(false)} aria-label="Close chat">
              x
            </button>
          </div>
          <div className="chatbot-body">
            {messages.map((msg, idx) => (
              <div key={idx} className={`chat-msg chat-msg-${msg.role}`}>
                {msg.text}
              </div>
            ))}
            {loading && <div className="chat-msg chat-msg-bot">Typing...</div>}
          </div>
          <div className="chatbot-input-row">
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
              placeholder="Ask something..."
              className="chatbot-input"
            />
            <button onClick={sendMessage} className="chatbot-send" disabled={loading}>
              Send
            </button>
          </div>
        </div>
      )}
      <button className="chatbot-fab" onClick={() => setOpen((v) => !v)}>
        Chat
      </button>
    </div>
  );
};

export default ChatbotWidget;
