<template>
  <!-- Left side: Country Selection -->
  <h2 class="col-span-12 font-bold p-4 text-2xl">Image</h2>
  <div
    class="col-span-4 bg-white border rounded-lg shadow-lg p-4 space-y-2 text-black"
  >
    <h2 class="font-bold text-lg">Resize Your Image</h2>
    <div class="max-w-sm space-y-3 pt-2">
      <div>
        <label
          for="country"
          class="block font-medium mb-2 font-semibold text-left"
          >Select a Country</label
        >
        <select
          v-model="selectedCountry"
          @change="saveSelectedCountry"
          id="country"
          class="sm:py-3 ps-3 pe-10 block w-full rounded-lg border border-gray-300"
        >
          <option value="">-- Select Country --</option>
          <option
            v-for="country in countryList"
            :key="country.code"
            :value="country.code"
          >
            {{ country.name }}
          </option>
        </select>
      </div>
    </div>
    <div class="max-w-sm space-y-4 pt-3">
      <button
        @click="handleResize"
        :class="resizeButtonClass"
        :disabled="!baseImage || !selectedCountry || isLoading"
        class="sm:py-3 ps-3 pe-10 block w-full rounded-lg text-black"
      >
        Resize
      </button>
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
    resetCounter: {
      type: Number,
      default: 0,
    },
    originalImage: String, // Receive the first uploaded image from the parent
  },
  emits: [
    "resize-complete",
    "update:imageData",
    "update:imageHistory",
    "update:redoHistory",
  ],
  data() {
    return {
      baseImage: null, // Stores the working image (resets when needed)
      resizedImage: null, // Stores resized image
      selectedCountry: "",
      countryList: [],
      isLoading: false,

      // Local history for undo/redo within Image Resizing page
      localImageHistory: [],
      localRedoHistory: [],
    };
  },
  mounted() {
    // Store the uploaded image only when first mounting
    if (!this.originalImage) {
      this.originalImage = this.imageData;
    }

    this.baseImage = this.imageData;

    console.log("ImageResizing mounted - ready to handle undo/redo");
  },

  computed: {
    resizeButtonClass() {
      if (!this.originalImage || this.isLoading) {
        return "bg-gray-400 text-white font-bold py-2 px-4 rounded opacity-50 cursor-not-allowed border border-gray-500"; // Disabled (gray but still a button)
      }
      if (this.selectedCountry) {
        return "bg-green-500 hover:bg-green-600 text-white font-bold py-2 px-4 rounded border border-green-600"; // Green when country selected
      }
      return "bg-gray-400 text-white font-bold py-2 px-4 rounded opacity-50 cursor-not-allowed border border-gray-500"; // Default gray
    },
  },
  created() {
    console.log("Component created with imageData:", this.imageData);

    this.resizedImage = null;

    const storedCountry = localStorage.getItem("selectedCountry");

    // Fetch the country list first
    this.fetchCountryList().then(() => {
      // After countries are loaded, validate the stored country
      if (
        storedCountry &&
        this.countryList.some((country) => country.code === storedCountry)
      ) {
        this.selectedCountry = storedCountry;
        console.log("Restored saved country:", this.selectedCountry);
      } else {
        this.selectedCountry = ""; // Default to "-- Select Country --"
        console.log("Saved country not found or invalid, using default");
      }
    });
  },
  beforeUnmount() {
    // Only update parent with resized image if it exists
    if (this.resizedImage && this.resizedImage !== this.originalImage) {
      console.log("Leaving - passing resized image to parent");
      this.$emit("update:imageData", this.resizedImage);
      this.$emit("update:imageHistory", this.localImageHistory);
      this.$emit("update:redoHistory", this.localRedoHistory);
    }
  },

  watch: {
    resetCounter(newVal, oldVal) {
      if (newVal !== oldVal) {
        console.log(`resetCounter changed: ${oldVal} → ${newVal}`);
        this.baseImage = null; // Temporarily set to null
        this.$nextTick(() => {
          this.baseImage = this.originalImage; // Ensure Vue detects a full update
          this.resizedImage = null; // Clear resized image
        });
      }
    },

    imageData(newImage, oldImage) {
      if (newImage !== oldImage) {
        console.log("🖼 imageData updated in ImageResizing.vue:", newImage);
        this.baseImage = ""; // Temporarily clear to force reactivity
        this.$nextTick(() => {
          this.baseImage = newImage; // Set correct image after reactivity updates
          this.resizedImage = null;
        });
      }
    },
  },

  methods: {
    handleReset() {
      console.log("🔄 Resetting ImageResizing.vue to the uploaded image!");
      this.localImageHistory.push(this.baseImage);
      this.baseImage = this.originalImage; // Reset displayed image
      this.resizedImage = null; // Clear resized image
      this.$emit("update:imageData", this.originalImage); // Notify parent of reset
    },

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
    saveSelectedCountry() {
      localStorage.setItem("selectedCountry", this.selectedCountry);
      console.log("✅ Selected country saved:", this.selectedCountry);
    },
    async handleResize() {
      if (!this.imageData || !this.selectedCountry) {
        alert("Please select a country first.");
        return;
      }

      this.isLoading = true;
      try {
        // Convert base64 to Blob
        const file = this.base64ToFile(this.imageData, "uploaded-image.jpg");

        // Create FormData
        const formData = new FormData();
        formData.append("image", file);
        formData.append("country", this.selectedCountry);

        // API Call
        const response = await fetch("http://localhost:8080/image/resize", {
          method: "POST",
          body: formData,
        });

        if (!response.ok) {
          throw new Error("Image resizing failed");
        }

        const result = await response.json();

        if (result.status === "success" && result.image) {
          console.log(
            "Received resized image:",
            result.image ? result.image.substring(0, 50) + "..." : "NULL"
          );

          this.localImageHistory.push(this.baseImage);
          this.localRedoHistory = [];

          // Set the resized image
          this.resizedImage = result.image;

          this.baseImage = result.image;
        }
      } catch (error) {
        console.error("Error resizing image:", error);
        alert("Error resizing image: " + error.message);
      } finally {
        this.isLoading = false;
      }
    },

    handleLocalUndo() {
      console.log(
        "📜 localImageHistory length:",
        this.localImageHistory.length
      );
      if (this.localImageHistory.length > 0) {
        console.log("↩️ Local Undo in ImageResizing.vue");

        // Store current image in redo history **only if it's different**
        if (
          this.baseImage !==
          this.localImageHistory[this.localImageHistory.length - 1]
        ) {
          this.localRedoHistory.push(this.baseImage);
        }

        this.baseImage = "";
        this.$nextTick(() => {
          this.baseImage = this.localImageHistory.pop();
          this.resizedImage = this.baseImage; // Also update resizedImage
          this.$emit("update:imageData", this.baseImage); // ✅ Notify parent!
        });
      }
    },

    handleLocalRedo() {
      if (this.localRedoHistory.length > 0) {
        console.log("↪️ Local Redo in ImageResizing.vue");
        this.localImageHistory.push(this.baseImage);
        // Restore the last undone image
        this.baseImage = "";
        this.$nextTick(() => {
          this.baseImage = this.localRedoHistory.pop();
          this.resizedImage = this.baseImage; // Also update resizedImage
          this.$emit("update:imageData", this.baseImage); // Emit to update parent
        });
      }
    },

    handleLocalReset() {
      console.log("🔄 Local Reset in ImageResizing.vue");
      this.localImageHistory.push(this.baseImage);
      this.baseImage = this.originalImage;
      this.resizedImage = null;
      this.$emit("update:imageData", this.originalImage);
    },

    base64ToFile(base64String, fileName) {
      const arr = base64String.split(",");
      const mime = arr[0].match(/:(.*?);/)[1];
      const bstr = atob(arr[1]);
      let n = bstr.length;
      const u8arr = new Uint8Array(n);
      while (n--) {
        u8arr[n] = bstr.charCodeAt(n);
      }
      return new File([u8arr], fileName, { type: mime });
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
