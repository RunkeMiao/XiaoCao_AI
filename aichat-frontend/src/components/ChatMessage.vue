<!-- src/components/ChatMessage.vue -->
<template>
  <div class="message-row" :class="msg.role">
    <div class="avatar" :class="msg.role + '-avatar'">
      <template v-if="msg.role === 'ai'">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
        </svg>
      </template>
      <template v-else>U</template>
    </div>
    <div class="bubble" :class="msg.role">
      <div class="content">
        <span class="text">{{ msg.content }}</span>
        <span v-if="streaming" class="cursor"></span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  msg: {type: Object, required: true},
  streaming: {type: Boolean, default: false}
})
</script>

<style scoped>
.message-row {
  display: flex;
  gap: 12px;
  animation: fadeUp 0.35s ease;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.message-row.user {
  flex-direction: row-reverse;
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

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
  overflow: hidden;
}

.ai-logo-img {
  width: 22px;
  height: 22px;
  object-fit: contain;
}

.ai-avatar {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
}

.user-avatar {
  background: linear-gradient(135deg, #059669, #10b981);
  color: white;
}

.bubble {
  max-width: 75%;
  padding: 12px 18px;
  border-radius: 18px;
  font-size: 14.5px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble.ai {
  background: var(--ai-bubble);
  border: 1px solid var(--border-color);
  border-top-left-radius: 4px;
  color: var(--text-primary);
}

.bubble.user {
  background: var(--accent-color);
  color: white;
  border-top-right-radius: 4px;
}

.cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: var(--accent-color);
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
</style>
