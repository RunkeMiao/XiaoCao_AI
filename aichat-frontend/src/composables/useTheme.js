// src/composables/useTheme.js
import {ref, watch, onMounted, onUnmounted} from 'vue'

// 检测系统主题偏好
function getSystemTheme() {
    if (typeof window === 'undefined') return true // 默认值：深色
    return window.matchMedia('(prefers-color-scheme: dark)').matches
}

// 初始化主题：优先使用系统偏好
function initTheme() {
    return getSystemTheme()
}

// 初始化
const initialTheme = initTheme()
const isDark = ref(initialTheme)

// 立即设置初始主题
if (typeof document !== 'undefined') {
    document.documentElement.setAttribute('data-theme', initialTheme ? 'dark' : 'light')
}

// 监听系统主题变化（始终跟随系统）
let mediaQuery = null
let mediaQueryCleanup = null

function setupSystemThemeListener() {
    if (typeof window === 'undefined') return
    
    mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    
    console.log('[Theme] 系统主题监听已设置，当前系统主题:', mediaQuery.matches ? '深色' : '浅色')
    
    // 监听系统主题变化 - 始终跟随系统
    const handleChange = (e) => {
        console.log('[Theme] 检测到系统主题变化，更新为:', e.matches ? '深色' : '浅色')
        isDark.value = e.matches
    }
    
    mediaQuery.addEventListener('change', handleChange)
    
    // 返回清理函数
    return () => {
        mediaQuery.removeEventListener('change', handleChange)
    }
}

// 切换主题（临时覆盖，系统主题变化时会自动更新）
function toggleTheme() {
    isDark.value = !isDark.value
}

// 监听主题变化
watch(isDark, (val) => {
    // 设置 HTML 属性
    document.documentElement.setAttribute('data-theme', val ? 'dark' : 'light')
    
    console.log('[Theme] 主题已更新:', val ? '深色' : '浅色')
})

// 导出组合式函数
export function useTheme() {
    onMounted(() => {
        // 组件挂载时，设置系统主题监听
        mediaQueryCleanup = setupSystemThemeListener()
        
        // 组件卸载时，清理监听
        onUnmounted(() => {
            if (mediaQueryCleanup) {
                mediaQueryCleanup()
                mediaQueryCleanup = null
            }
        })
    })
    
    return {isDark, toggleTheme}
}
