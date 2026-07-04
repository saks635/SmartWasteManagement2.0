<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router   = useRouter()
const username = ref('')
const password = ref('')
const role     = ref('USER')  // Default role
const error    = ref('')
const success  = ref('')
const loading  = ref(false)

async function register() {
  error.value   = ''
  success.value = ''
  loading.value = true

  // Send registration data as JSON to Spring Boot
  const res = await fetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: username.value,
      password: password.value,
      role: role.value
    }),
    credentials: 'include',
  })

  loading.value = false
  const data = await res.json()

  if (res.ok) {
    success.value = data.message
    // Auto-redirect to login after 1.5 seconds
    setTimeout(() => router.push('/login'), 1500)
  } else {
    error.value = data.error || 'Registration failed'
  }
}
</script>

<template>
  <div class="min-h-screen bg-[#0a192f] flex items-center justify-center p-4">
    <div class="w-full max-w-md bg-[#112240] rounded-2xl p-8 border border-[#1d3461] shadow-2xl">

      <!-- Header -->
      <div class="text-center mb-8">
        <div class="text-4xl mb-3">📝</div>
        <h1 class="text-2xl font-bold text-[#ccd6f6]">Create Account</h1>
        <p class="text-[#8892b0] text-sm mt-1">Join the Waste Management System</p>
      </div>

      <!-- Success message -->
      <div v-if="success" class="mb-5 p-3 rounded-lg bg-green-900/30 border border-green-700 text-green-400 text-sm">
        ✅ {{ success }}
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
          placeholder="Username"
          class="w-full bg-[#0a192f] border border-[#1d3461] text-[#ccd6f6] rounded-xl px-4 py-3 focus:outline-none focus:border-[#2196f3] placeholder-[#8892b0]"
        />
        <input
          v-model="password"
          type="password"
          placeholder="Password"
          class="w-full bg-[#0a192f] border border-[#1d3461] text-[#ccd6f6] rounded-xl px-4 py-3 focus:outline-none focus:border-[#2196f3] placeholder-[#8892b0]"
        />

        <!-- Role Selector -->
        <div>
          <p class="text-[#8892b0] text-sm mb-2">Select your role:</p>
          <div class="grid grid-cols-3 gap-2">
            <button v-for="r in ['USER', 'WORKER', 'ADMIN']" :key="r"
              @click="role = r"
              :class="role === r
                ? 'bg-[#2196f3] text-white border-[#2196f3]'
                : 'bg-[#0a192f] text-[#8892b0] border-[#1d3461] hover:border-[#64b5f6]'"
              class="py-2 rounded-xl border text-sm font-medium transition-colors">
              {{ r === 'USER' ? '🧑 Citizen' : r === 'WORKER' ? '👷 Worker' : '🛡️ Admin' }}
            </button>
          </div>
        </div>

        <button
          @click="register"
          :disabled="loading"
          class="w-full bg-[#2196f3] hover:bg-[#1976d2] disabled:opacity-50 text-white font-semibold rounded-xl py-3 transition-colors">
          {{ loading ? 'Creating account...' : 'Register' }}
        </button>
      </div>

      <p class="text-center text-[#8892b0] text-sm mt-6">
        Already have an account?
        <RouterLink to="/login" class="text-[#64b5f6] hover:underline">Login</RouterLink>
      </p>
    </div>
  </div>
</template>
