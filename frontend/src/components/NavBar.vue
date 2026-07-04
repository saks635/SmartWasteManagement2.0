<script setup>
import { useRouter } from 'vue-router'
import { user, clearUser } from '../composables/useUser.js'

const router = useRouter()

async function logout() {
  await fetch('/logout', { method: 'POST', credentials: 'include' })
  clearUser()
  router.push('/login')
}
</script>

<template>
  <nav class="flex items-center justify-between px-6 py-4 bg-[#112240] border-b border-[#1d3461]">
    <div class="flex items-center gap-3">
      <span class="text-xl font-bold text-[#64b5f6]">♻️ WasteManager</span>
      <span class="text-xs px-2 py-1 rounded-full bg-[#1d3461] text-[#64b5f6] font-medium uppercase tracking-wider">
        {{ user.role }}
      </span>
    </div>
    <div class="flex items-center gap-4">
      <span class="text-sm text-[#8892b0] hidden sm:block">{{ user.email }}</span>
      <button @click="logout"
        class="text-sm px-4 py-2 rounded-lg border border-[#1d3461] text-[#8892b0] hover:text-red-400 hover:border-red-400 transition-colors">
        Logout
      </button>
    </div>
  </nav>
</template>
