<template>
  <!-- 移动端遮罩层 -->
  <div v-if="isMobile && !collapsed" class="sidebar-overlay" @click="collapsed = true"></div>

  <aside class="sidebar" :class="{ collapsed, 'mobile': isMobile }">
    <!-- 退出登录确认弹窗 -->
    <ConfirmModal
      v-model:visible="showLogoutModal"
      title="退出登录"
      message="确定要退出当前账号吗？退出后需要重新登录才能使用。"
      confirm-text="退出"
      cancel-text="取消"
      type="warning"
      @confirm="confirmLogout"
    />
    <!-- Logo 区域 -->
    <div class="sidebar-header" :class="{ collapsed }">
      <div class="logo" v-show="!collapsed">
        <span class="logo-text">XiaoCao AI</span>
      </div>
      <button class="btn-toggle" @click="collapsed = !collapsed" :title="collapsed ? '展开' : '收起'">
        <svg v-if="collapsed" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2.5">
          <polyline points="9 18 15 12 9 6"></polyline>
        </svg>
        <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
      </button>
    </div>

    <!-- 新对话按钮 -->
    <button class="btn-new-chat" @click="handleNewChat">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <line x1="12" y1="5" x2="12" y2="19"></line>
        <line x1="5" y1="12" x2="19" y2="12"></line>
      </svg>
      <span v-show="!collapsed">新对话</span>
    </button>

    <!-- 菜单项 -->
    <nav class="sidebar-nav" v-show="!collapsed">
      <button class="nav-item">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <circle cx="12" cy="12" r="10"></circle>
          <polyline points="12 6 12 12 16 14"></polyline>
        </svg>
        <span>对话历史</span>
      </button>
      <button class="nav-item">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <circle cx="12" cy="12" r="3"></circle>
          <path
              d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
        </svg>
        <span>设置</span>
      </button>
    </nav>

    <!-- 最近对话 -->
    <div class="history-section" v-show="!collapsed">
      <div class="history-label">最近对话</div>
      <div class="history-list">
        <div
            v-for="item in chatHistory"
            :key="item.id"
            class="history-item"
            :class="{ active: item.id === currentSessionId }"
            @click="handleSwitchSession(item.id)"
            role="button"
            tabindex="0"
            @keydown.enter="handleSwitchSession(item.id)"
            @keydown.space.prevent="handleSwitchSession(item.id)"
        >
          <span class="history-title">{{ item.title }}</span>
          <span class="history-time">{{ formatTime(item.updatedAt) }}</span>
          <button class="btn-delete" @click.stop="$emit('delete-session', item.id)" title="删除">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <div v-if="chatHistory.length === 0" class="history-empty">暂无对话记录</div>
      </div>
    </div>

    <!-- 底部用户信息 -->
    <div class="sidebar-footer" v-show="!collapsed">
      <div class="user-card">
        <div class="user-avatar">{{ username ? username.charAt(0).toUpperCase() : 'U' }}</div>
        <div class="user-info">
          <span class="user-name">{{ username || '未登录' }}</span>
        </div>
        <button class="btn-logout" @click="handleLogout" title="退出登录">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
            <polyline points="16 17 21 12 16 7"></polyline>
            <line x1="21" y1="12" x2="9" y2="12"></line>
          </svg>
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup>
import {ref, computed, onMounted, onUnmounted} from 'vue'
import {useAuth} from '../composables/useAuth.js'
import ConfirmModal from './ConfirmModal.vue'

const {username, logout} = useAuth()

const props = defineProps({
  chatHistory: {type: Array, default: () => []},
  currentSessionId: {type: String, default: ''},
  collapsed: {type: Boolean, default: false}
})

const emit = defineEmits(['new-chat', 'switch-session', 'delete-session', 'update:collapsed'])

const showLogoutModal = ref(false)
const isMobile = ref(false)

// 计算collapsed状态
const collapsed = computed({
  get: () => props.collapsed,
  set: (value) => emit('update:collapsed', value)
})

// 检测是否为移动端
function checkMobile() {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) {
    emit('update:collapsed', true)
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

function handleLogout() {
  showLogoutModal.value = true
}

function confirmLogout() {
  logout()
}

function handleNewChat() {
  emit('new-chat')
  if (isMobile.value) {
    collapsed.value = true
  }
}

function handleSwitchSession(id) {
  emit('switch-session', id)
  if (isMobile.value) {
    collapsed.value = true
  }
}

function formatTime(timestamp) {
  if (!timestamp) return ''
  const diff = Date.now() - timestamp
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return new Date(timestamp).toLocaleDateString('zh-CN', {month: 'numeric', day: 'numeric'})
}
</script>

<style scoped>
/* 移动端遮罩层 */
.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 99;
}

.sidebar {
  width: 260px;
  min-width: 260px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--sidebar-border);
  transition: width 0.25s ease, min-width 0.25s ease, transform 0.25s ease;
  overflow: hidden;
}

.sidebar.collapsed {
  width: 56px;
  min-width: 56px;
}

.sidebar.collapsed .btn-toggle {
  margin: 0 auto;
}

/* 移动端样式 */
.sidebar.mobile {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 100;
  width: 280px;
  min-width: 280px;
  transform: translateX(0);
}

.sidebar.mobile.collapsed {
  width: 0;
  min-width: 0;
  transform: translateX(-100%);
  border-right: none;
}

/* Header */
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid var(--sidebar-border);
  flex-shrink: 0;
}

.sidebar-header.collapsed {
  justify-content: center;
  padding: 0;
}

.logo {
  display: flex;
  align-items: center;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  letter-spacing: 0.02em;
}

.btn-toggle {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  flex-shrink: 0;
}

.btn-toggle:hover {
  background: var(--sidebar-hover);
  color: var(--text-primary);
}

/* New Chat Button */
.btn-new-chat {
  margin: 12px 16px;
  padding: 10px 16px;
  border: 1px solid var(--sidebar-border);
  border-radius: 10px;
  background: transparent;
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  font-family: inherit;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.15s;
  width: calc(100% - 32px);
}

.collapsed .btn-new-chat {
  margin: 12px auto;
  padding: 10px;
  width: 40px;
  justify-content: center;
}

.btn-new-chat:hover {
  background: var(--sidebar-hover);
  border-color: var(--text-muted);
}

/* Nav */
.sidebar-nav {
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: none;
  background: transparent;
  color: var(--sidebar-text-muted);
  font-size: 14px;
  cursor: pointer;
  font-family: inherit;
  border-radius: 8px;
  transition: all 0.15s;
  text-align: left;
  width: 100%;
}

.nav-item:hover {
  background: var(--sidebar-hover);
  color: var(--sidebar-text);
}

/* History */
.history-section {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
}

.history-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--sidebar-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 8px 12px 4px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border: none;
  background: transparent;
  color: var(--sidebar-text-muted);
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
  border-radius: 8px;
  transition: all 0.15s;
  text-align: left;
  width: 100%;
  position: relative;
  line-height: 1.4;
}

.history-item:hover {
  background: var(--sidebar-session-hover);
  color: var(--sidebar-text);
}

.history-item.active {
  background: var(--sidebar-session-active);
  color: var(--sidebar-text);
}

.history-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.history-time {
  font-size: 12px;
  color: var(--text-muted);
  flex-shrink: 0;
  margin-right: 24px;
  line-height: 1.4;
}

.btn-delete {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  width: 22px;
  height: 22px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 4px;
  display: none;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.1s;
}

.history-item:hover .btn-delete {
  display: flex;
  opacity: 1;
}

.btn-delete:hover {
  background: rgba(0, 0, 0, 0.08);
  color: var(--text-primary);
}

.history-empty {
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
  padding: 20px 0;
}

/* Footer */
.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--sidebar-border);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}

.user-card:hover {
  background: var(--sidebar-hover);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--text-primary);
  color: var(--bg-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-email {
  font-size: 11px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-logout {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s;
}

.btn-logout:hover {
  background: var(--sidebar-hover);
  color: #e74c3c;
}
</style>
