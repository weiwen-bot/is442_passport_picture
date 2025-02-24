import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import ImageUpload from '../components/ImageUpload.vue';
import ImageCropping from '../components/ImageCropping.vue';

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/image-upload',
    name: 'ImageUpload',
    component: ImageUpload
  },
  {
    path: '/image-crop',
    name: 'ImageCrop',
    component: ImageCropping
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