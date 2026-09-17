import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './features/auth/authStore'
import { installSessionExpiryRedirect } from './features/auth/sessionExpiryRedirect'
import 'element-plus/dist/index.css'
import './styles/main.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
installSessionExpiryRedirect(useAuthStore(pinia), router)
app.use(router)
app.mount('#app')
