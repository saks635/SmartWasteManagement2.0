import { createRouter, createWebHistory } from 'vue-router'
import { fetchUser, user } from '../composables/useUser.js'

// Import all page components
import LoginView      from '../views/LoginView.vue'
import RegisterView   from '../views/RegisterView.vue'
import ChooseRoleView from '../views/ChooseRoleView.vue'
import CitizenView    from '../views/CitizenView.vue'
import AdminView      from '../views/AdminView.vue'
import WorkerView     from '../views/WorkerView.vue'
import WorkerMapView  from '../views/WorkerMapView.vue'

// Define all app routes
const routes = [
  { path: '/login',       component: LoginView },
  { path: '/register',    component: RegisterView },
  { path: '/choose-role', component: ChooseRoleView, meta: { requiresAuth: true } },
  { path: '/',            component: CitizenView,    meta: { requiresAuth: true, role: 'USER'   } },
  { path: '/admin',       component: AdminView,      meta: { requiresAuth: true, role: 'ADMIN'  } },
  { path: '/worker',      component: WorkerView,     meta: { requiresAuth: true, role: 'WORKER' } },
  { path: '/admin/worker/:username', component: WorkerMapView, meta: { requiresAuth: true, role: 'ADMIN' } },
]

const router = createRouter({
  history: createWebHistory(), // Uses real URLs like /login, /admin (not #/login)
  routes
})

// Navigation Guard — runs before every page change
// It checks: are you logged in? Do you have the right role?
router.beforeEach(async (to) => {
  // Step 1: If we don't know who the user is yet, ask Spring Boot
  if (!user.role) {
    await fetchUser()
  }

  // Step 2: If the page requires auth and user is NOT logged in → go to login
  if (to.meta.requiresAuth && !user.role) {
    return '/login'
  }

  // Step 3: If user is PENDING (new OAuth2 user), force them to choose a role
  if (user.role === 'PENDING' && to.path !== '/choose-role') {
    return '/choose-role'
  }

  // Step 4: If the page needs a specific role and the user has a different one,
  // redirect them to THEIR correct dashboard
  if (to.meta.role && user.role !== to.meta.role && user.role !== 'PENDING') {
    if (user.role === 'ADMIN')  return '/admin'
    if (user.role === 'WORKER') return '/worker'
    return '/'
  }

  // Step 5: If user is already logged in and goes to /login or /register → skip, go home
  if ((to.path === '/login' || to.path === '/register') && user.role && user.role !== 'PENDING') {
    if (user.role === 'ADMIN')  return '/admin'
    if (user.role === 'WORKER') return '/worker'
    return '/'
  }
})

export default router
