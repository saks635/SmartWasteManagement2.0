<script setup>
import { ref, onMounted } from 'vue'
import { user } from '../composables/useUser.js'
import NavBar from '../components/NavBar.vue'

// Form data
const name     = ref(user.email || '')
const location = ref('')
const issue    = ref('')
const lat      = ref(null)
const lng      = ref(null)
const success  = ref(false)
const error    = ref('')
const loading  = ref(false)

// Submit the complaint
async function submit() {
  if (!name.value || !location.value || !issue.value) {
    error.value = 'Please fill in all fields'
    return
  }
  error.value  = ''
  loading.value = true

  const res = await fetch('/api/complaints', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      userName: name.value,
      location: location.value,
      issue: issue.value,
      latitude:  lat.value,
      longitude: lng.value,
    }),
    credentials: 'include',
  })

  loading.value = false
  if (res.ok) {
    success.value = true
    name.value = user.email || ''
    location.value = ''
    issue.value = ''
  } else {
    error.value = 'Failed to submit complaint. Please try again.'
  }
}

// Get user's current GPS location from the browser
function getLocation() {
  if (!navigator.geolocation) {
    error.value = 'Your browser does not support GPS location.'
    return
  }
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      lat.value = pos.coords.latitude.toFixed(6)
      lng.value = pos.coords.longitude.toFixed(6)
      location.value = `GPS: ${lat.value}, ${lng.value}`
    },
    () => { error.value = 'Could not get your location. Please type it manually.' }
  )
}

// Initialize Leaflet map
let map = null
onMounted(async () => {
  // Dynamically import leaflet (only runs in browser)
  const L = (await import('leaflet')).default
  await import('leaflet/dist/leaflet.css')

  // Fix default marker icons (common Leaflet + bundler issue)
  delete L.Icon.Default.prototype._getIconUrl
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  })

  map = L.map('map').setView([20.5937, 78.9629], 5) // Default center: India

  // Add OpenStreetMap tiles (free, no API key!)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap'
  }).addTo(map)

  let marker = null

  // When user clicks the map, set the location
  map.on('click', (e) => {
    lat.value = e.latlng.lat.toFixed(6)
    lng.value = e.latlng.lng.toFixed(6)
    location.value = `GPS: ${lat.value}, ${lng.value}`

    if (marker) marker.remove()
    marker = L.marker([e.latlng.lat, e.latlng.lng]).addTo(map)
  })
})
</script>

<template>
  <div class="min-h-screen bg-[#0a192f]">
    <NavBar />

    <div class="max-w-4xl mx-auto p-6">
      <h1 class="text-2xl font-bold text-[#ccd6f6] mb-1">Report a Waste Issue</h1>
      <p class="text-[#8892b0] text-sm mb-6">Help keep your city clean by reporting issues in your area</p>

      <!-- Success banner -->
      <div v-if="success" class="mb-6 p-4 rounded-xl bg-green-900/30 border border-green-700 text-green-400">
        ✅ Complaint submitted successfully! Our team will address it soon.
        <button @click="success = false" class="ml-3 underline text-sm">Submit another</button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

        <!-- Form panel -->
        <div class="bg-[#112240] rounded-2xl p-6 border border-[#1d3461]">
          <h2 class="text-[#64b5f6] font-semibold mb-5">Complaint Details</h2>

          <div v-if="error" class="mb-4 p-3 rounded-lg bg-red-900/30 border border-red-700 text-red-400 text-sm">
            ⚠️ {{ error }}
          </div>

          <div class="space-y-4">
            <div>
              <label class="text-xs text-[#8892b0] mb-1 block">Your Name</label>
              <input v-model="name" type="text" placeholder="Full name"
                class="w-full bg-[#0a192f] border border-[#1d3461] text-[#ccd6f6] rounded-xl px-4 py-3 focus:outline-none focus:border-[#2196f3] placeholder-[#8892b0]" />
            </div>

            <div>
              <label class="text-xs text-[#8892b0] mb-1 block">Location</label>
              <div class="flex gap-2">
                <input v-model="location" type="text" placeholder="Type address or click the map"
                  class="flex-1 bg-[#0a192f] border border-[#1d3461] text-[#ccd6f6] rounded-xl px-4 py-3 focus:outline-none focus:border-[#2196f3] placeholder-[#8892b0]" />
                <button @click="getLocation" title="Use my GPS location"
                  class="px-3 bg-[#1d3461] hover:bg-[#2196f3] text-[#64b5f6] hover:text-white rounded-xl transition-colors">
                  📍
                </button>
              </div>
            </div>

            <div>
              <label class="text-xs text-[#8892b0] mb-1 block">Describe the Issue</label>
              <textarea v-model="issue" rows="4" placeholder="e.g. Overflowing bin near the park gate..."
                class="w-full bg-[#0a192f] border border-[#1d3461] text-[#ccd6f6] rounded-xl px-4 py-3 focus:outline-none focus:border-[#2196f3] placeholder-[#8892b0] resize-none"></textarea>
            </div>

            <button @click="submit" :disabled="loading"
              class="w-full bg-[#2196f3] hover:bg-[#1976d2] disabled:opacity-50 text-white font-semibold rounded-xl py-3 transition-colors">
              {{ loading ? 'Submitting...' : '🚀 Submit Complaint' }}
            </button>
          </div>
        </div>

        <!-- Map panel -->
        <div class="bg-[#112240] rounded-2xl border border-[#1d3461] overflow-hidden">
          <div class="p-4 border-b border-[#1d3461]">
            <h2 class="text-[#64b5f6] font-semibold">📍 Pick Location on Map</h2>
            <p class="text-[#8892b0] text-xs mt-1">Click anywhere on the map to set the location</p>
          </div>
          <div id="map" class="h-80 md:h-full md:min-h-[400px]"></div>
        </div>
      </div>
    </div>
  </div>
</template>
