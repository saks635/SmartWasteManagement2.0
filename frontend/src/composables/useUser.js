// useUser.js — Simple shared state for the logged-in user
// Think of this as a tiny "global variable" that all components can read
import { reactive } from 'vue'
import { API_BASE } from '../config.js'

// This object is shared across all components
// When it changes, any component using it automatically updates
export const user = reactive({
  email: '',
  role: ''   // 'USER' | 'ADMIN' | 'WORKER' | 'PENDING' | '' (not logged in)
})

// Fetch current user from Spring Boot
// Called on app startup and after login
export async function fetchUser() {
  try {
    const res = await fetch(API_BASE + '/api/auth/me', { credentials: 'include' })
    if (res.ok) {
      const data = await res.json()
      user.email = data.email
      user.role  = data.role
      return true  // ← logged in
    }
  } catch (e) { /* network error */ }
  user.email = ''
  user.role  = ''
  return false  // ← not logged in
}

// Clear user state (called after logout)
export function clearUser() {
  user.email = ''
  user.role  = ''
}
