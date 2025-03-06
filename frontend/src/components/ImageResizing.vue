<template>
  <div class="w-full h-full flex justify-center items-center">
    <!-- Main Container -->
    <div class="w-full max-w-4xl bg-white shadow-lg p-6 rounded-lg">
      <h2 class="text-xl font-bold text-center">Resize Your Image</h2>

      <div class="grid grid-cols-2 gap-6">
        <!-- Original Image Section -->
        <div class="flex flex-col items-center">
          <h3 class="font-semibold">Original Image</h3>
          <img
            v-if="originalImage"
            :src="originalImage"
            alt="Original"
            class="w-full max-h-[500px] object-contain border rounded shadow-md"
          />
          <div v-else class="text-gray-500">No image loaded</div>
        </div>

        <!-- Resized Image Section -->
        <div class="flex flex-col items-center">
          <h3 class="font-semibold">Resized Image</h3>
          <img
            v-if="resizedImage"
            :src="resizedImage"
            alt="Resized"
            class="w-full max-h-[500px] object-contain border rounded shadow-md"
          />
          <div v-else class="text-gray-500">Select a country to resize</div>
        </div>
      </div>

      <!-- Country Dropdown -->
      <div class="mt-4">
        <label for="country" class="font-semibold block text-center"
          >Select a Country</label
        >
        <select
          v-model="selectedCountry"
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
      </div>

      <!-- Actions -->
      <div class="flex justify-center mt-4 gap-3">
        <button
          @click="handleResize"
          class="bg-blue-500 hover:bg-blue-600 text-white font-bold py-2 px-4 rounded disabled:opacity-50"
          :disabled="!originalImage || !selectedCountry || isLoading"
        >
          Resize
        </button>

        <button
          @click="handleDownload"
          class="bg-green-500 hover:bg-green-600 text-white font-bold py-2 px-4 rounded disabled:opacity-50"
          :disabled="!resizedImage || isLoading"
        >
          Download
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    imageData: String,
  },
  data() {
    return {
      originalImage: null,
      resizedImage: null,
      selectedCountry: "",
      countryList: [],
      isLoading: false,
    };
  },
  created() {
    console.log("Checking props:", this.imageData);
    console.log("Checking localStorage:", localStorage.getItem("imageData"));

    this.originalImage = this.imageData || localStorage.getItem("imageData");

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
          this.resizedImage = result.image;
          localStorage.setItem("resizedImage", this.resizedImage);
        } else {
          throw new Error(result.message || "Resizing failed");
        }
      } catch (error) {
        console.error("Error resizing image:", error);
        alert("Error resizing image: " + error.message);
      } finally {
        this.isLoading = false;
      }
    },

    handleDownload() {
      if (!this.resizedImage) return;
      const downloadLink = document.createElement("a");
      downloadLink.href = this.resizedImage;
      const fileExtension = this.resizedImage.includes("image/png")
        ? ".png"
        : ".jpg";
      downloadLink.download = `id-photo-${this.selectedCountry}${fileExtension}`;

      document.body.appendChild(downloadLink);
      downloadLink.click();
      document.body.removeChild(downloadLink);
    },

    handleRetake() {
      localStorage.removeItem("imageData");
      localStorage.removeItem("resizedImage");
      this.$router.push({ name: "ImageUpload" });
    },

    goToRemoveBackground() {
      this.$router.push({ name: "RemoveBackground" });
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

<style>
.bg-gray-800 {
  background-color: #2d3748 !important;
}

.spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border-left-color: #007bff;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
