<template>
  <router-view />
</template>

<script setup>
// 根组件 - 路由容器
// 主题由 theme.css 和 useTheme.js 共同管理
import './composables/useTheme.js'
import './styles/theme.css'
</script>

<style>
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
  height: 100vh; /* fallback */
  height: 100dvh; /* 移动端动态视口，排除地址栏/底栏 */
  overflow: hidden;
}
</style>
