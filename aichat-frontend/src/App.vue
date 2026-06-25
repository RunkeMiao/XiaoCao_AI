<!-- src/App.vue -->
<template>
  <div class="app-layout">
    <!-- 侧边栏 -->
    <Sidebar
        :chat-history="chatHistory"
        :current-session-id="sessionId"
        @new-chat="handleNewChat"
        @switch-session="switchSession"
        @delete-session="deleteSession"
    />

    <!-- 主区域 -->
    <main class="main-area">
      <!-- 顶部标题栏 -->
      <header class="topbar">
        <h1 class="topbar-title">XiaoCao AI</h1>
        <button class="btn-theme" @click="toggleTheme" :title="isDark ? '切换浅色' : '切换深色'">
          <svg v-if="isDark" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="1.8">
            <circle cx="12" cy="12" r="5"></circle>
            <line x1="12" y1="1" x2="12" y2="3"></line>
            <line x1="12" y1="21" x2="12" y2="23"></line>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
            <line x1="1" y1="12" x2="3" y2="12"></line>
            <line x1="21" y1="12" x2="23" y2="12"></line>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
          </svg>
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
          </svg>
        </button>
      </header>

      <!-- 消息区域 -->
      <div class="messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="welcome">
          <h2>亲爱的用户，你说，我在听！</h2>
          <div class="welcome-input-wrapper">
            <textarea
                ref="inputRef"
                v-model="input"
                @keydown.enter.exact.prevent="handleSend"
                @input="autoResize"
                placeholder="输入消息..."
                rows="1"
                :disabled="isStreaming"
            ></textarea>
            <button
                class="btn-send-circle"
                @click="handleSend"
                :disabled="isStreaming || !input.trim()"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                   stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="17" x2="12" y2="7"></line>
                <polyline points="8 11 12 7 16 11"></polyline>
              </svg>
            </button>
          </div>
        </div>

        <ChatMessage
            v-for="(msg, i) in messages"
            :key="i"
            :msg="msg"
            :streaming="isStreaming && i === messages.length - 1 && msg.role === 'ai'"
        />
      </div>

      <!-- 输入区域（有消息时显示） -->
      <div class="input-area" v-if="messages.length > 0">
        <div class="input-wrapper">
          <textarea
              ref="chatInputRef"
              v-model="input"
              @keydown.enter.exact.prevent="handleSend"
              @input="autoResizeChatInput"
              placeholder="问问XiaoCaoAI"
              rows="1"
              :disabled="isStreaming"
          ></textarea>
          <button
              class="btn-send-circle"
              @click="handleSend"
              :disabled="isStreaming || !input.trim()"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                 stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="17" x2="12" y2="7"></line>
              <polyline points="8 11 12 7 16 11"></polyline>
            </svg>
          </button>
        </div>
        <p class="disclaimer">AI 生成的内容仅供参考，请仔细甄别</p>
      </div>
    </main>
  </div>
</template>

<script setup>
import {ref, watch, nextTick, onMounted} from 'vue'
import {useChat} from './composables/useChat.js'
import {useTheme} from './composables/useTheme.js'
import ChatMessage from './components/ChatMessage.vue'
import Sidebar from './components/Sidebar.vue'

const {messages, isStreaming, sessionId, chatHistory, sendMessage, newSession, switchSession, deleteSession} = useChat()
const {isDark, toggleTheme} = useTheme()

const input = ref('')
const inputRef = ref(null)
const chatInputRef = ref(null)
const messagesRef = ref(null)

async function handleSend() {
  const text = input.value.trim()
  if (!text) return
  input.value = ''
  // 立即重置输入框高度并保持焦点
  const el = chatInputRef.value || inputRef.value
  if (el) {
    el.style.height = 'auto'
    el.focus()
  }
  await sendMessage(text)
}

function handleNewChat() {
  newSession()
}

function autoResize() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = el.scrollHeight + 'px'
}

function autoResizeChatInput() {
  const el = chatInputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = el.scrollHeight + 'px'
}

// 自动滚到底部（页面加载时也触发）
watch([messages, sessionId], async () => {
  await nextTick()
  await new Promise(r => setTimeout(r, 50))
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}, {deep: true, immediate: true})

// 页面加载后滚到底部（兜底）
onMounted(async () => {
  await nextTick()
  await new Promise(r => setTimeout(r, 100))
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
})
</script>

<style>
/* ===== CSS 变量（主题） ===== */
:root,
[data-theme="dark"] {
  --bg-primary: #0f0f0f;
  --bg-secondary: #14141c;
  --sidebar-bg: #1f1f1f;
  --surface: #1a1a26;
  --border-color: #252535;
  --hover-bg: rgba(255, 255, 255, 0.04);
  --active-bg: rgba(255, 255, 255, 0.08);
  --text-primary: #eaeaf0;
  --text-secondary: #9999aa;
  --text-muted: #55556a;
  --accent-color: #e0e0e0;
  --accent-light: #ffffff;
  --user-bubble: #171717;
  --ai-bubble: #1c1c28;
  --input-bg: #1e1f20;
  --scrollbar-thumb: #2a2a3e;
  --code-block-bg: #2a2a2a;
  --code-block-border: rgba(255, 255, 255, 0.12);
  --code-block-color: #d4d4d8;
  --inline-code-bg: rgba(255, 255, 255, 0.10);
  --inline-code-color: #b0b0c0;
}

[data-theme="light"] {
  --bg-primary: #ffffff;
  --bg-secondary: #f8f9fc;
  --sidebar-bg: #f4f5f8;
  --surface: #ffffff;
  --border-color: #e5e7ef;
  --hover-bg: rgba(0, 0, 0, 0.03);
  --active-bg: rgba(0, 0, 0, 0.06);
  --text-primary: #1a1a2e;
  --text-secondary: #55557a;
  --text-muted: #9999aa;
  --accent-color: #333333;
  --accent-light: #000000;
  --user-bubble: #f2f0f0;
  --ai-bubble: #f8f9fc;
  --input-bg: #f0f1f5;
  --scrollbar-thumb: #d1d5db;
  --code-block-bg: #f0f0f5;
  --code-block-border: #e0e0ea;
  --code-block-color: #33334a;
  --inline-code-bg: rgba(0, 0, 0, 0.06);
  --inline-code-color: #555555;
}

/* ===== Reset & Base ===== */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Noto Sans SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: var(--bg-primary);
  color: var(--text-primary);
  height: 100vh;
  overflow: hidden;
}

/* ===== Layout ===== */
.app-layout {
  display: flex;
  height: 100vh;
  width: 100%;
}

/* ===== Main Area ===== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--bg-primary);
}

/* ===== Topbar ===== */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 28px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-primary);
  flex-shrink: 0;
}

.topbar-title {
  font-size: 17px;
  font-weight: 650;
  letter-spacing: 0.02em;
  color: var(--text-primary);
}

.btn-theme {
  width: 38px;
  height: 38px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.btn-theme:hover {
  background: var(--hover-bg);
  color: var(--text-primary);
  border-color: var(--text-muted);
}

/* ===== Messages ===== */
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.messages::-webkit-scrollbar {
  width: 5px;
}

.messages::-webkit-scrollbar-track {
  background: transparent;
}

.messages::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb);
  border-radius: 3px;
}

/* ===== Welcome ===== */
.welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 28px;
  color: var(--text-muted);
}

.welcome h2 {
  font-size: 26px;
  font-weight: 500;
  color: var(--text-primary);
  letter-spacing: 0.03em;
}

.welcome-input-wrapper {
  width: 680px;
  max-width: 90vw;
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: var(--input-bg);
  border: 1px solid var(--border-color);
  border-radius: 24px;
  padding: 14px 18px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.welcome-input-wrapper:focus-within {
  border-color: #888;
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.06);
}

/* ===== Input Area ===== */
.input-area {
  padding: 16px 28px 20px;
  flex-shrink: 0;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  max-width: 800px;
  margin: 0 auto;
  background: var(--input-bg);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 10px 14px;
  transition: border-color 0.2s;
}

.input-wrapper:focus-within {
  border-color: #888;
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.06);
}

textarea {
  flex: 1;
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-size: 14.5px;
  font-family: inherit;
  resize: none;
  outline: none;
  max-height: 150px;
  line-height: 1.6;
  padding: 8px 0;
}

textarea::-webkit-scrollbar {
  width: 4px;
}

textarea::-webkit-scrollbar-track {
  background: transparent;
}

textarea::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb);
  border-radius: 2px;
}

textarea::placeholder {
  color: var(--text-muted);
}

textarea:disabled {
  opacity: 0.5;
}

.btn-send-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: #333;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s;
}

.btn-send-circle:hover:not(:disabled) {
  background: #555;
}

.btn-send-circle:active:not(:disabled) {
  transform: scale(0.92);
}

.btn-send-circle:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.disclaimer {
  text-align: center;
  font-size: 11.5px;
  color: var(--text-muted);
  margin-top: 10px;
}
</style>
