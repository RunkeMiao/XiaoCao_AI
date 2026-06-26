const BASE_URL = ''

let refreshing = false
let refreshQueue = []
let redirecting = false

/**
 * 尝试刷新 Token
 */
async function tryRefresh() {
  if (refreshing) {
    // 等待正在进行的刷新完成
    return new Promise((resolve) => refreshQueue.push(resolve))
  }
  refreshing = true
  try {
    const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
    })
    const data = await res.json()
    const success = data.code === 200
    refreshing = false
    refreshQueue.forEach(resolve => resolve(success))
    refreshQueue = []
    return success
  } catch {
    refreshing = false
    refreshQueue.forEach(resolve => resolve(false))
    refreshQueue = []
    return false
  }
}

/**
 * 封装 fetch，自动携带 Cookie，401 时自动刷新
 */
export async function request(url, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  }

  let res = await fetch(`${BASE_URL}${url}`, {
    ...options,
    headers,
    credentials: 'include',
  })

  // 401 时尝试刷新 Token
  if (res.status === 401) {
    const refreshed = await tryRefresh()
    if (refreshed) {
      // 刷新成功，重试原请求
      res = await fetch(`${BASE_URL}${url}`, {
        ...options,
        headers,
        credentials: 'include',
      })
    } else if (!redirecting) {
      // 刷新失败，跳转登录（只跳转一次）
      redirecting = true
      window.location.href = '/login'
      return res
    }
  }

  return res
}

export function get(url) {
  return request(url, { method: 'GET' })
}

export function post(url, data) {
  return request(url, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}
