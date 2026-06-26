// src/composables/useTheme.js
// 主题管理：始终跟随系统主题，手动切换为临时覆盖
// 系统主题变化时自动切换，手动切换仅在当前会话有效
import {ref, watch} from 'vue'

// 检测系统主题偏好
function getSystemTheme() {
    if (typeof window === 'undefined') return false
    return window.matchMedia('(prefers-color-scheme: dark)').matches
}

// 初始化：始终使用系统主题
const isDark = ref(getSystemTheme())

// 立即同步到 DOM
function applyTheme(dark) {
    if (typeof document !== 'undefined') {
        document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
    }
}

applyTheme(isDark.value)

// 系统主题变化监听（始终生效）
if (typeof window !== 'undefined') {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    const systemChangeHandler = (e) => {
        isDark.value = e.matches
    }
    mediaQuery.addEventListener('change', systemChangeHandler)
}

// 手动切换主题（临时覆盖，系统主题变化时会自动覆盖回来）
function toggleTheme() {
    isDark.value = !isDark.value
}

// 监听 isDark 变化，同步到 DOM
watch(isDark, (val) => {
    applyTheme(val)
})

export function useTheme() {
    return {isDark, toggleTheme}
}
