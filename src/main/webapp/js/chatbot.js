// ============================================================
// chatbot.js — Red404 AI Chatbot Widget
// Blood Donation FAQ Assistant powered by Claude AI
// Add this script to donor-dashboard.html
// ============================================================

(function () {
  // ── Inject Styles ─────────────────────────────────────────
  const style = document.createElement('style');
  style.textContent = `
    #red404-chat-btn {
      position: fixed;
      bottom: 28px;
      right: 28px;
      width: 58px;
      height: 58px;
      background: #c0392b;
      border-radius: 50%;
      border: none;
      cursor: pointer;
      box-shadow: 0 4px 20px rgba(192,57,43,0.45);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 9999;
      transition: transform 0.2s, box-shadow 0.2s;
    }
    #red404-chat-btn:hover {
      transform: scale(1.08);
      box-shadow: 0 6px 28px rgba(192,57,43,0.55);
    }
    #red404-chat-btn svg { width: 26px; height: 26px; fill: white; }

    #red404-chat-window {
      position: fixed;
      bottom: 100px;
      right: 28px;
      width: 360px;
      max-height: 520px;
      background: #fff;
      border-radius: 18px;
      box-shadow: 0 8px 40px rgba(0,0,0,0.18);
      display: none;
      flex-direction: column;
      z-index: 9998;
      overflow: hidden;
      font-family: 'Segoe UI', sans-serif;
      animation: chatSlideUp 0.25s ease;
    }
    @keyframes chatSlideUp {
      from { opacity: 0; transform: translateY(16px); }
      to   { opacity: 1; transform: translateY(0); }
    }
    #red404-chat-window.open { display: flex; }

    #red404-chat-header {
      background: #c0392b;
      color: white;
      padding: 14px 18px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    #red404-chat-header .title {
      font-weight: 700;
      font-size: 0.95rem;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    #red404-chat-header .subtitle {
      font-size: 0.72rem;
      opacity: 0.82;
      margin-top: 2px;
    }
    #red404-close-btn {
      background: none;
      border: none;
      color: white;
      cursor: pointer;
      font-size: 1.3rem;
      line-height: 1;
      padding: 2px 6px;
      border-radius: 6px;
      transition: background 0.15s;
    }
    #red404-close-btn:hover { background: rgba(255,255,255,0.2); }

    #red404-messages {
      flex: 1;
      overflow-y: auto;
      padding: 14px 14px 8px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      background: #faf9f9;
    }
    #red404-messages::-webkit-scrollbar { width: 4px; }
    #red404-messages::-webkit-scrollbar-thumb { background: #ddd; border-radius: 4px; }

    .chat-msg {
      max-width: 82%;
      padding: 10px 13px;
      border-radius: 14px;
      font-size: 0.87rem;
      line-height: 1.5;
      word-break: break-word;
    }
    .chat-msg.bot {
      background: #fff;
      color: #222;
      align-self: flex-start;
      border: 1px solid #eee;
      border-bottom-left-radius: 4px;
      box-shadow: 0 1px 4px rgba(0,0,0,0.06);
    }
    .chat-msg.user {
      background: #c0392b;
      color: white;
      align-self: flex-end;
      border-bottom-right-radius: 4px;
    }
    .chat-msg.typing {
      background: #fff;
      border: 1px solid #eee;
      align-self: flex-start;
      color: #999;
      font-style: italic;
      font-size: 0.82rem;
    }

    .quick-btns {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      padding: 6px 14px 10px;
      background: #faf9f9;
    }
    .quick-btn {
      background: #fff;
      border: 1.5px solid #c0392b;
      color: #c0392b;
      border-radius: 20px;
      padding: 5px 12px;
      font-size: 0.78rem;
      cursor: pointer;
      transition: background 0.15s, color 0.15s;
      font-family: inherit;
    }
    .quick-btn:hover { background: #c0392b; color: white; }

    #red404-input-row {
      display: flex;
      gap: 8px;
      padding: 10px 12px;
      border-top: 1px solid #f0f0f0;
      background: #fff;
    }
    #red404-input {
      flex: 1;
      border: 1.5px solid #e0e0e0;
      border-radius: 22px;
      padding: 8px 14px;
      font-size: 0.87rem;
      outline: none;
      font-family: inherit;
      transition: border-color 0.2s;
    }
    #red404-input:focus { border-color: #c0392b; }
    #red404-send-btn {
      background: #c0392b;
      border: none;
      border-radius: 50%;
      width: 38px;
      height: 38px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      transition: background 0.15s;
    }
    #red404-send-btn:hover { background: #a93226; }
    #red404-send-btn svg { width: 16px; height: 16px; fill: white; }
    #red404-send-btn:disabled { background: #ccc; cursor: not-allowed; }
  `;
  document.head.appendChild(style);

  // ── Create Chat Button ────────────────────────────────────
  const chatBtn = document.createElement('button');
  chatBtn.id = 'red404-chat-btn';
  chatBtn.title = 'Blood Donation Assistant';
  chatBtn.innerHTML = `<svg viewBox="0 0 24 24"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"/></svg>`;
  document.body.appendChild(chatBtn);

  // ── Create Chat Window ────────────────────────────────────
  const chatWindow = document.createElement('div');
  chatWindow.id = 'red404-chat-window';
  chatWindow.innerHTML = `
    <div id="red404-chat-header">
      <div>
        <div class="title">🩸 Red404 Assistant</div>
        <div class="subtitle">Blood Donation AI Helper</div>
      </div>
      <button id="red404-close-btn" title="Close">✕</button>
    </div>
    <div id="red404-messages"></div>
    <div class="quick-btns" id="red404-quick-btns">
      <button class="quick-btn">Donate karna safe hai?</button>
      <button class="quick-btn">Kitne time baad donate karein?</button>
      <button class="quick-btn">Kya khayein donate karne se pehle?</button>
      <button class="quick-btn">Blood group match kaise hota hai?</button>
    </div>
    <div id="red404-input-row">
      <input id="red404-input" type="text" placeholder="Sawaal poochein..." maxlength="300"/>
      <button id="red404-send-btn">
        <svg viewBox="0 0 24 24"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>
      </button>
    </div>
  `;
  document.body.appendChild(chatWindow);

  // ── State ─────────────────────────────────────────────────
  const messages = [];
  let isLoading = false;

  // ── Helpers ───────────────────────────────────────────────
  const msgContainer = () => document.getElementById('red404-messages');

  function addMessage(text, role) {
    const div = document.createElement('div');
    div.className = `chat-msg ${role}`;
    div.textContent = text;
    msgContainer().appendChild(div);
    msgContainer().scrollTop = msgContainer().scrollHeight;
    return div;
  }

  function showWelcome() {
    addMessage('Namaste! 🙏 Main Red404 ka Blood Donation Assistant hoon. Blood donation ke baare mein koi bhi sawaal poochein — main help karunga!', 'bot');
  }

  // ── API Call to Claude ────────────────────────────────────
  async function askClaude(userMessage) {
    messages.push({ role: 'user', content: userMessage });

    const typingEl = addMessage('Soch raha hoon...', 'typing');
    isLoading = true;
    document.getElementById('red404-send-btn').disabled = true;

    try {
      const response = await fetch('https://api.anthropic.com/v1/messages', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          model: 'claude-sonnet-4-20250514',
          max_tokens: 1000,
          system: `Tu Red404 blood donation platform ka AI assistant hai. 
Tu sirf blood donation, blood groups, donor health, donation process, 
aur related medical topics pe help karta hai. 
Jawab short aur simple Hindi-English mix (Hinglish) mein de.
Agar koi unrelated sawaal pooche toh politely redirect kar.
Platform: Red404 — donors aur hospitals ko connect karta hai.`,
          messages: messages
        })
      });

      const data = await response.json();
      typingEl.remove();

      const reply = data.content?.[0]?.text || 'Maafi chahta hoon, abhi jawab nahi de pa raha. Thodi der baad try karein.';
      messages.push({ role: 'assistant', content: reply });
      addMessage(reply, 'bot');

    } catch (err) {
      typingEl.remove();
      addMessage('Network error. Please check your connection.', 'bot');
    } finally {
      isLoading = false;
      document.getElementById('red404-send-btn').disabled = false;
    }
  }

  // ── Event Listeners ───────────────────────────────────────
  chatBtn.addEventListener('click', () => {
    chatWindow.classList.toggle('open');
    if (chatWindow.classList.contains('open') && msgContainer().children.length === 0) {
      showWelcome();
    }
  });

  document.getElementById('red404-close-btn').addEventListener('click', () => {
    chatWindow.classList.remove('open');
  });

  document.getElementById('red404-send-btn').addEventListener('click', sendMessage);

  document.getElementById('red404-input').addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !isLoading) sendMessage();
  });

  document.querySelectorAll('.quick-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      if (!isLoading) {
        addMessage(btn.textContent, 'user');
        askClaude(btn.textContent);
        document.getElementById('red404-quick-btns').style.display = 'none';
      }
    });
  });

  function sendMessage() {
    const input = document.getElementById('red404-input');
    const text = input.value.trim();
    if (!text || isLoading) return;
    input.value = '';
    document.getElementById('red404-quick-btns').style.display = 'none';
    addMessage(text, 'user');
    askClaude(text);
  }

})();