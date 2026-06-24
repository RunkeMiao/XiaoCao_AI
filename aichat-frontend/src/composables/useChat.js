import {ref, nextTick, watch} from 'vue'

export function useChat() {
    const messages = ref([])
    const isStreaming = ref(false)
    const sessionId = ref('')
    // 对话历史列表
    const chatHistory = ref(JSON.parse(localStorage.getItem('chat-history') || '[]'))

    function initSession() {
        let id = localStorage.getItem('chat-session-id')
        if (!id) {
            id = 's_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8)
            localStorage.setItem('chat-session-id', id)
        }
        sessionId.value = id
        loadHistoryMessages(id)
    }

    function loadHistoryMessages(id) {
        const stored = localStorage.getItem('chat-messages-' + id)
        if (stored) {
            messages.value = JSON.parse(stored)
        } else {
            messages.value = []
        }
    }

    function saveCurrentMessages() {
        if (sessionId.value) {
            localStorage.setItem('chat-messages-' + sessionId.value, JSON.stringify(messages.value))
        }
    }

    function addChatHistory(title) {
        const exists = chatHistory.value.find(h => h.id === sessionId.value)
        if (!exists) {
            chatHistory.value.unshift({
                id: sessionId.value,
                title: title || '新对话',
                time: new Date().toLocaleString('zh-CN', {
                    month: 'numeric',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit'
                }),
                updatedAt: Date.now()
            })
            // 只保留最近 20 条
            if (chatHistory.value.length > 20) chatHistory.value.pop()
            persistHistory()
        }
    }

    function updateChatHistoryTitle(title) {
        const item = chatHistory.value.find(h => h.id === sessionId.value)
        if (item && !item.titleEdited) {
            item.title = title
            item.titleEdited = true
            persistHistory()
        }
    }

    function persistHistory() {
        localStorage.setItem('chat-history', JSON.stringify(chatHistory.value))
    }

    function switchSession(id) {
        saveCurrentMessages()
        sessionId.value = id
        localStorage.setItem('chat-session-id', id)
        loadHistoryMessages(id)
    }

    function deleteSession(id) {
        chatHistory.value = chatHistory.value.filter(h => h.id !== id)
        localStorage.removeItem('chat-messages-' + id)
        persistHistory()
        if (sessionId.value === id) {
            newSession()
        }
    }

    function newSession() {
        saveCurrentMessages()
        sessionId.value = 's_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8)
        localStorage.setItem('chat-session-id', sessionId.value)
        messages.value = []
    }

    async function sendMessage(text) {
        if (isStreaming.value || !text.trim()) return

        // 首条消息时添加到历史
        if (messages.value.length === 0) {
            addChatHistory(text.substring(0, 30))
        }
        updateChatHistoryTitle(text.substring(0, 30))

        messages.value.push({role: 'user', content: text})
        const aiIndex = messages.value.length
        messages.value.push({role: 'ai', content: ''})
        isStreaming.value = true

        await nextTick()

        try {
            const resp = await fetch('/api/chat', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    sessionId: sessionId.value,
                    message: text
                })
            })

            const reader = resp.body.getReader()
            const decoder = new TextDecoder()
            let buffer = ''

            while (true) {
                const {done, value} = await reader.read()
                if (done) break

                buffer += decoder.decode(value, {stream: true})
                const lines = buffer.split('\n')
                buffer = lines.pop() || ''

                for (const line of lines) {
                    const trimmed = line.trim()
                    if (trimmed.startsWith('data:')) {
                        const token = trimmed.substring(5)
                        if (token) {
                            messages.value[aiIndex].content += token
                        }
                    }
                }
            }

            if (buffer.trim().startsWith('data:')) {
                const token = buffer.trim().substring(5)
                if (token) {
                    messages.value[aiIndex].content += token
                }
            }

            // 保存消息到本地存储
            saveCurrentMessages()

        } catch (e) {
            messages.value[aiIndex].content = '请求出错: ' + e.message
        } finally {
            isStreaming.value = false
        }
    }

    // 页面关闭前保存
    if (typeof window !== 'undefined') {
        window.addEventListener('beforeunload', saveCurrentMessages)
    }

    initSession()

    return {messages, isStreaming, sessionId, chatHistory, sendMessage, newSession, switchSession, deleteSession}
}
