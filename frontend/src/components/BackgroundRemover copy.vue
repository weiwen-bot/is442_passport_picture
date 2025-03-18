<template>
  <div class="grid grid-cols-12 gap-4 h-[85vh] w-[calc(100vw-240px)] mx-auto">
    <!-- Left Side: Displayed Image -->
    <div class="col-span-8 flex flex-col h-full justify-center items-center border-r border-gray-300">
      <h3 class="text-lg font-semibold mb-4">Image</h3>
      <div v-if="displayedImage" class="w-4/5 max-h-[75%] flex justify-center items-center">
        <img :src="displayedImage" alt="Displayed Image" class="max-w-full max-h-full rounded-lg shadow-md border border-gray-400" />
      </div>
    </div>

    <!-- Right Side: Controls -->
    <div class="col-span-4 flex flex-col h-full justify-center items-center p-4">
      <!-- Show loading indicator -->
      <div v-if="isProcessing" class="mt-4 text-gray-600 text-sm animate-pulse">Processing image, please wait...</div>

      <!-- Error Message -->
      <div v-if="errorMessage" class="mt-4 text-red-500 text-sm">{{ errorMessage }}</div>

      <!-- Process Button -->
      <button v-if="displayedImage" @click="processImage"
        class="mt-6 px-5 py-2 bg-blue-500 hover:bg-blue-600 text-white font-semibold rounded-lg shadow-md transition duration-200">
        Process Image
      </button>
      
      <!-- Background Selection -->
      <div class="mt-6 flex flex-col items-center w-full">
        <label for="backgroundColor" class="mb-2 text-gray-700 font-medium">Select Background Color:</label>
        <input type="color" id="backgroundColor" v-model="color" class="w-12 h-12 border rounded-lg cursor-pointer">
      </div>

      <!-- Upload Image Button -->
      <div class="mt-6">
        <UploadImageButton @image-uploaded="updateImage" />
      </div>
    </div>
  </div>
</template>

<script>
import UploadImageButton from "./UploadImageButton.vue";
export default {
  props: {
    imageData: String,
  },
  components: {
    UploadImageButton,
  },
  data() {
    return {
      displayedImage: null,
      isProcessing: false,
      errorMessage: "",
      backgroundImage: null,
      color: "#FFFFFF",
    };
  },
  created() {
    this.displayedImage = this.imageData || localStorage.getItem("imageData");
  },
  methods: {
    updateImage(image) {
      this.backgroundImage = image;
    },
    async processImage() {
      if (!this.displayedImage) {
        this.errorMessage = "No image available for processing.";
        return;
      }

      this.isProcessing = true;
      this.errorMessage = "";

      try {
        const finalBase64 = await this.resizeToClosestMultipleOf32(this.displayedImage);
        
        const payload = this.backgroundImage 
          ? { image: finalBase64, category: "background", backgroundString: this.backgroundImage }
          : { image: finalBase64, category: "color", colorString: this.color };

        const response = await fetch("http://localhost:8080/image/process", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });

        if (!response.ok) throw new Error("Processing failed");

        const result = await response.json();
        if (result.processedImage) {
          this.displayedImage = result.processedImage;
          localStorage.setItem("imageData", result.processedImage);
          //emit event to parent component
          this.$emit("update:imageData", result.processedImage);

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
          const newWidth = this.roundToNearestMultiple(img.width, 32);
          const newHeight = this.roundToNearestMultiple(img.height, 32);

          const canvas = document.createElement("canvas");
          canvas.width = newWidth;
          canvas.height = newHeight;
          const ctx = canvas.getContext("2d");
          ctx.drawImage(img, 0, 0, newWidth, newHeight);

          resolve(canvas.toDataURL("image/png"));
        };
        img.onerror = () => reject(new Error("Failed to load image"));
      });
    },

    roundToNearestMultiple(value, m) {
      const remainder = value % m;
      return remainder <= m / 2 ? value - remainder : value + (m - remainder);
    },
  },
};
</script>
