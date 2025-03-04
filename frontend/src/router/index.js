import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import ImageUpload from '../components/ImageUpload.vue';
import ImageEdit from '../components/ImageEdit.vue';
import BackgroundRemover from '../components/BackgroundRemover.vue';
import ProcessImage from '../components/ProcessImage.vue';

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
    path: '/image-edit',
    name: 'ImageEdit',
    component: ImageEdit
  },
  //add Background remover route
  {
    path : '/background-remover',
    name : 'BackgroundRemover',
    component: BackgroundRemover
  },

  {
    path : '/process-image',
    name : 'ProcessImage',
    component: ProcessImage
  }
  
]

const router = createRouter({
  mode: 'history',
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// router.beforeEach(async (to) => {

// })

export default router