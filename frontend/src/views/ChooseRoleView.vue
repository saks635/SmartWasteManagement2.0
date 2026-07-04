<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchUser, user } from '../composables/useUser.js'

const router  = useRouter()
const selected = ref('USER')  // Which role card is selected
const loading  = ref(false)
const error    = ref('')

async function submitRole() {
  loading.value = true
  error.value   = ''

  // Send chosen role to Spring Boot
  const res = await fetch('/api/auth/choose-role', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ role: selected.value }),
    credentials: 'include',
  })

  loading.value = false

  if (res.ok) {
    // Refresh user data, then go to the right dashboard
    await fetchUser()
    if (user.role === 'ADMIN')   router.push('/admin')
    else if (user.role === 'WORKER') router.push('/worker')
    else router.push('/')
  } else {
    error.value = 'Something went wrong. Please try again.'
  }
}

// Role options with icons and descriptions
const roles = [
  { value: 'USER',   icon: '🧑‍💼', label: 'Citizen',  desc: 'Report waste issues in your area' },
  { value: 'WORKER', icon: '👷',   label: 'Worker',   desc: 'Handle assigned cleaning tasks' },
  { value: 'ADMIN',  icon: '🛡️',  label: 'Admin',    desc: 'Manage complaints and workers' },
]
</script>

<template>
  <div class="min-h-screen bg-[#0a192f] flex items-center justify-center p-4">
    <div class="w-full max-w-lg bg-[#112240] rounded-2xl p-8 border border-[#1d3461] shadow-2xl text-center">

      <div class="text-4xl mb-4">🎉</div>
      <h1 class="text-2xl font-bold text-[#ccd6f6]">One last step!</h1>
      <p class="text-[#8892b0] text-sm mt-2 mb-8">Choose how you'll use this platform</p>

      <div v-if="error" class="mb-5 p-3 rounded-lg bg-red-900/30 border border-red-700 text-red-400 text-sm">
        ⚠️ {{ error }}
      </div>

      <!-- Role cards -->
      <div class="grid grid-cols-3 gap-3 mb-8">
        <button
          v-for="r in roles" :key="r.value"
          @click="selected = r.value"
          :class="selected === r.value
            ? 'border-[#2196f3] bg-[#1a3a5c]'
            : 'border-[#1d3461] hover:border-[#64b5f6]'"
          class="flex flex-col items-center gap-2 p-4 rounded-xl border transition-all">
          <span class="text-3xl">{{ r.icon }}</span>
          <span class="font-semibold text-[#ccd6f6] text-sm">{{ r.label }}</span>
          <span class="text-[#8892b0] text-xs">{{ r.desc }}</span>
        </button>
      </div>

      <button
        @click="submitRole"
        :disabled="loading"
        class="w-full bg-[#2196f3] hover:bg-[#1976d2] disabled:opacity-50 text-white font-semibold rounded-xl py-3 transition-colors">
        {{ loading ? 'Saving...' : 'Continue →' }}
      </button>

      <p class="text-xs text-[#8892b0] mt-4">
        ⚠️ You won't be able to change your role later without contacting an admin.
      </p>
    </div>
  </div>
</template>
