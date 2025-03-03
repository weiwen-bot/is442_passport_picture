<template>
  <div class="flex flex-col items-center space-y-4 p-4">
    <h2 class="text-xl font-bold">Upload Image for Background Removal</h2>
    <input type="file" @change="handleFileUpload" accept="image/png, image/jpeg" class="border p-2" />

    <div v-if="imageUrl" class="relative w-full max-w-md">
      <img ref="imageRef" :src="imageUrl" class="w-full" />
    </div>

    <button @click="processImage" :disabled="!imageFile" 
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
  data() {
    return {
      imageFile: null,
      imageUrl: null,
      cropper: null,
    };
  },
  methods: {
    handleFileUpload(event) {
      const file = event.target.files[0];
      if (file) {
        this.imageFile = file;
        this.imageUrl = URL.createObjectURL(file);
        this.$nextTick(this.initCropper);
      }
    },
    initCropper() {
      if (this.cropper) {
        this.cropper.destroy();
      }
      const image = this.$refs.imageRef;
      this.cropper = new Cropper(image, {
        aspectRatio: NaN, // Allows free resizing of width and height
        viewMode: 1,
        autoCropArea: 0.8,
        movable: true,
        zoomable: true,
        rotatable: true,
        scalable: true,
      });
    },
    async processImage() {
      if (!this.imageFile || !this.cropper) return;
      const cropData = this.cropper.getData();
      const formData = new FormData();
      formData.append("file", this.imageFile);
      formData.append("x", Math.round(cropData.x));
      formData.append("y", Math.round(cropData.y));
      formData.append("width", Math.round(cropData.width));
      formData.append("height", Math.round(cropData.height));

      //alert x y width height
      alert(Math.round(cropData.x) + " " + Math.round(cropData.y) + " " + Math.round(cropData.width) + " " + Math.round(cropData.height));



      // try {
      //   const response = await axios.post("http://localhost:8080/api/remove-background", formData, {
      //     headers: { "Content-Type": "multipart/form-data" },
      //   });
      //   console.log("Processed Image:", response.data);
      //   alert("Background removed successfully!");
      // } catch (error) {
      //   console.error("Error processing image:", error);
      //   alert("Failed to process image.");
      // }
    },
  },
};
</script>
