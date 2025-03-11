<template>
    <div class="col-span-4 flex flex-col rounded-lg shadow-lg p-2 space-y-2 text-black">
    <h2 class="text-2xl font-semibold mb-6 text-white">Remove Background</h2>

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
      class="mt-6 px-6 py-3 bg-blue-500 hover:bg-blue-600 text-white font-semibold rounded-lg shadow-md transition duration-300 transform hover:scale-105"
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
        // Get the original image dimensions
        const img = new Image();
        img.src = this.originalImage;
        await new Promise((resolve, reject) => {
          img.onload = resolve;
          img.onerror = () => reject(new Error("Failed to load image"));
        });

        const originalWidth = img.width;
        const originalHeight = img.height;
        console.log("og", originalWidth, originalHeight);

        // Step 1: Resize with padding to nearest multiple of 32
        const { base64: paddedBase64, newWidth, newHeight } = await this.resizeWithPadding(
          this.originalImage, 
          originalWidth, 
          originalHeight
        );
        console.log("new", newWidth, newHeight);

        const payload = { image: paddedBase64 };

        // Step 2: Send the padded image for processing
        const response = await fetch("http://localhost:8080/image/process", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });

        if (!response.ok) throw new Error("Processing failed");

        const result = await response.json();
        if (!result.processedImage) throw new Error("No processed image received from backend.");

        // Step 3: Crop the processed image back to original dimensions
        this.processedImage = await this.cropBackToOriginal(result.processedImage, originalWidth, originalHeight);

        // Save the final cropped image
        this.$emit("update:imageData", this.processedImage);
        localStorage.setItem("imageData", this.processedImage);
      } catch (error) {
        console.error("Error processing image:", error);
        this.errorMessage = "Error processing image.";
      } finally {
        this.isProcessing = false;
      }
    },

    async resizeWithPadding(base64, originalWidth, originalHeight) {
      return new Promise((resolve, reject) => {
        const img = new Image();
        img.src = base64;
        img.onload = () => {
          const newWidth = this.roundUpToMultiple(originalWidth, 32);
          const newHeight = this.roundUpToMultiple(originalHeight, 32);

          const canvas = document.createElement("canvas");
          canvas.width = newWidth;
          canvas.height = newHeight;
          const ctx = canvas.getContext("2d");

          // Ensure transparency by clearing the canvas
          ctx.clearRect(0, 0, newWidth, newHeight);

          // Draw the image at (0,0), padding will appear in the remaining space
          ctx.drawImage(img, 0, 0);

          const paddedBase64 = canvas.toDataURL("image/png");
          resolve({ base64: paddedBase64, newWidth, newHeight });
        };
        img.onerror = () => reject(new Error("Failed to load image"));
      });
    },

    // Round up to the nearest multiple of 32
    roundUpToMultiple(value, m) {
      return Math.ceil(value / m) * m;
    },

    async cropBackToOriginal(base64, originalWidth, originalHeight) {
      return new Promise((resolve, reject) => {
        const img = new Image();
        img.src = base64;
        img.onload = () => {
          const canvas = document.createElement("canvas");
          canvas.width = originalWidth;
          canvas.height = originalHeight;
          const ctx = canvas.getContext("2d");

          // Crop the image back to its original size
          ctx.drawImage(img, 0, 0, originalWidth, originalHeight, 0, 0, originalWidth, originalHeight);

          const croppedBase64 = canvas.toDataURL("image/png");
          resolve(croppedBase64);
        };
        img.onerror = () => reject(new Error("Failed to load processed image"));
      });
    },
  },
};
</script>

