<template>
  <!-- Left side: Country Selection -->
  <div
    class="col-span-1 flex flex-col bg-white border rounded-lg shadow-lg p-4 space-y-4 text-black"
  >
    <h2 class="font-bold text-lg">Resize Your Image</h2>

    <label for="country" class="font-semibold">Select a Country</label>
    <select
      v-model="selectedCountry"
      @change="saveSelectedCountry"
      id="country"
      class="border border-gray-300 rounded p-2 w-full text-black bg-white"
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

    <button
      @click="handleResize"
      :class="resizeButtonClass"
      :disabled="!originalImage || !selectedCountry || isLoading"
    >
      Resize
    </button>
  </div>

  <!-- Right side: Image Display -->
  <div class="col-span-4 flex flex-col items-center space-y-4">
    <h3 class="font-semibold">
      {{ resizedImage ? "Resized Image" : "Original Image" }}
    </h3>

    <img
      v-if="resizedImage"
      :src="resizedImage"
      alt="Resized"
      class="max-w-full max-h-[500px] w-auto h-auto object-contain border rounded shadow-md"
    />

    <img
      v-else-if="originalImage"
      :src="originalImage"
      alt="Original"
      class="max-w-full max-h-[500px] w-auto h-auto object-contain border rounded shadow-md opacity-70"
    />

    <div v-else class="text-gray-500 text-center">
      No image yet. Click "Resize" to generate an image.
    </div>
  </div>
</template>

<script>
export default {
  props: {
    imageData: String,
  },
  emits: ["resize-complete", "discard-resize", "update:imageData"],
  data() {
    return {
      originalImage: null,
      resizedImage: null,
      selectedCountry: "",
      countryList: [],
      isLoading: false,
    };
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
    console.log("Checking props:", this.imageData);

    // Load the original image from the parent component's data
    this.originalImage = this.imageData;

    this.resizedImage = null;

    // Load the previously selected country if any
    this.selectedCountry = localStorage.getItem("selectedCountry") || "";

    if (!this.originalImage) {
      console.warn("No image found! Redirecting to upload.");
      this.$router.push({ name: "ImageUpload" });
    }

    this.fetchCountryList(); // Fetch countries when component loads
  },
  methods: {
    async fetchCountryList() {
      try {
        const response = await fetch("http://localhost:8080/image/countries");
        if (!response.ok) throw new Error("Failed to fetch country list");

        this.countryList = await response.json();
      } catch (error) {
        console.error("Error fetching country list:", error);
      }
    },
    saveSelectedCountry() {
      localStorage.setItem("selectedCountry", this.selectedCountry);
      console.log("✅ Selected country saved:", this.selectedCountry);
    },
    async handleResize() {
      if (!this.originalImage || !this.selectedCountry) {
        alert("Please select a country first.");
        return;
      }

      this.isLoading = true;
      try {
        // Convert base64 to Blob
        const file = this.base64ToFile(
          this.originalImage,
          "uploaded-image.jpg"
        );

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

          // Set the resized image
          this.resizedImage = result.image;

          // Emit the resize-complete event ONCE after the image is set
          if (this.resizedImage) {
            console.log("Emitting resize-complete event");
            // Update the parent component with the new image
            this.$emit("update:imageData", this.resizedImage);

            // Emit the resize-complete event
            this.$emit("resize-complete", this.resizedImage);
          } else {
            console.error(
              "🚨 resizedImage is NULL, not storing in localStorage."
            );
          }
        }
      } catch (error) {
        console.error("Error resizing image:", error);
        alert("Error resizing image: " + error.message);
      } finally {
        this.isLoading = false;
      }
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
  all: unset; /* Reset inherited styles */
  display: inline-block; /* Keeps button visible */
  padding: 0.5rem 1rem; /* Maintain padding */
  border-radius: 0.375rem; /* Keep rounded corners */
  font-weight: bold; /* Maintain bold text */
  text-align: center; /* Ensure text stays centered */
  font-family: inherit; /* Keep the same font as the rest of the app */
  border: 1px solid transparent; /* Prevent shifting when enabling/disabling */
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
