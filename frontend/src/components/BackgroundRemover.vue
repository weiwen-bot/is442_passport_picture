<template>
  <div class="col-span-4 flex flex-col rounded-lg shadow-lg p-2 space-y-2 text-black">
  <h2 class="text-2xl font-semibold mb-6">Remove Background</h2>

  <!-- Image Display center -->
  <div class="flex flex-col items-center space-y-4">

    <div v-if="originalImage" class="relative">
      <img :src="originalImage" alt="Original" class="w-full h-auto rounded-lg shadow-md border border-gray-300" />
    </div>
    <div v-else class="text-gray-500 text-sm">No image selected</div>
  </div>

  <!-- Processing Status -->
  <div v-if="isProcessing" class="text-gray-600 text-sm animate-pulse mb-4">Processing image, please wait...</div>

  <!-- Error Message -->
  <div v-if="errorMessage" class="mt-4 text-red-500 text-sm">{{ errorMessage }}</div>

  <!-- Remove Background Button -->
  <button
    v-if="originalImage && !processedImage"
    @click="processImage"
    :disabled="isProcessing || processedImage"
    class="mt-6 px-6 py-3 bg-blue-500 hover:bg-blue-600 font-semibold rounded-lg shadow-md transition duration-300 transform hover:scale-105"
  >
    Remove Background
  </button>

  <!-- Processed Image Display -->
  <div v-if="processedImage" class="w-3/4 max-w-md mt-6">
    <h3 class="text-lg font-semibold mb-4">Processed Image</h3>
    <img :src="processedImage" alt="Processed" class="w-full h-auto rounded-lg shadow-md border border-gray-300" />
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
      const finalBase64 = await this.resizeToClosestMultipleOf32(this.originalImage);

      const payload = { image: finalBase64 };

      const response = await fetch("http://localhost:8080/image/process", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!response.ok) throw new Error("Processing failed");

      const result = await response.json();
      if (result.processedImage) {
        this.processedImage = result.processedImage;
        // Save processed image to props (from ImageEdit.vue) or localStorage
        this.$emit("update:imageData", this.processedImage);
        localStorage.setItem("imageData", this.processedImage);
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

  async resizeToClosestMultipleOf32(base64) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.src = base64;
      img.onload = () => {
        const originalWidth = img.width;
        const originalHeight = img.height;

        const newWidth = this.roundToNearestMultiple(originalWidth, 32);
        const newHeight = this.roundToNearestMultiple(originalHeight, 32);

        const canvas = document.createElement("canvas");
        canvas.width = newWidth;
        canvas.height = newHeight;
        const ctx = canvas.getContext("2d");

        ctx.drawImage(img, 0, 0, newWidth, newHeight);

        const resizedBase64 = canvas.toDataURL("image/png");
        resolve(resizedBase64);
      };
      img.onerror = () => reject(new Error("Failed to load image"));
    });
  },

  roundToNearestMultiple(value, m) {
    const remainder = value % m;
    const down = value - remainder;
    const up = down + m;

    if (remainder <= m / 2) {
      return down;
    } else {
      return up;
    }
  },
},
};
</script>

<style scoped>
/* Add additional styling here if needed */
</style>
