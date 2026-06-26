<!-- src/components/ChatMessage.vue -->
<template>
  <div class="message-row" :class="msg.role">
    <div class="bubble" :class="msg.role">
      <div class="text markdown-body" v-html="renderedContent"></div>
      <span v-if="streaming" class="cursor"></span>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import DOMPurify from 'dompurify'
// highlight.js CSS 通过 theme.css 的 CSS 变量控制，不在这里硬编码导入

// 配置 marked：换行、GFM、代码高亮
marked.setOptions({
  breaks: true,
  gfm: true,
  highlight(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  }
})

const props = defineProps({
  msg: { type: Object, required: true },
  streaming: { type: Boolean, default: false }
})

// 使用 ref 存储解析后的 HTML
const renderedContent = ref('')

// 监听内容变化，异步解析 Markdown
watch(
  () => props.msg.content,
  async (newContent) => {
    if (!newContent) {
      renderedContent.value = ''
      return
    }
    try {
      const rawHtml = await marked.parse(newContent)
      renderedContent.value = DOMPurify.sanitize(rawHtml)
    } catch (e) {
      console.error('Markdown parse error:', e)
      renderedContent.value = '<p>' + newContent.replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</p>'
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.message-row {
  display: flex;
  animation: fadeUp 0.35s ease;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.message-row.user {
  justify-content: flex-end;
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.bubble {
  max-width: 85%;
  padding: 16px 22px;
  border-radius: 16px;
  font-size: 14.5px;
  line-height: 1.75;
  word-break: break-word;
}

.bubble.ai {
  background: var(--bubble-ai-bg);
  color: var(--text-primary);
}

.bubble.user {
  background: var(--bubble-user-bg);
  color: var(--bubble-user-text);
  border-radius: 40px;
}

.cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: #999;
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 0.8s infinite;
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

/* ===== Markdown 样式 ===== */
.markdown-body {
  word-break: break-word;
  line-height: 1.8;
}

/* 段落 */
.markdown-body :deep(p) {
  margin: 0 0 8px;
}
.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

/* 标题 */
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  margin: 16px 0 8px;
  font-weight: 600;
}
.markdown-body :deep(h1) { font-size: 1.4em; }
.markdown-body :deep(h2) { font-size: 1.25em; }
.markdown-body :deep(h3) { font-size: 1.1em; }
.markdown-body :deep(h4) { font-size: 1em; }

/* 加粗 / 斜体 */
.markdown-body :deep(strong) { font-weight: 600; }
.markdown-body :deep(em) { font-style: italic; }

/* 行内代码 */
.markdown-body :deep(code) {
  background: var(--inline-code-bg);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  color: var(--inline-code-color);
}

/* 代码块 */
.markdown-body :deep(pre) {
  background: var(--code-block-bg);
  border-radius: 10px;
  padding: 16px 20px;
  overflow-x: auto;
  margin: 12px 0;
  border: 1px solid var(--code-block-border);
}
.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
  font-size: 13.2px;
  line-height: 1.65;
  color: var(--code-block-color);
}
/* 覆盖 highlight.js 默认颜色，确保跟随主题 */
.markdown-body :deep(.hljs) {
  background: var(--code-block-bg) !important;
  color: var(--code-block-color) !important;
}
.markdown-body :deep(.hljs-keyword) { color: var(--hljs-keyword) !important; }
.markdown-body :deep(.hljs-string) { color: var(--hljs-string) !important; }
.markdown-body :deep(.hljs-comment) { color: var(--hljs-comment) !important; font-style: italic; }
.markdown-body :deep(.hljs-number) { color: var(--hljs-number) !important; }
.markdown-body :deep(.hljs-built_in) { color: var(--hljs-builtin) !important; }
.markdown-body :deep(.hljs-title) { color: var(--hljs-title) !important; }
.markdown-body :deep(.hljs-params) { color: var(--hljs-params) !important; }
.markdown-body :deep(.hljs-attr) { color: var(--hljs-attr) !important; }
.markdown-body :deep(.hljs-type) { color: var(--hljs-type) !important; }

/* 列表 */
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 24px;
  margin: 8px 0;
}
.markdown-body :deep(li) {
  margin: 4px 0;
}

/* 引用块 */
.markdown-body :deep(blockquote) {
  border-left: 3px solid var(--blockquote-border);
  padding-left: 14px;
  margin: 10px 0;
  color: var(--text-secondary);
}

/* 表格 */
.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 13.5px;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid var(--border-color);
  padding: 8px 14px;
  text-align: left;
}
.markdown-body :deep(th) {
  background: var(--table-head-bg);
  color: var(--table-head-text);
  font-weight: 600;
}
.markdown-body :deep(td) {
  border-color: var(--table-border);
}
.markdown-body :deep(tr:nth-child(even)) {
  background: var(--table-row-even);
}

/* 分割线 */
.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color);
  margin: 14px 0;
}

/* 链接 */
.markdown-body :deep(a) {
  color: var(--link-color);
  text-decoration: none;
}
.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

/* 图片 */
.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}
</style>
