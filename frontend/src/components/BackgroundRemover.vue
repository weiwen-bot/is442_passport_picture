<template>
  <div class="flex flex-col items-center space-y-4 p-4 w-full">
    <h2 class="text-xl font-bold">Remove Background</h2>

    <!-- Ensure image takes full width -->
    <div v-if="imageUrl" class="relative w-full max-w-4xl">
      <img ref="imageRef" :src="imageUrl" class="w-full h-auto max-h-[80vh]" @load="initCropper" />
    </div>

    <button @click="processImage" :disabled="!imageUrl" 
            class="bg-blue-500 text-white px-4 py-2 rounded disabled:bg-gray-400">
      Remove Background
    </button>
  </div>
</template>


<script>
import Cropper from "cropperjs";
import "cropperjs/dist/cropper.css";
import axios from "axios";

export default {
  props: {
    imageData: String, // ✅ Accepts image from ImageEdit.vue
  },
  data() {
    return {
      cropper: null,
      imageFile: null, // ✅ Maintain imageFile for processing
    };
  },
  computed: {
    imageUrl() {
      return this.imageData;
    },
  },
  watch: {
    imageData: {
      immediate: true, // ✅ Ensures watcher runs on mount
      handler(newImage) {
        if (newImage) {
          console.log("New image detected:", newImage);
          this.imageFile = this.dataURLtoFile(newImage, "uploaded-image.jpg");
          this.$nextTick(this.initCropper); // ✅ Ensure Cropper initializes
        }
      },
    },
  },

  methods: {
    initCropper() {
      if (this.cropper) {
        this.cropper.destroy();
      }
      const image = this.$refs.imageRef;
      this.cropper = new Cropper(image, {
        aspectRatio: NaN,
        viewMode: 1,
        autoCropArea: 0.8,
        movable: true,
        zoomable: true,
        rotatable: true,
        scalable: true,
      });
    },
    async processImage() {
      console.log("Processing image...");

      if (!this.imageFile) {
        console.error("❌ No image file detected!");
        alert("No image file detected!");
        return;
      }

      if (!this.cropper) {
        console.error("❌ Cropper is not initialized!");
        alert("Cropper is not initialized!");
        return;
      }

      const cropData = this.cropper.getData();
      console.log("Crop data:", cropData);

      if (!cropData.width || !cropData.height) {
        console.error("❌ Invalid crop dimensions!");
        alert("Invalid crop dimensions!");
        return;
      }

      const formData = new FormData();
      formData.append("file", this.imageFile);
      formData.append("x", Math.round(cropData.x));
      formData.append("y", Math.round(cropData.y));
      formData.append("width", Math.round(cropData.width));
      formData.append("height", Math.round(cropData.height));

      alert(`x: ${Math.round(cropData.x)}, y: ${Math.round(cropData.y)}, width: ${Math.round(cropData.width)}, height: ${Math.round(cropData.height)}`);
    },
    dataURLtoFile(dataUrl, filename) {
      let arr = dataUrl.split(","),
        mime = arr[0].match(/:(.*?);/)[1],
        bstr = atob(arr[1]),
        n = bstr.length,
        u8arr = new Uint8Array(n);

      while (n--) {
        u8arr[n] = bstr.charCodeAt(n);
      }
      return new File([u8arr], filename, { type: mime });
    },
  },
};
</script>
