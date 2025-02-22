import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import ImageCropping from '../components/ImageCropping.vue'
import ImageUpload from '../components/ImageUpload.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/crop', // Add a route to the ImageCropper component
    name: 'ImageCropping',
    component: ImageCropping,
  },
  {
    path: '/upload', // Add a route to the ImageUpload component
    name: 'ImageUpload',
    component: ImageUpload,
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