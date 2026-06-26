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
    if (['login', 'register', 'forgot-password'].includes(to.name)) {
      next('/')
    } else {
      next()
    }
  } else {
    if (to.name === 'chat') {
      next('/login')
    } else {
      next()
    }
  }
})

export default router
