<template>
  <div class="flex h-[85vh] w-[calc(100vw-240px)] mx-auto">
    <!-- Left Side: Original Image -->
    <div class="flex flex-col w-1/2 h-full justify-center items-center border-r border-gray-300">
      <h3 class="text-lg font-semibold mb-4">Original Image</h3>
      <div v-if="originalImage" class="w-4/5 max-h-[75%] flex justify-center items-center">
        <img :src="originalImage" alt="Original" class="max-w-full max-h-full rounded-lg shadow-md border border-gray-400" />
      </div>
    </div>

    <!-- Right Side: Processed Image -->
    <div class="flex flex-col w-1/2 h-full justify-center items-center">
      <h3 class="text-lg font-semibold mb-4">Processed Image</h3>
      <div v-if="processedImage" class="w-4/5 max-h-[75%] flex justify-center items-center">
        <img :src="processedImage" alt="Processed" class="max-w-full max-h-full rounded-lg shadow-md border border-gray-400" />
      </div>
      <div v-else class="text-gray-500 text-sm">No processed image yet</div>

      <!-- Show loading indicator -->
      <div v-if="isProcessing" class="mt-4 text-gray-600 text-sm animate-pulse">Processing image, please wait...</div>

      <!-- Error Message -->
      <div v-if="errorMessage" class="mt-4 text-red-500 text-sm">{{ errorMessage }}</div>

      <!-- Process Button -->
      <button v-if="originalImage && !processedImage" @click="processImage"
        class="mt-6 px-5 py-2 bg-blue-500 hover:bg-blue-600 text-white font-semibold rounded shadow-md transition">
        Process Image
      </button>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    imageData: String, // Receive imageData as a prop from ImageEdit.vue
  },
  data() {
    return {
      originalImage: null, // Base64 image from parent/localStorage
      processedImage: null, // Processed image from backend
      isProcessing: false,
      errorMessage: "", // Error message handling
    };
  },
  created() {
    // Load image from props (from ImageEdit.vue) or localStorage
    this.originalImage = this.imageData || localStorage.getItem("imageData");
  },
  methods: {
    async processImage() {
      if (!this.originalImage) {
        this.errorMessage = "No image available for processing.";
        return;
      }

      this.isProcessing = true;
      this.errorMessage = "";

      try {
        // Ensure image is resized to 512x512 before sending
        const resizedBase64 = await this.resizeImage(this.originalImage, 512, 512);

        const payload = { image: resizedBase64 };

        const response = await fetch("http://localhost:8080/image/process", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });

        if (!response.ok) throw new Error("Processing failed");

        const result = await response.json();
        if (result.processedImage) {
          this.processedImage = result.processedImage; // Update UI with processed image
        } else {
          throw new Error("No processed image received from backend.");
        }
      } catch (error) {
        console.error("Error processing image:", error);
        this.errorMessage = "Error processing image.";
      } finally {
        this.isProcessing = false;
      }
    },

    async resizeImage(base64, width, height) {
      return new Promise((resolve, reject) => {
        const img = new Image();
        img.src = base64;
        img.onload = () => {
          const canvas = document.createElement("canvas");
          canvas.width = width;
          canvas.height = height;
          const ctx = canvas.getContext("2d");
          ctx.drawImage(img, 0, 0, width, height);
          resolve(canvas.toDataURL("image/png"));
        };
        img.onerror = () => reject(new Error("Failed to load image"));
      });
    },
  },
};
</script>
