<template>
  <div class="col-span-8 flex flex-col rounded-lg shadow-lg p-2 space-y-2 text-black bg-white shad">
    <h2 class="text-2xl font-semibold mb-6 text-black">Remove Background</h2>

    <!-- Image Display center -->
    <div class="flex flex-col sm:flex-row sm:justify-between sm:space-x-4">
      <div class="flex flex-col items-center space-y-4 sm:w-1/2 w-full">
        <h3 class="text-lg font-semibold mb-4">Original Image</h3>
        <div v-if="originalImage" class="relative">
          <img :src="originalImage" alt="Original" class="w-full max-w-4xl h-auto rounded-lg shadow-md border border-gray-300" />
        </div>
        <div v-else class="text-gray-500 text-sm">No image selected</div>
      </div>

      <div class="flex flex-col items-center space-y-4 sm:w-1/2 w-full">
        <h3 class="text-lg font-semibold mb-4">Processed Image</h3>
        <div v-if="processedImage" class="relative">
          <img :src="processedImage" alt="Processed" class="w-full max-w-4xl h-auto rounded-lg shadow-md border border-gray-300" />
        </div>
        <div v-else class="text-gray-500 text-sm">No processed image available</div>
      </div>
    </div>

    <!-- Processing Status -->
    <div v-if="isProcessing" class="text-gray-600 text-sm animate-pulse mb-4">Processing image, please wait...</div>

    <!-- Error Message -->
    <div v-if="errorMessage" class="mt-4 text-red-500 text-sm">{{ errorMessage }}</div>

    <!-- Remove Background Button -->
    <button
      v-if="originalImage && !processedImage"
      @click="processImage"
      class="mt-6 px-6 py-3 bg-blue-500 hover:bg-blue-600 text-white font-semibold rounded-lg shadow-md transition duration-300 transform hover:scale-105"
    >
      Remove Background
    </button>
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
        // Crop + scale to multiple-of-32 dimension
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

        // 1) Round each dimension to the nearest multiple of 32
        const newWidth = this.roundToNearestMultiple(originalWidth, 32);
        const newHeight = this.roundToNearestMultiple(originalHeight, 32);

        // 2) Create a canvas at the new size
        const canvas = document.createElement("canvas");
        canvas.width = newWidth;
        canvas.height = newHeight;
        const ctx = canvas.getContext("2d");

        // 3) Draw the entire original image scaled to the new dimension
        ctx.drawImage(img, 0, 0, newWidth, newHeight);

        // 4) Convert to base64 and return
        const resizedBase64 = canvas.toDataURL("image/png");
        resolve(resizedBase64);
      };
      img.onerror = () => reject(new Error("Failed to load image"));
    });
  },

  // Helper to round a number to the nearest multiple of 'm'
  roundToNearestMultiple(value, m) {
    const remainder = value % m;
    const down = value - remainder; // e.g. 185 - 25 = 160
    const up = down + m;           // e.g. 160 + 32 = 192

    if (remainder <= m / 2) {
      return down;   // e.g. if remainder < 16, we pick 160
    } else {
      return up;     // e.g. remainder >=16 => we pick 192
    }
  },

  },
};
</script>
