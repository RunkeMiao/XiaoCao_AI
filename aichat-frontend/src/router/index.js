import {createRouter, createWebHistory} from 'vue-router'
import {useAuth} from '../composables/useAuth.js'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import ForgotPassword from '../views/ForgotPassword.vue'
import Chat from '../views/Chat.vue'

const routes = [
  {
    path: '/',
    name: 'chat',
    component: Chat
  },
  {
    path: '/login',
    name: 'login',
    component: Login
  },
  {
    path: '/register',
    name: 'register',
    component: Register
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: ForgotPassword
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const {isLoggedIn, checkLogin} = useAuth()
  await checkLogin()

  if (isLoggedIn.value) {
    // 已登录，禁止访问登录/注册页
    if (['login', 'register', 'forgot-password'].includes(to.name)) {
      next({name: 'chat'})
    } else {
      next()
    }
  } else {
    // 未登录，只允许访问登录/注册/忘记密码页
    if (['login', 'register', 'forgot-password'].includes(to.name)) {
      next()
    } else {
      next({name: 'login'})
    }
  }
})

export default router
