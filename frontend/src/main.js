import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import VueSidebarMenu from 'vue-sidebar-menu'
import 'vue-sidebar-menu/dist/vue-sidebar-menu.css'
import '@fortawesome/fontawesome-free/css/all.css'; // Import FontAwesome CSS

const app = createApp(App)
app.use(router)
app.use(VueSidebarMenu)
app.mount('#app')
