<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import { API_BASE } from '../config.js'

const router     = useRouter()
const complaints = ref([])   // All complaints list
const workers    = ref([])   // All workers list
const loading    = ref(true)

// Load all complaints and workers when page opens
onMounted(async () => {
  const [cRes, wRes] = await Promise.all([
    fetch(API_BASE + '/api/admin/complaints', { credentials: 'include' }),
    fetch(API_BASE + '/api/admin/workers',    { credentials: 'include' }),
  ])
  if (cRes.ok) complaints.value = await cRes.json()
  if (wRes.ok) workers.value    = await wRes.json()
  loading.value = false
})

// Assign a worker to a complaint
async function assign(complaintId, workerName) {
  if (!workerName) return
  await fetch(API_BASE + '/api/admin/assign', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ complaintId, workerName }),
    credentials: 'include',
  })
  // Refresh complaints list
  const res = await fetch(API_BASE + '/api/admin/complaints', { credentials: 'include' })
  if (res.ok) complaints.value = await res.json()
}

// Status badge color
function statusColor(status) {
  if (status === 'CLEANED')  return 'bg-green-900/40 text-green-400 border-green-700'
  if (status === 'ASSIGNED') return 'bg-blue-900/40 text-blue-400 border-blue-700'
  return 'bg-yellow-900/40 text-yellow-400 border-yellow-700'
}
</script>

<template>
  <div class="min-h-screen bg-[#0a192f]">
    <NavBar />

    <div class="max-w-6xl mx-auto p-6">
      <!-- Header with stats -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between mb-6 gap-4">
        <div>
          <h1 class="text-2xl font-bold text-[#ccd6f6]">Admin Dashboard</h1>
          <p class="text-[#8892b0] text-sm">Manage all complaints and worker assignments</p>
        </div>
        <!-- Quick stats -->
        <div class="flex gap-3">
          <div class="bg-[#112240] rounded-xl p-3 border border-[#1d3461] text-center min-w-[70px]">
            <div class="text-xl font-bold text-[#64b5f6]">{{ complaints.length }}</div>
            <div class="text-xs text-[#8892b0]">Total</div>
          </div>
          <div class="bg-[#112240] rounded-xl p-3 border border-[#1d3461] text-center min-w-[70px]">
            <div class="text-xl font-bold text-yellow-400">{{ complaints.filter(c => c.status === 'NEW').length }}</div>
            <div class="text-xs text-[#8892b0]">Pending</div>
          </div>
          <div class="bg-[#112240] rounded-xl p-3 border border-[#1d3461] text-center min-w-[70px]">
            <div class="text-xl font-bold text-green-400">{{ complaints.filter(c => c.status === 'CLEANED').length }}</div>
            <div class="text-xs text-[#8892b0]">Cleaned</div>
          </div>
        </div>
      </div>

      <!-- Loading state -->
      <div v-if="loading" class="text-center text-[#8892b0] py-20">Loading complaints...</div>

      <!-- Complaints table -->
      <div v-else class="bg-[#112240] rounded-2xl border border-[#1d3461] overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-[#1d3461] text-xs uppercase text-[#8892b0]">
                <th class="px-4 py-3 text-left">Citizen</th>
                <th class="px-4 py-3 text-left">Issue</th>
                <th class="px-4 py-3 text-left">Location</th>
                <th class="px-4 py-3 text-left">Status</th>
                <th class="px-4 py-3 text-left">Assign Worker</th>
                <th class="px-4 py-3 text-left">Track</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in complaints" :key="c.id"
                class="border-b border-[#1d3461]/50 hover:bg-[#1d3461]/30 transition-colors">
                <td class="px-4 py-3 text-[#ccd6f6] text-sm">{{ c.userName }}</td>
                <td class="px-4 py-3 text-[#ccd6f6] text-sm max-w-[150px] truncate">{{ c.issue }}</td>
                <td class="px-4 py-3 text-[#8892b0] text-xs max-w-[150px] truncate">{{ c.location }}</td>
                <td class="px-4 py-3">
                  <span :class="statusColor(c.status)"
                    class="text-xs px-2 py-1 rounded-full border font-medium">
                    {{ c.status }}
                  </span>
                </td>
                <td class="px-4 py-3">
                  <select @change="assign(c.id, $event.target.value)"
                    class="bg-[#0a192f] border border-[#1d3461] text-[#ccd6f6] rounded-lg px-2 py-1 text-xs focus:outline-none focus:border-[#2196f3]">
                    <option value="">-- Assign --</option>
                    <option v-for="w in workers" :key="w.id" :value="w.username"
                      :selected="w.username === c.assignedWorker">
                      {{ w.username }}
                    </option>
                  </select>
                </td>
                <td class="px-4 py-3">
                  <button v-if="c.assignedWorker"
                    @click="router.push(`/admin/worker/${c.assignedWorker}`)"
                    class="text-xs text-[#64b5f6] hover:underline">
                    📍 Map
                  </button>
                </td>
              </tr>
              <tr v-if="complaints.length === 0">
                <td colspan="6" class="px-4 py-12 text-center text-[#8892b0]">No complaints yet</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>
