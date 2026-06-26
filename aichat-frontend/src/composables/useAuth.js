import {ref} from 'vue'
import {get, post} from '../utils/request.js'

const username = ref('')
const isLoggedIn = ref(false)
const checked = ref(false)

/**
 * 登录状态管理（基于 HttpOnly Cookie）
 */
export function useAuth() {

  async function checkLogin() {
    if (checked.value) return isLoggedIn.value
    try {
      const res = await get('/api/auth/me')
      const data = await res.json()
      if (data.code === 200) {
        isLoggedIn.value = true
        username.value = data.username || ''
      } else {
        isLoggedIn.value = false
        username.value = ''
      }
    } catch {
      isLoggedIn.value = false
      username.value = ''
    }
    checked.value = true
    return isLoggedIn.value
  }

  function setLogin(newUsername) {
    isLoggedIn.value = true
    username.value = newUsername || ''
    checked.value = true
  }

  async function logout() {
    console.log('logout() called')
    try {
      const res = await post('/api/auth/logout', {})
      console.log('Logout response:', res.status)
    } catch (e) {
      console.error('Logout request failed:', e)
    }
    // 无论请求是否成功，都清除本地状态
    console.log('Clearing local state...')
    isLoggedIn.value = false
    username.value = ''
    checked.value = false
    console.log('Redirecting to /login...')
    window.location.href = '/login'
  }

  return {
    username,
    isLoggedIn,
    checked,
    checkLogin,
    setLogin,
    logout,
  }
}
