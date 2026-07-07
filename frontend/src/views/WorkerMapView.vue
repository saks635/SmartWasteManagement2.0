<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import { API_BASE } from '../config.js'

const route     = useRoute()
const router    = useRouter()
const username  = route.params.username
const locations = ref([])
const loading   = ref(true)

onMounted(async () => {
  // Fetch this worker's last 10 GPS locations
  const res = await fetch(API_BASE + `/api/admin/worker/${username}/locations`, { credentials: 'include' })
  if (res.ok) locations.value = await res.json()
  loading.value = false

  // Initialize Leaflet map
  const L = (await import('leaflet')).default
  await import('leaflet/dist/leaflet.css')

  // Fix default marker icons
  delete L.Icon.Default.prototype._getIconUrl
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  })

  // If no locations, show default India view
  if (locations.value.length === 0) {
    L.map('map').setView([20.5937, 78.9629], 5)
        .addLayer(L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'))
    return
  }

  // Center the map on the most recent location
  const latest = locations.value[0]
  const map = L.map('map').setView([latest.latitude, latest.longitude], 14)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap'
  }).addTo(map)

  // Add a marker and polyline for each location
  const coords = locations.value.map(l => [l.latitude, l.longitude])

  // Draw a blue line connecting all points
  L.polyline(coords, { color: '#2196f3', weight: 3 }).addTo(map)

  // Add markers — most recent is red, older ones are blue
  locations.value.forEach((loc, i) => {
    const isLatest = i === 0
    const circle = L.circleMarker([loc.latitude, loc.longitude], {
      radius: isLatest ? 10 : 6,
      fillColor: isLatest ? '#f44336' : '#2196f3',
      color: '#fff',
      weight: 2,
      fillOpacity: 0.9,
    }).addTo(map)
    circle.bindPopup(`<b>${isLatest ? '📍 Latest' : 'Location ' + (i + 1)}</b><br>${loc.timestamp || ''}`)
  })
})
</script>

<template>
  <div class="min-h-screen bg-[#0a192f]">
    <NavBar />

    <div class="max-w-5xl mx-auto p-6">
      <!-- Header -->
      <div class="flex items-center gap-3 mb-6">
        <button @click="router.push('/admin')"
          class="text-[#8892b0] hover:text-[#64b5f6] transition-colors text-sm">
          ← Back to Dashboard
        </button>
      </div>

      <h1 class="text-2xl font-bold text-[#ccd6f6] mb-1">
        GPS Location History
      </h1>
      <p class="text-[#8892b0] text-sm mb-6">
        Tracking worker: <span class="text-[#64b5f6] font-medium">{{ username }}</span>
        · Last {{ locations.length }} locations shown
      </p>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Map -->
        <div class="bg-[#112240] rounded-2xl border border-[#1d3461] overflow-hidden md:col-span-2">
          <div v-if="loading" class="h-80 flex items-center justify-center text-[#8892b0]">
            Loading map...
          </div>
          <div v-else id="map" class="h-80 md:h-[450px]"></div>
        </div>

        <!-- Locations table -->
        <div class="bg-[#112240] rounded-2xl border border-[#1d3461] overflow-hidden md:col-span-2">
          <div class="p-4 border-b border-[#1d3461]">
            <h2 class="text-[#64b5f6] font-semibold">Location History</h2>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-[#1d3461] text-xs uppercase text-[#8892b0]">
                  <th class="px-4 py-3 text-left">#</th>
                  <th class="px-4 py-3 text-left">Latitude</th>
                  <th class="px-4 py-3 text-left">Longitude</th>
                  <th class="px-4 py-3 text-left">Timestamp</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="locations.length === 0">
                  <td colspan="4" class="px-4 py-8 text-center text-[#8892b0]">No locations recorded yet</td>
                </tr>
                <tr v-for="(loc, i) in locations" :key="loc.id"
                  class="border-b border-[#1d3461]/50 hover:bg-[#1d3461]/30 transition-colors">
                  <td class="px-4 py-3 text-[#8892b0]">
                    <span v-if="i === 0" class="text-red-400 font-bold">● Latest</span>
                    <span v-else class="text-[#64b5f6]">{{ i + 1 }}</span>
                  </td>
                  <td class="px-4 py-3 text-[#ccd6f6] font-mono">{{ loc.latitude }}</td>
                  <td class="px-4 py-3 text-[#ccd6f6] font-mono">{{ loc.longitude }}</td>
                  <td class="px-4 py-3 text-[#8892b0]">{{ loc.timestamp }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
