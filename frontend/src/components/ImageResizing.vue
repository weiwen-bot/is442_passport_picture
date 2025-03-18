<template>
  <!-- Left side: Country Selection -->
  <h2 class="col-span-12 font-bold p-4 text-2xl">Image</h2>
  <div
    class="col-span-4 bg-white border rounded-lg shadow-lg p-4 space-y-2 text-black"
  >
    <h2 class="font-bold text-lg">Resize Your Image</h2>

    <div class="max-w-sm space-y-3 pt-2">
      <label
        for="country"
        class="block font-medium mb-2 font-semibold text-left"
        >Select a Country
      </label>
      <select
        v-model="selectedCountry"
        @change="handleResize"
        id="country"
        class="sm:py-3 ps-3 pe-10 block w-full rounded-lg border border-gray-300"
      >
        <option value="">-- Select Country --</option>
        <option
          v-for="country in countryList"
          :key="country.code"
          :value="country.code"
        >
          {{ country.name }} ({{ country.dimensions }})
        </option>
      </select>
    </div>

    <div class="max-w-sm space-y-3 pt-2">
      <label
        for="template"
        class="block font-medium mb-2 font-semibold text-left"
        >Or Choose a Popular Size
      </label>
      <select
        v-model="selectedTemplate"
        @change="handleResize"
        id="template"
        class="sm:py-3 ps-3 pe-10 block w-full rounded-lg border border-gray-300"
        :disabled="hasResized"
      >
        <option value="">-- Select Template --</option>
        <option
          v-for="template in templateList"
          :key="template.label"
          :value="template.size"
        >
          {{ template.label }} {{ template.size }}
        </option>
      </select>
    </div>

    <div class="max-w-sm space-y-3 pt-2">
      <label class="block font-medium mb-2 font-semibold text-left">
        Custom Size (Aspect Ratio Locked)
      </label>
      <div class="flex items-center space-x-2">
        <!-- Width Input -->
        <div class="flex items-center space-x-1">
          <input
            type="number"
            v-model.number="customWidth"
            @input="updateHeight"
            class="resize-input"
            :placeholder="originalWidth ? originalWidth + ' px' : 'Width'"
            :disabled="hasResized"
          />
          <span class="text-gray-600 text-sm px-label">px</span>
        </div>

        <!-- "×" Symbol -->
        <span class="text-gray-700 font-semibold text-lg">×</span>

        <!-- Height Input -->
        <div class="flex items-center space-x-1">
          <input
            type="number"
            v-model.number="customHeight"
            @input="updateWidth"
            class="resize-input"
            :placeholder="originalHeight ? originalHeight + ' px' : 'Height'"
            :disabled="hasResized"
          />
          <span class="text-gray-600 text-sm px-label">px</span>
        </div>
      </div>
    </div>
  </div>

  <!-- Right side: Image Display -->
  <div class="col-span-8 shadow-lg flex justify-center items-center">
    <img
      v-if="resizedImage"
      :src="resizedImage"
      alt="Resized"
      class="max-w-full max-h-[500px] w-auto h-auto object-contain border rounded shadow-md"
    />

    <img
      v-else-if="baseImage"
      :src="baseImage"
      alt="Original"
      class="h-full w-auto max-w-full object-contain border rounded shadow-md"
    />

    <div v-else class="text-gray-500 text-center">
      No image yet. Click "Resize" to generate an image.
    </div>
  </div>
</template>

<script>
export default {
  props: {
    imageData: String, // Parent passes the original image
    resetCounter: Number,
  },
  emits: ["resize-complete", "update:imageData"],
  data() {
    return {
      baseImage: this.imageData,
      resizedImage: null, // Stores resized image
      selectedCountry: "",
      selectedTemplate: "",
      countryList: [],
      templateList: [
        { label: "Instagram Post", size: "1080x1080" },
        { label: "Thumbnail", size: "150x150" },
        { label: "HD Wallpaper", size: "1920x1080" },
      ],
      hasResized: false,
      aspectRatio: 1,
      originalWidth: null, // Stores the actual width of the image
      originalHeight: null, // Stores the actual height of the image
      customWidth: null,
      customHeight: null,
    };
  },
  watch: {
    resetCounter() {
      this.resetResize();
    },

    imageData(newImage) {
      if (newImage) {
        this.extractImageDimensions(newImage);
      }
    },
  },
  mounted() {
    this.fetchCountryList();

    if (this.imageData) {
      this.extractImageDimensions(this.imageData); // Ensures dimensions are fetched immediately
    }
  },
  methods: {
    async fetchCountryList() {
      try {
        const response = await fetch("http://localhost:8080/image/countries");
        if (!response.ok) throw new Error("Failed to fetch country list");

        this.countryList = await response.json();
        return this.countryList;
      } catch (error) {
        console.error("Error fetching country list:", error);
        this.countryList = [];
        return [];
      }
    },
    extractImageDimensions(imageSrc) {
      if (!imageSrc) return;

      const img = new Image();
      img.onload = () => {
        this.originalWidth = img.width;
        this.originalHeight = img.height;
        this.aspectRatio = img.width / img.height;

        this.customWidth = this.originalWidth;
        this.customHeight = this.originalHeight;

        console.log(
          `Image dimensions extracted: ${this.originalWidth}x${this.originalHeight}`
        );
      };
      img.src = imageSrc;
    },
    async handleResize() {
      if (this.hasResized) return;

      const size =
        this.selectedCountry || `${this.customWidth}x${this.customHeight}`;
      if (!size) return;

      this.hasResized = true;
      try {
        const response = await fetch("http://localhost:8080/image/resize", {
          method: "POST",
          body: JSON.stringify({ image: this.imageData, size }),
          headers: { "Content-Type": "application/json" },
        });
        const result = await response.json();
        if (result.status === "success") {
          this.resizedImage = result.image;
          this.$emit("update:imageData", this.resizedImage);
        }
      } catch (error) {
        console.error("Error resizing image:", error);
      }
    },
    resetResize() {
      this.baseImage = this.imageData;
      this.resizedImage = null;
      this.hasResized = false;
      this.selectedCountry = "";
      this.selectedTemplate = "";
      this.customWidth = this.originalWidth;
      this.customHeight = this.originalHeight;
    },
    updateHeight() {
      if (this.customWidth) {
        this.customHeight = Math.round(this.customWidth / this.aspectRatio);
      }
    },
    updateWidth() {
      if (this.customHeight) {
        this.customWidth = Math.round(this.customHeight * this.aspectRatio);
      }
    },
  },
};
</script>

<style scoped>
select:disabled,
input:disabled {
  background-color: #f0f0f0;
  cursor: not-allowed;
  opacity: 0.7;
}

.px-label {
  display: flex;
  align-items: center;
  height: 100%;
}

.text-gray-700 {
  display: flex;
  align-items: center;
}

.resize-input {
  width: 80px; /* Set a fixed width to ensure consistency */
  min-width: 80px;
  max-width: 80px;
  padding: 6px;
  text-align: center;
  box-sizing: border-box; /* Ensures padding doesn't affect width */
  border: 1px solid #ccc; /* ✅ Explicitly define the border */
  border-radius: 6px; /* ✅ Keeps the rounded edges consistent */
  background-color: white; /* ✅ Ensures no transparency issues */
}

/* Loading spinner styling */
.spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border-left-color: #007bff;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

/* Keyframes for spinner animation */
@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
