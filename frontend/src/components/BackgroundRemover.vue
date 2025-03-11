<template>
  <!-- Right side: Image Display -->
  <div class="col-span-6 flex items-center justify-center bg-gray-100 shadow-lg">
    <img v-if="displayedImage" :src="displayedImage" alt="Displayed Image" class="max-h-full max-w-full object-contain" />
  </div>

  <!-- Left side: Background Change Controls -->
  <div class="col-span-6 flex flex-col bg-white rounded-lg shadow-lg p-4 space-y-4 text-black">
    <h2 class="font-bold text-lg">Change Background</h2>

    <!-- Tabs -->
    <div class="border-b border-gray-200">
      <ul class="flex text-sm font-medium text-center" role="tablist">
        <li class="flex-1">
          <button @click="activeTab = 'color'" :class="tabClass('color')" role="tab">Change Color</button>
        </li>
        <li class="flex-1">
          <button @click="activeTab = 'image'" :class="tabClass('image')" role="tab">Replace BG with Image</button>
        </li>
      </ul>
    </div>

    <!-- Tab Content -->
    <div v-if="activeTab === 'color'" class="p-4">
      <label class="block text-gray-700 font-medium mb-2">Select Background Color:</label>
      <div class="flex items-center space-x-2">
        <!-- Color Picker -->
        <input type="color" v-model="color" @input="applyColor" class="w-12 h-12 border rounded-lg cursor-pointer">

        <!-- Common Colors -->
        <div class="flex space-x-2">
          <div v-for="c in commonColors" :key="c" 
               :style="{ backgroundColor: c }" 
               @click="applyColor(c)" 
               class="w-10 h-10 rounded-lg border cursor-pointer hover:opacity-80"></div>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'image'" class="p-4">
      <label class="block text-gray-700 font-medium mb-2">Select Background Image:</label>
      <div class="grid grid-cols-3 gap-2">
        <!-- Upload Button -->
        <div class="relative w-16 h-16 flex items-center justify-center border-2 border-dashed rounded-lg cursor-pointer">
          <span class="text-2xl font-bold">+</span>
          <!-- Invisible Upload Button -->
          <UploadImageButton class="absolute inset-0 opacity-0 cursor-pointer" @image-uploaded="applyBackground" />
        </div>

        <!-- Sample Backgrounds -->
        <div 
          v-for="bg in sampleBackgrounds" 
          :key="bg.name" 
          class="w-16 h-16 bg-cover bg-center rounded-lg cursor-pointer border border-gray-300"
          :style="{ backgroundImage: `url(${bg.url})` }"
          @click="applyBackground(bg.url)"
        ></div>
      </div>

      <!-- Hidden UploadImageButton -->
      <UploadImageButton ref="uploadButton" @image-uploaded="applyBackground" class="hidden" />
    </div>

    <!-- Loading Indicator -->
    <div v-if="isProcessing" class="text-gray-600 text-sm animate-pulse">Processing image, please wait...</div>

    <!-- Error Message -->
    <div v-if="errorMessage" class="text-red-500 text-sm">{{ errorMessage }}</div>
  </div>
</template>

<script>
import UploadImageButton from "./UploadImageButton.vue";

export default {
  props: {
    imageData: String, // Receive image updates from parent
  },
  components: {
    UploadImageButton,
  },
  data() {
    return {
      originalImage: this.imageData || localStorage.getItem("imageData"), // Keep the original
      displayedImage: this.imageData || localStorage.getItem("imageData"), // Shown to user
      isProcessing: false,
      errorMessage: "",
      color: "#FFFFFF",
      backgroundImage: null,
      activeTab: "color",
      commonColors: ["#FFFFFF", "#FF0000", "#0000FF", "#FFFF00", "#000000"],
      sampleBackgrounds: [
        { name: "Office", url: "/office.jpeg" },
        { name: "Beach", url: "/beach.jpeg" },
        { name: "Forest", url: "/forest.jpeg" },
      ],
    };
  },
  watch: {
    imageData(newImage) {
      if (!this.originalImage) { // Set only if not set before
        this.originalImage = newImage;
      }
      this.displayedImage = newImage; // Only update display, not original
    },
  },

  methods: {
    async applyBackground(image) {
      if (typeof image === "string") {
        this.backgroundImage = image;
        try {
          const base64Image = await this.convertImageToBase64(image);
          this.processImage({ category: "background", backgroundString: base64Image, image: this.originalImage });
        } catch (error) {
          console.error("Error converting image:", error);
          this.errorMessage = "Failed to load background image.";
        }
      } else if (image instanceof File || image instanceof Blob) {
        const reader = new FileReader();
        reader.onload = (e) => {
          this.backgroundImage = e.target.result;
          this.processImage({ category: "background", backgroundString: this.backgroundImage, image: this.originalImage });
        };
        reader.readAsDataURL(image);
      }
    },


    async convertImageToBase64(imageUrl) {
      return new Promise((resolve, reject) => {
        fetch(imageUrl)
          .then((response) => response.blob())
          .then((blob) => {
            const reader = new FileReader();
            reader.onloadend = () => resolve(reader.result);
            reader.onerror = reject;
            reader.readAsDataURL(blob);
          })
          .catch(reject);
      });
    },

    tabClass(tab) {
      return `w-full py-2 border-b-2 ${
        this.activeTab === tab ? "border-blue-500 text-blue-500 font-semibold" : "border-transparent text-gray-500"
      } hover:text-gray-700`;
    },

    triggerUpload() {
      this.$refs.uploadButton.$el.click();
    },

    applyColor(eventOrColor) {
      const color = typeof eventOrColor === "string" ? eventOrColor : eventOrColor.target.value;
      this.color = color;
      this.processImage({ category: "color", colorString: color, image: this.originalImage });
    },



    async processImage(payload) {
      if (!this.originalImage) { // Ensure we always use the original
        this.errorMessage = "No image available for processing.";
        return;
      }

      this.isProcessing = true;
      this.errorMessage = "";

      try {
        // Always resize originalImage, not the modified one
        const finalBase64 = await this.resizeToClosestMultipleOf32(this.originalImage); 
        payload.image = finalBase64;

        const response = await fetch("http://localhost:8080/image/process", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });

        if (!response.ok) throw new Error("Processing failed");

        const result = await response.json();
        if (result.processedImage) {
          this.displayedImage = result.processedImage; // Update only the display
          localStorage.setItem("imageData", result.processedImage);

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
