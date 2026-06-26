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
          <!-- 发送按钮（非流式输出时显示） -->
          <button
              v-if="!isStreaming"
              class="btn-send-circle"
              @click="handleSend"
              :disabled="!input.trim()"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                 stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="17" x2="12" y2="7"></line>
              <polyline points="8 11 12 7 16 11"></polyline>
            </svg>
          </button>
          <!-- 停止按钮（流式输出时显示） -->
          <button
              v-else
              class="btn-stop-circle"
              @click="stopGeneration"
              title="停止生成"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <rect x="6" y="6" width="12" height="12" rx="2"></rect>
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
import {useChat} from '../composables/useChat.js'
import {useTheme} from '../composables/useTheme.js'
import ChatMessage from '../components/ChatMessage.vue'
import Sidebar from '../components/Sidebar.vue'

const {messages, isStreaming, sessionId, chatHistory, sendMessage, stopGeneration, newSession, switchSession, deleteSession} = useChat()
const {isDark, toggleTheme} = useTheme()

const input = ref('')
const inputRef = ref(null)
const chatInputRef = ref(null)
const messagesRef = ref(null)

async function handleSend() {
  const text = input.value.trim()
  if (!text) return
  
  // 清空输入框
  input.value = ''
  
  // 立即重置输入框高度
  const currentInput = chatInputRef.value || inputRef.value
  if (currentInput) {
    currentInput.style.height = 'auto'
  }
  
  // 发送消息
  await sendMessage(text)
  
  // 等待 DOM 更新（可能从欢迎页切换到聊天页）
  await nextTick()
  
  // 重新获取输入框引用并聚焦
  await new Promise(resolve => setTimeout(resolve, 50))
  const newInput = chatInputRef.value || inputRef.value
  if (newInput) {
    newInput.focus()
  }
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
  
  // 消息更新后，保持输入框焦点
  await nextTick()
  const inputEl = chatInputRef.value || inputRef.value
  if (inputEl && !inputEl.disabled) {
    inputEl.focus()
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

<style scoped>
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
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.1);
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
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.1);
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
  background: var(--btn-primary-bg, #111);
  color: var(--btn-primary-text, #fff);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s;
}

.btn-send-circle:hover:not(:disabled) {
  opacity: 0.85;
}

.btn-send-circle:active:not(:disabled) {
  transform: scale(0.92);
}

.btn-send-circle:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* 停止按钮 */
.btn-stop-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: var(--btn-primary-bg, #111);
  color: var(--btn-primary-text, #fff);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s;
}

.btn-stop-circle:hover {
  opacity: 0.85;
}

.btn-stop-circle:active {
  transform: scale(0.92);
}

.disclaimer {
  text-align: center;
  font-size: 11.5px;
  color: var(--text-muted);
  margin-top: 10px;
}
</style>
