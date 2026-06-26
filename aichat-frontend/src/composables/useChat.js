import {ref, nextTick, watch} from 'vue'
import {post} from '../utils/request.js'

export function useChat() {
    const messages = ref([])
    const isStreaming = ref(false)
    const sessionId = ref('')
    // 当前请求的 AbortController（用于终止请求）
    let abortController = null
    // 当前 reader（用于终止流式读取）
    let currentReader = null
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

        // 创建 AbortController 用于终止请求
        abortController = new AbortController()

        await nextTick()

        try {
            const resp = await post('/api/chat', {
                sessionId: sessionId.value,
                message: text
            })

            if (!resp.ok) {
                const data = await resp.json().catch(() => null)
                throw new Error(data?.message || `请求失败 (${resp.status})`)
            }

            currentReader = resp.body.getReader()
            const decoder = new TextDecoder()
            let buffer = ''

            while (true) {
                const {done, value} = await currentReader.read()
                if (done) break

                buffer += decoder.decode(value, {stream: true})

                // SSE 事件以 \n\n 分隔，正确处理包含换行符的内容
                while (true) {
                    const eventEnd = buffer.indexOf('\n\n')
                    if (eventEnd === -1) break

                    const event = buffer.substring(0, eventEnd)
                    buffer = buffer.substring(eventEnd + 2)

                    // 提取事件中所有 data: 行，用 \n 拼接（SSE 多行 data 规范）
                    const dataParts = []
                    for (const line of event.split('\n')) {
                        if (line.startsWith('data:')) {
                            dataParts.push(line.slice(5))
                        }
                    }
                    if (dataParts.length > 0) {
                        messages.value[aiIndex].content += dataParts.join('\n')
                    }
                }
            }

            // 处理缓冲区中剩余的数据
            if (buffer.trim()) {
                const dataParts = []
                for (const line of buffer.split('\n')) {
                    if (line.startsWith('data:')) {
                        dataParts.push(line.slice(5))
                    }
                }
                if (dataParts.length > 0) {
                    messages.value[aiIndex].content += dataParts.join('\n')
                }
            }

            // 保存消息到本地存储
            saveCurrentMessages()

        } catch (e) {
            // 如果是用户主动终止，不显示错误
            if (e.name === 'AbortError') {
                messages.value[aiIndex].content += '\n\n[已停止生成]'
            } else if (messages.value[aiIndex].content) {
                // 已有内容，说明数据已收到，流异常结束，不覆盖
                messages.value[aiIndex].content += '\n\n[生成结束]'
            } else {
                messages.value[aiIndex].content = '请求出错: ' + e.message
            }
        } finally {
            isStreaming.value = false
            abortController = null
            currentReader = null
        }
    }

    // 停止 AI 生成
    function stopGeneration() {
        if (abortController) {
            abortController.abort()
        }
        if (currentReader) {
            currentReader.cancel()
        }
    }

    // 页面关闭前保存
    if (typeof window !== 'undefined') {
        window.addEventListener('beforeunload', saveCurrentMessages)
    }

    initSession()

    return {messages, isStreaming, sessionId, chatHistory, sendMessage, stopGeneration, newSession, switchSession, deleteSession}
}
