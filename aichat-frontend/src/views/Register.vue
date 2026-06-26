<template>
  <div class="login-page">
    <!-- 主题切换 -->
    <button class="btn-theme-float" @click="toggleTheme" :title="isDark ? '切换浅色' : '切换深色'">
      <svg v-if="isDark" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
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
      <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
      </svg>
    </button>

    <!-- 左侧品牌区域 -->
    <div class="login-left">
      <div class="left-header">
        <div class="logo">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 3L4 8l8 5 8-5-8-5z"/>
            <path d="M4 12l8 5 8-5"/>
            <path d="M4 16l8 5 8-5"/>
          </svg>
          <span class="logo-text">XiaoCao AI</span>
        </div>
      </div>

      <div class="left-content">
        <h1 class="slogan">智能 · 高效 · 简洁</h1>
        <p class="subtitle">新一代 AI 金融数据查询助手</p>
        <div class="illustration">
          <div class="illu-base"></div>
          <div class="illu-card">
            <div class="card-dots">
              <span></span><span></span><span></span>
            </div>
            <div class="card-line"></div>
          </div>
          <span class="deco deco-1"></span>
          <span class="deco deco-2"></span>
          <span class="deco deco-3"></span>
          <span class="deco deco-4"></span>
          <span class="deco deco-5"></span>
        </div>
      </div>
    </div>

    <!-- 右侧注册表单区域 -->
    <div class="login-right">
      <div class="login-form-card">
        <h2 class="form-title">创建账号</h2>
        <p class="form-subtitle">注册以开始使用 XiaoCao AI</p>

        <form @submit.prevent="handleRegister" class="login-form">
          <!-- 用户名 -->
          <div class="input-group">
            <div class="input-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
            </div>
            <input
                v-model="form.username"
                type="text"
                placeholder="用户名"
                autocomplete="username"
                @input="clearError"
            />
          </div>

          <!-- 姓名 -->
          <div class="input-group">
            <div class="input-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
            </div>
            <input
                v-model="form.realName"
                type="text"
                placeholder="姓名"
                autocomplete="name"
                @input="clearError"
            />
          </div>

          <!-- 邮箱 -->
          <div class="input-group">
            <div class="input-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
              </svg>
            </div>
            <input
                v-model="form.email"
                type="email"
                placeholder="邮箱"
                autocomplete="email"
                @input="clearError"
            />
          </div>

          <!-- 验证码 -->
          <div class="input-group input-group-code">
            <div class="input-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </div>
            <input
                v-model="form.code"
                type="text"
                placeholder="验证码"
                maxlength="6"
                autocomplete="one-time-code"
                @input="clearError"
            />
            <button type="button" class="btn-send-code" :disabled="sendingCode || codeCooldown > 0 || !form.email.trim()" @click="sendCode">
              {{ sendingCode ? '发送中...' : codeCooldown > 0 ? codeCooldown + 's' : '发送验证码' }}
            </button>
          </div>

          <!-- 密码 -->
          <div class="input-group">
            <div class="input-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </div>
            <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="密码（至少6位）"
                autocomplete="new-password"
                @input="clearError"
            />
            <button type="button" class="btn-toggle-pwd" @click="showPassword = !showPassword" tabindex="-1">
              <svg v-if="showPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="1.8">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                <line x1="1" y1="1" x2="23" y2="23"/>
              </svg>
              <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="1.8">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
            </button>
          </div>

          <!-- 确认密码 -->
          <div class="input-group">
            <div class="input-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </div>
            <input
                v-model="form.confirmPassword"
                :type="showPassword ? 'text' : 'password'"
                placeholder="确认密码"
                autocomplete="new-password"
                @input="clearError"
            />
          </div>

          <!-- 提示信息 -->
          <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
          <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>

          <!-- 注册按钮 -->
          <button type="submit" class="btn-login" :disabled="loading">
            {{ loading ? '注册中...' : '注册' }}
          </button>
        </form>

        <!-- 登录链接 -->
        <p class="register-link">
          已有账号？<a href="javascript:;" @click="$router.push('/login')">立即登录</a>
        </p>
      </div>

      <!-- 底部版权 -->
      <footer class="login-footer">
        &copy; 2026 XiaoCao AI. All rights reserved.
      </footer>
    </div>
  </div>
</template>

<script setup>
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {useTheme} from '../composables/useTheme.js'
import {useAuth} from '../composables/useAuth.js'
import {post} from '../utils/request.js'

const router = useRouter()
const {isDark, toggleTheme} = useTheme()
const {setLogin} = useAuth()

const form = ref({
  username: '',
  realName: '',
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})
const showPassword = ref(false)
const loading = ref(false)
const sendingCode = ref(false)
const codeCooldown = ref(0)
const errorMsg = ref('')
const successMsg = ref('')
let cooldownTimer = null

function clearError() {
  errorMsg.value = ''
}

async function sendCode() {
  if (!form.value.email.trim() || codeCooldown.value > 0) return
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email)) {
    errorMsg.value = '请输入正确的邮箱格式'
    return
  }
  errorMsg.value = ''
  sendingCode.value = true
  try {
    const res = await post('/api/email/send-code', {email: form.value.email})
    const data = await res.json()
    if (data.code === 200) {
      successMsg.value = data.message
      codeCooldown.value = 60
      cooldownTimer = setInterval(() => {
        codeCooldown.value--
        if (codeCooldown.value <= 0) clearInterval(cooldownTimer)
      }, 1000)
    } else {
      errorMsg.value = data.message
    }
  } catch (e) {
    errorMsg.value = e.name === 'TypeError' ? '无法连接服务器' : '发送失败，请稍后重试'
  } finally {
    sendingCode.value = false
  }
}

async function handleRegister() {
  if (!form.value.username.trim() || !form.value.realName.trim() || !form.value.email.trim() || !form.value.code.trim() || !form.value.password) {
    errorMsg.value = '请填写所有字段'
    return
  }
  if (form.value.password !== form.value.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  if (form.value.password.length < 6) {
    errorMsg.value = '密码长度至少6位'
    return
  }
  if (!/[A-Za-z]/.test(form.value.password) || !/[0-9]/.test(form.value.password)) {
    errorMsg.value = '密码必须包含字母和数字'
    return
  }
  errorMsg.value = ''
  successMsg.value = ''
  loading.value = true
  try {
    const res = await post('/api/auth/register', {
      username: form.value.username,
      realName: form.value.realName,
      email: form.value.email,
      code: form.value.code,
      password: form.value.password
    })
    const data = await res.json()
    if (data.code === 200) {
      setLogin(form.value.username, form.value.realName)
      successMsg.value = '注册成功，即将跳转...'
      setTimeout(() => router.push('/'), 1500)
    } else {
      errorMsg.value = data.message
    }
  } catch (e) {
    errorMsg.value = e.name === 'TypeError' ? '无法连接服务器，请检查网络' : '请求超时，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: var(--bg-secondary);
}

/* ===== 左侧 ===== */
.login-left {
  flex: 0 0 33.333%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 48px;
  position: relative;
  overflow: hidden;
  background: var(--bg-primary);
}

.left-header {
  position: absolute;
  top: 36px;
  left: 48px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-primary);
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.left-content {
  max-width: 420px;
  text-align: center;
}

.slogan {
  font-size: 34px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.04em;
  margin-bottom: 14px;
  line-height: 1.3;
}

.subtitle {
  font-size: 15px;
  color: var(--text-muted);
  margin-bottom: 50px;
  line-height: 1.5;
}

/* 插图 */
.illustration {
  width: 320px;
  height: 260px;
  position: relative;
}

.illu-base {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 220px;
  height: 140px;
  background: linear-gradient(135deg, var(--deco-base) 0%, var(--deco-base-light) 100%);
  border-radius: 24px;
  transform: translateX(-50%) perspective(400px) rotateX(15deg);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.06);
}

.illu-card {
  position: absolute;
  bottom: 70px;
  left: 50%;
  transform: translateX(-50%);
  width: 110px;
  height: 90px;
  background: linear-gradient(180deg, var(--deco-card) 0%, var(--deco-card-light) 100%);
  border-radius: 14px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  animation: float 4s ease-in-out infinite;
}

.card-dots {
  display: flex;
  gap: 6px;
}

.card-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--deco-dot-dark);
}

.card-dots span:nth-child(2) {background: var(--deco-dot-mid); width: 6px; height: 6px;}
.card-dots span:nth-child(3) {background: var(--deco-dot-light); width: 5px; height: 5px;}

.card-line {
  width: 60px;
  height: 6px;
  border-radius: 3px;
  background: linear-gradient(90deg, var(--deco-line), var(--deco-line-light));
  opacity: 0.6;
}

@keyframes float {
  0%, 100% {transform: translateX(-50%) translateY(0);}
  50% {transform: translateX(-50%) translateY(-10px);}
}

/* 浮动装饰 */
.deco {
  position: absolute;
  border-radius: 6px;
}

.deco-1 {
  top: 15px; right: 60px; width: 14px; height: 14px;
  background: linear-gradient(135deg, var(--deco-float-1), var(--deco-float-1-light)); opacity: 0.45;
  animation: float-deco 5s ease-in-out infinite;
}
.deco-2 {
  top: 80px; left: 25px; width: 10px; height: 10px;
  background: linear-gradient(135deg, var(--deco-float-2), var(--deco-float-2-light)); opacity: 0.35;
  animation: float-deco 4s ease-in-out infinite 0.5s;
}
.deco-3 {
  bottom: 120px; right: 20px; width: 12px; height: 12px;
  background: linear-gradient(135deg, var(--deco-float-3), var(--deco-float-3-light)); opacity: 0.3;
  animation: float-deco 6s ease-in-out infinite 1s;
}
.deco-4 {
  bottom: 80px; left: 55px; width: 8px; height: 8px;
  border-radius: 50%; background: var(--deco-float-4); opacity: 0.25;
  animation: float-deco 4.5s ease-in-out infinite 1.5s;
}
.deco-5 {
  top: 50%; right: 10px; width: 6px; height: 6px;
  border-radius: 50%; background: var(--deco-float-5); opacity: 0.2;
  animation: float-deco 5.5s ease-in-out infinite 2s;
}

@keyframes float-deco {
  0%, 100% {transform: translateY(0) rotate(0deg);}
  50% {transform: translateY(-12px) rotate(10deg);}
}

/* ===== 右侧 ===== */
.login-right {
  flex: 0 0 66.666%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  position: relative;
}

/* 浮动表单卡片 */
.login-form-card {
  background: var(--bg-primary);
  border-radius: 20px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
  padding: 36px 44px 48px;
  width: 420px;
  max-width: calc(100% - 48px);
}

.form-title {
  font-size: 26px;
  font-weight: 650;
  color: var(--text-primary);
  margin-bottom: 8px;
  letter-spacing: 0.03em;
}

.form-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 36px;
}

/* 表单 */
.login-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-group {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border: 1.5px solid var(--border-color);
  border-radius: 10px;
  transition: all 0.2s;
  background: transparent;
}

.input-group-code {
  padding-right: 8px;
}

.input-group-code > input {
  min-width: 0;
}

.input-group:focus-within {
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.1);
}

.input-icon {
  color: var(--text-muted);
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.input-group:focus-within .input-icon {
  color: var(--text-secondary);
}

.input-group > input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14.5px;
  color: var(--text-primary);
  background: transparent;
  font-family: inherit;
  line-height: 1.4;
}

.input-group > input::placeholder {
  color: var(--text-muted);
}

.btn-toggle-pwd {
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  padding: 2px;
  display: flex;
  align-items: center;
  flex-shrink: 0;
  transition: color 0.15s;
}

.btn-toggle-pwd:hover {
  color: var(--text-secondary);
}

.btn-send-code {
  flex-shrink: 0;
  border: 1.5px solid var(--border-color);
  background: transparent;
  color: var(--text-secondary);
  font-size: 12.5px;
  font-family: inherit;
  font-weight: 500;
  padding: 4px 14px;
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  height: 28px;
}

.btn-send-code:hover:not(:disabled) {
  border-color: var(--accent-color);
  color: var(--text-primary);
}

.btn-send-code:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* 提示信息 */
.error-msg {
  font-size: 13px;
  color: #e74c3c;
  margin: -4px 0 0;
  text-align: center;
}

.success-msg {
  font-size: 13px;
  color: #2ecc71;
  margin: -4px 0 0;
  text-align: center;
}

/* 注册按钮 */
.btn-login {
  width: 100%;
  padding: 13px;
  border: none;
  border-radius: 10px;
  background: var(--btn-primary-bg, #111);
  color: var(--btn-primary-text, #fff);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  letter-spacing: 0.04em;
  margin-top: 6px;
  transition: all 0.2s;
}

.btn-login:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.18);
}

.btn-login:active:not(:disabled) {
  transform: translateY(0);
}

.btn-login:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 登录链接 */
.register-link {
  font-size: 13.5px;
  color: var(--text-muted);
  margin-top: 28px;
  text-align: center;
}

.register-link a {
  color: var(--link-color);
  font-weight: 600;
  text-decoration: none;
  transition: color 0.15s;
}

.register-link a:hover {
  color: var(--text-secondary);
}

/* 主题切换按钮（悬浮） */
.btn-theme-float {
  position: fixed;
  top: 20px;
  right: 24px;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 1.5px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.btn-theme-float:hover {
  border-color: var(--accent-color);
  background: var(--hover-bg);
  color: var(--text-primary);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 页脚 */
.login-footer {
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
  padding: 40px 20px 20px;
  flex-shrink: 0;
}


/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .login-left {display: none;}
  .login-right {flex: 0 0 100%; background: var(--bg-secondary);}
  .login-form-card {width: 400px; padding: 36px 32px;}
}

@media (max-width: 480px) {
  .login-form-card {width: calc(100% - 24px); padding: 28px 24px; border-radius: 16px;}
}

</style>
