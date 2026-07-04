import { createApp } from 'vue'
import App from './App.vue'
import router from './router/index.js'
import './style.css'

// Create the Vue app, add the router, and mount it to the HTML div#app
createApp(App).use(router).mount('#app')
