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
      <div class="flex space-x-2">
        <input
          type="number"
          v-model.number="customWidth"
          @input="updateHeight"
          class="sm:py-2 px-3 block w-1/2 rounded-lg border border-gray-300"
          placeholder="Width"
          :disabled="hasResized"
        />
        <span class="self-center"></span>
        <input
          type="number"
          v-model.number="customHeight"
          @input="updateWidth"
          class="sm:py-2 px-3 block w-1/2 rounded-lg border border-gray-300"
          placeholder="Height"
          :disabled="hasResized"
        />
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
      customWidth: null,
      customHeight: null,
    };
  },
  watch: {
    resetCounter() {
      this.resetResize();
    },
  },
  mounted() {
    this.fetchCountryList();
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
    async handleResize() {
      if (this.hasResized) return;

      const size = this.selectedTemplate || this.selectedCountry;
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
      this.customWidth = null;
      this.customHeight = null;
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
/* Ensure button styling is not overridden */
button {
  /* all: unset; /* Reset inherited styles */
  display: inline-block; /* Keeps button visible */
  padding: 0.5rem 1rem; /* Maintain padding */
  border-radius: 0.375rem; /* Keep rounded corners */
  font-weight: bold; /* Maintain bold text */
  text-align: center; /* Ensure text stays centered */
  font-family: inherit; /* Keep the same font as the rest of the app */
  border: 1px solid transparent; /* Prevent shifting when enabling/disabling */
  color: black;
}

/* Default disabled button (light gray but visible) */
.bg-gray-400 {
  background-color: #a0aec0 !important; /* Light gray but visible */
  border: 1px solid #718096 !important; /* Adds a border to make it look clickable */
}

/* Active button (Green) */
.bg-green-500 {
  background-color: #48bb78 !important; /* Green */
}

.bg-green-600 {
  background-color: #38a169 !important; /* Darker Green */
}

/* Fix disabled button issue */
.disabled-button {
  background-color: #a0aec0 !important; /* Light gray */
  color: white !important;
  cursor: not-allowed !important;
  opacity: 0.6;
  border: 1px solid #718096; /* Ensure it still looks like a button */
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
