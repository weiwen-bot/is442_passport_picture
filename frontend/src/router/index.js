import { createRouter, createWebHistory } from "vue-router";
import Home from "../views/Home.vue";
import ImageUpload from "../components/ImageUpload.vue";
import ImageEdit from "../components/ImageEdit.vue";
import BackgroundRemover from "../components/BackgroundRemover.vue";
import ImageResizing from "../components/ImageResizing.vue";
import ImageEnhancement from "../components/ImageEnhancement.vue";
import QuickGenerate from "../components/QuickGenerate.vue";
import BatchImageUploader from "../components/BatchProcess.vue"

const routes = [
  // {
  //   path: "/",
  //   name: "Home",
  //   component: Home,
  // },
  {
    path: "/batch",
    name: "BatchImageUploader",
    component: BatchImageUploader,
  },
  {
    path: "/",
    name: "ImageUpload",
    component: ImageUpload,
  },
  {
    path: "/image-edit",
    name: "ImageEdit",
    component: ImageEdit,
  },
  // add image resize route
  {
    path: "/resize",
    name: "ImageResize",
    component: ImageResizing,
  },
  //add Background remover route
  {
    path: "/background-remover",
    name: "BackgroundRemover",
    component: BackgroundRemover,
  },
  //add Enhancement route
  {
    path: "/enhancement",
    name: "ImageEnhancement",
    component: ImageEnhancement,
  },
  //add Quick Generate route
  {
    path: "/quick-generate",
    name: "QuickGenerate",
    component: QuickGenerate,
  }
];

const router = createRouter({
  mode: "history",
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

// router.beforeEach(async (to) => {

// })

export default router;
