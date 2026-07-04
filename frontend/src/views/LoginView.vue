<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchUser } from '../composables/useUser.js'

const router   = useRouter()
const username = ref('')   // Bound to the username input
const password = ref('')   // Bound to the password input
const error    = ref('')   // Shows error message if login fails
const loading  = ref(false)

async function login() {
  error.value   = ''
  loading.value = true

  // Send username + password to Spring Boot as form data (required by Spring Security)
  const body = new URLSearchParams({ username: username.value, password: password.value })

  const res = await fetch('/api/auth/login', {
    method: 'POST',
    body,
    credentials: 'include',  // include session cookie
  })

  loading.value = false

  if (res.ok) {
    const data = await res.json()
    // Refresh user state, then redirect to the right dashboard
    await fetchUser()
    if (data.role === 'ADMIN')   router.push('/admin')
    else if (data.role === 'WORKER') router.push('/worker')
    else router.push('/')
  } else {
    const data = await res.json()
    error.value = data.error || 'Login failed'
  }
}
</script>

<template>
  <!-- Full-screen dark background -->
  <div class="min-h-screen bg-[#0a192f] flex items-center justify-center p-4">

    <!-- Login Card -->
    <div class="w-full max-w-md bg-[#112240] rounded-2xl p-8 border border-[#1d3461] shadow-2xl">

      <!-- Header -->
      <div class="text-center mb-8">
        <div class="text-4xl mb-3">♻️</div>
        <h1 class="text-2xl font-bold text-[#ccd6f6]">Welcome Back</h1>
        <p class="text-[#8892b0] text-sm mt-1">Smart Waste Management System</p>
      </div>

      <!-- Error message -->
      <div v-if="error" class="mb-5 p-3 rounded-lg bg-red-900/30 border border-red-700 text-red-400 text-sm">
        ⚠️ {{ error }}
      </div>

      <!-- Form -->
      <div class="space-y-4">
        <input
          v-model="username"
          type="text"
          placeholder="Username or Email"
          class="w-full bg-[#0a192f] border border-[#1d3461] text-[#ccd6f6] rounded-xl px-4 py-3 focus:outline-none focus:border-[#2196f3] placeholder-[#8892b0]"
        />
        <input
          v-model="password"
          type="password"
          placeholder="Password"
          @keyup.enter="login"
          class="w-full bg-[#0a192f] border border-[#1d3461] text-[#ccd6f6] rounded-xl px-4 py-3 focus:outline-none focus:border-[#2196f3] placeholder-[#8892b0]"
        />
        <button
          @click="login"
          :disabled="loading"
          class="w-full bg-[#2196f3] hover:bg-[#1976d2] disabled:opacity-50 text-white font-semibold rounded-xl py-3 transition-colors">
          {{ loading ? 'Logging in...' : 'Login' }}
        </button>
      </div>

      <!-- Divider -->
      <div class="flex items-center my-6">
        <div class="flex-1 h-px bg-[#1d3461]"></div>
        <span class="px-4 text-[#8892b0] text-sm">OR</span>
        <div class="flex-1 h-px bg-[#1d3461]"></div>
      </div>

      <!-- Google OAuth2 button — goes DIRECTLY to Spring Boot (not through Vite proxy)
           This is needed because OAuth2 session handling doesn't work through a proxy -->
      <a href="http://localhost:8080/oauth2/authorization/google"
        class="flex items-center justify-center gap-3 w-full border border-[#1d3461] text-[#ccd6f6] rounded-xl py-3 hover:bg-[#1d3461] transition-colors">
        <svg width="20" height="20" viewBox="0 0 24 24">
          <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
          <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
          <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
          <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
        </svg>
        Continue with Google
      </a>

      <p class="text-center text-[#8892b0] text-sm mt-6">
        No account?
        <RouterLink to="/register" class="text-[#64b5f6] hover:underline">Register here</RouterLink>
      </p>
    </div>
  </div>
</template>
