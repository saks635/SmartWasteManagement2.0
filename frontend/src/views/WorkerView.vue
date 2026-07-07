<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { user } from '../composables/useUser.js'
import NavBar from '../components/NavBar.vue'
import { API_BASE } from '../config.js'

const complaints = ref([])
const loading    = ref(true)
const gpsActive  = ref(false)
const gpsStatus  = ref('GPS tracking inactive')
let gpsInterval  = null

// Load complaints assigned to this worker
onMounted(async () => {
  const res = await fetch(API_BASE + '/api/worker/complaints', { credentials: 'include' })
  if (res.ok) complaints.value = await res.json()
  loading.value = false
})

// Start sending GPS location to Spring Boot every 30 seconds
function startGPS() {
  if (!navigator.geolocation) {
    gpsStatus.value = '❌ GPS not supported by your browser'
    return
  }
  gpsActive.value = true
  gpsStatus.value = '🟢 GPS tracking active — sending location every 30s'

  // Send location immediately once, then every 30s
  sendLocation()
  gpsInterval = setInterval(sendLocation, 30000)
}

function stopGPS() {
  gpsActive.value = false
  gpsStatus.value = '⛔ GPS tracking stopped'
  clearInterval(gpsInterval)
}

function sendLocation() {
  navigator.geolocation.getCurrentPosition(async (pos) => {
    await fetch(API_BASE + '/api/worker/location', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      credentials: 'include',
    })
  })
}

// Mark a complaint as cleaned
async function markCleaned(id) {
  await fetch(API_BASE + '/api/worker/clean', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ complaintId: id }),
    credentials: 'include',
  })
  // Refresh list
  const res = await fetch(API_BASE + '/api/worker/complaints', { credentials: 'include' })
  if (res.ok) complaints.value = await res.json()
}

// Status badge color
function statusColor(status) {
  if (status === 'CLEANED')  return 'bg-green-900/40 text-green-400 border-green-700'
  if (status === 'ASSIGNED') return 'bg-blue-900/40 text-blue-400 border-blue-700'
  return 'bg-yellow-900/40 text-yellow-400 border-yellow-700'
}

// Clean up GPS interval when leaving the page
onUnmounted(() => clearInterval(gpsInterval))
</script>

<template>
  <div class="min-h-screen bg-[#0a192f]">
    <NavBar />

    <div class="max-w-4xl mx-auto p-6">
      <!-- Header -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between mb-6 gap-4">
        <div>
          <h1 class="text-2xl font-bold text-[#ccd6f6]">Worker Dashboard</h1>
          <p class="text-[#8892b0] text-sm">Welcome, {{ user.email }}</p>
        </div>

        <!-- GPS tracking controls -->
        <div class="bg-[#112240] rounded-xl p-4 border border-[#1d3461]">
          <p class="text-xs text-[#8892b0] mb-2">{{ gpsStatus }}</p>
          <button v-if="!gpsActive" @click="startGPS"
            class="w-full bg-[#2196f3] hover:bg-[#1976d2] text-white text-sm font-semibold rounded-lg px-4 py-2 transition-colors">
            📍 Start GPS Tracking
          </button>
          <button v-else @click="stopGPS"
            class="w-full bg-red-800 hover:bg-red-700 text-white text-sm font-semibold rounded-lg px-4 py-2 transition-colors">
            ⛔ Stop GPS Tracking
          </button>
        </div>
      </div>

      <!-- Complaints -->
      <div v-if="loading" class="text-center text-[#8892b0] py-20">Loading your tasks...</div>

      <div v-else-if="complaints.length === 0"
        class="text-center text-[#8892b0] py-20 bg-[#112240] rounded-2xl border border-[#1d3461]">
        🎉 No tasks assigned to you yet!
      </div>

      <div v-else class="space-y-4">
        <div v-for="c in complaints" :key="c.id"
          class="bg-[#112240] rounded-2xl border border-[#1d3461] p-5">
          <div class="flex items-start justify-between gap-4">
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-2">
                <span :class="statusColor(c.status)"
                  class="text-xs px-2 py-1 rounded-full border font-medium">
                  {{ c.status }}
                </span>
              </div>
              <p class="text-[#ccd6f6] font-medium">{{ c.issue }}</p>
              <p class="text-[#8892b0] text-sm mt-1">📍 {{ c.location }}</p>
              <p class="text-[#8892b0] text-xs mt-1">Reported by: {{ c.userName }}</p>
            </div>

            <!-- Mark as cleaned button -->
            <button v-if="c.status !== 'CLEANED'"
              @click="markCleaned(c.id)"
              class="bg-green-800 hover:bg-green-700 text-green-200 text-sm font-semibold rounded-xl px-4 py-2 transition-colors whitespace-nowrap">
              ✅ Mark Cleaned
            </button>
            <span v-else class="text-green-400 text-sm font-semibold">✅ Done</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
