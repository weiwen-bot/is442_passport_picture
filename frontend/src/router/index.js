import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import DownloadPictures from '../views/DownloadPictures.vue'


const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: "/download-pictures",
    name: "DownloadPictures",
    component: DownloadPictures,
  },
  
]

const router = createRouter({
  mode: 'history',
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// router.beforeEach(async (to) => {

// })

export default router