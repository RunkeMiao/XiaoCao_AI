import {ref, nextTick} from 'vue'
import {post, chatApi} from '../utils/request.js'

export function useChat() {
    const messages = ref([])
    const isStreaming = ref(false)
    const sessionId = ref('')
    const chatHistory = ref([])
    let abortController = null
    let currentReader = null

    /**
     * 初始化：从服务器加载会话列表
     */
    async function initSessions() {
        try {
            const response = await chatApi.getSessions()
            if (response.ok) {
                const data = await response.json()
                chatHistory.value = data.map(session => ({
                    id: session.sessionId,
                    title: session.title,
                    titleEdited: session.titleEdited,
                    time: new Date(session.updatedAt).toLocaleString('zh-CN', {
                        month: 'numeric',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                    }),
                    updatedAt: new Date(session.updatedAt).getTime()
                }))
            }
        } catch (e) {
            console.error('加载会话列表失败:', e)
        }

        // 默认显示欢迎界面
        sessionId.value = ''
        messages.value = []
    }

    /**
     * 加载会话消息
     */
    async function loadSessionMessages(id) {
        try {
            const response = await chatApi.getSessionMessages(id)
            if (response.ok) {
                const data = await response.json()
                messages.value = data.map(msg => ({
                    role: msg.role === 'assistant' ? 'ai' : msg.role,
                    content: msg.content
                }))
            } else {
                messages.value = []
            }
        } catch (e) {
            console.error('加载消息失败:', e)
            messages.value = []
        }
    }

    /**
     * 切换会话
     */
    async function switchSession(id) {
        if (isStreaming.value) return

        sessionId.value = id
        await loadSessionMessages(id)
    }

    /**
     * 新建对话（点击按钮）- 只跳转到欢迎界面，不创建会话
     */
    function newSession() {
        if (isStreaming.value) return

        // 清空当前状态，显示欢迎页面
        sessionId.value = ''
        messages.value = []
    }

    /**
     * 创建新会话（内部方法，发送消息时调用）
     */
    async function createNewSession() {
        try {
            const response = await chatApi.createSession()
            if (response.ok) {
                const data = await response.json()
                sessionId.value = data.sessionId

                // 更新历史列表
                chatHistory.value.unshift({
                    id: data.sessionId,
                    title: data.title,
                    titleEdited: data.titleEdited,
                    time: new Date(data.createdAt).toLocaleString('zh-CN', {
                        month: 'numeric',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                    }),
                    updatedAt: new Date(data.createdAt).getTime()
                })

                return data.sessionId
            }
        } catch (e) {
            console.error('创建会话失败:', e)
        }
        return null
    }

    /**
     * 删除会话
     */
    async function deleteSession(id) {
        try {
            const response = await chatApi.deleteSession(id)
            if (response.ok) {
                chatHistory.value = chatHistory.value.filter(h => h.id !== id)

                if (sessionId.value === id) {
                    // 删除的是当前会话，显示欢迎界面
                    sessionId.value = ''
                    messages.value = []
                }
            }
        } catch (e) {
            console.error('删除会话失败:', e)
        }
    }

    /**
     * 更新会话标题（用户手动编辑）
     */
    async function updateSessionTitle(id, title) {
        try {
            const response = await chatApi.updateSessionTitle(id, title)
            if (response.ok) {
                // 更新本地历史列表
                const item = chatHistory.value.find(h => h.id === id)
                if (item) {
                    item.title = title
                    item.titleEdited = true
                }
            }
        } catch (e) {
            console.error('更新会话标题失败:', e)
        }
    }

    /**
     * 发送消息
     */
    async function sendMessage(text) {
        if (isStreaming.value || !text.trim()) return

        // 如果没有当前会话，创建新会话
        if (!sessionId.value) {
            const sid = await createNewSession()
            if (!sid) {
                console.error('无法创建会话')
                return
            }
        }

        // 添加用户消息到界面
        messages.value.push({role: 'user', content: text})
        const aiIndex = messages.value.length
        messages.value.push({role: 'ai', content: ''})
        isStreaming.value = true

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
                if (done) {
                    // 流结束，处理buffer中剩余的数据
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
                    break
                }

                buffer += decoder.decode(value, {stream: true})

                while (true) {
                    const eventEnd = buffer.indexOf('\n\n')
                    if (eventEnd === -1) break

                    const event = buffer.substring(0, eventEnd)
                    buffer = buffer.substring(eventEnd + 2)

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

        } catch (e) {
            if (e.name === 'AbortError') {
                messages.value[aiIndex].content += '\n\n[已停止生成]'
            } else if (messages.value[aiIndex].content) {
                messages.value[aiIndex].content += '\n\n[生成结束]'
            } else {
                messages.value[aiIndex].content = '请求出错: ' + e.message
            }
        } finally {
            isStreaming.value = false
            abortController = null
            currentReader = null
        }

        // AI回复已由后端保存到数据库，前端只需生成标题
        if (messages.value.length === 2) {
            // 先设置临时标题，让用户立即看到
            const tempTitle = text.substring(0, 10) || '新对话'
            const item = chatHistory.value.find(h => h.id === sessionId.value)
            if (item && !item.titleEdited) {
                item.title = tempTitle
            }
            // 异步生成AI标题
            generateSmartTitle()
        }
    }

    /**
     * 智能生成会话标题（异步，不阻塞用户）
     */
    async function generateSmartTitle() {
        try {
            // 取前4条消息作为上下文
            const contextMessages = messages.value.slice(0, 4)
            const context = contextMessages.map(m =>
                `${m.role === 'user' ? '用户' : 'AI'}: ${m.content.substring(0, 200)}`
            ).join('\n')

            // 调用AI生成标题（通过后端API）
            const resp = await post('/api/sessions/' + sessionId.value + '/generate-title', {context})
            if (resp.ok) {
                const data = await resp.json()
                const title = data.title

                // 更新本地历史列表
                const item = chatHistory.value.find(h => h.id === sessionId.value)
                if (item && !item.titleEdited) {
                    item.title = title
                }
            }
        } catch (e) {
            console.error('生成标题失败:', e)
            // 降级策略已在sendMessage中设置临时标题，无需额外处理
        }
    }

    /**
     * 停止生成
     */
    function stopGeneration() {
        if (abortController) {
            abortController.abort()
        }
        if (currentReader) {
            currentReader.cancel()
        }
    }

    // 初始化
    initSessions()

    return {
        messages,
        isStreaming,
        sessionId,
        chatHistory,
        sendMessage,
        stopGeneration,
        newSession,
        switchSession,
        deleteSession,
        updateSessionTitle
    }
}
