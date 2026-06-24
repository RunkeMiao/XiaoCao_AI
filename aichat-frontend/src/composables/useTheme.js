// src/composables/useTheme.js
import {ref, watch} from 'vue'

const isDark = ref(localStorage.getItem('theme') !== 'light')

function toggleTheme() {
    isDark.value = !isDark.value
}

watch(isDark, (val) => {
    localStorage.setItem('theme', val ? 'dark' : 'light')
    document.documentElement.setAttribute('data-theme', val ? 'dark' : 'light')
}, {immediate: true})

export function useTheme() {
    return {isDark, toggleTheme}
}
