<template>
  <div class="grid grid-cols-5 gap-4">
    <!-- Left Side: Country Selection & Actions -->
    <div
      class="col-span-1 flex flex-col bg-white border rounded-lg shadow-lg p-2 space-y-2 text-black"
    >
      <h2 class="font-bold">Resize Your Image</h2>
      <label for="country" class="font-semibold">Select Country:</label>
      <select
        id="country"
        v-model="selectedCountry"
        class="p-2 border border-gray-300 rounded-md"
      >
        <option value="" disabled>Select a country</option>
        <option value="jpn">Japan (35mm x 45mm)</option>
        <option value="usa">United States (51mm x 51mm)</option>
        <option value="sgp">Singapore (35mm x 45mm)</option>
        <option value="chn">China (33mm x 48mm)</option>
        <option value="mas">Malaysia (35mm x 50mm)</option>
      </select>

      <button
        class="text-white bg-gray-800 p-2 rounded mt-4"
        @click="handleResize"
        :disabled="!originalImage || !selectedCountry || isLoading"
      >
        Resize
      </button>

      <button
        class="text-white bg-green-600 p-2 rounded"
        @click="handleDownload"
        :disabled="!resizedImage || isLoading"
      >
        Download
      </button>

      <button
        class="text-white bg-gray-500 p-2 rounded"
        @click="handleRetake"
        :disabled="isLoading"
      >
        Upload Another Image
      </button>

      <button
        class="text-white bg-blue-600 p-2 rounded"
        @click="goToRemoveBackground"
        :disabled="!resizedImage || isLoading"
      >
        Next: Remove Background
      </button>
    </div>

    <!-- Right Side: Image Display -->
    <div class="col-span-4 shadow-lg p-4 bg-white border rounded-lg">
      <div class="flex justify-center gap-4">
        <div class="flex flex-col items-center">
          <h3 class="font-semibold">Original Image</h3>
          <img
            v-if="originalImage"
            :src="originalImage"
            alt="Original"
            class="max-w-xs border rounded"
          />
          <div v-else class="text-gray-500">No image loaded</div>
        </div>

        <div class="flex flex-col items-center">
          <h3 class="font-semibold">Resized Image</h3>
          <img
            v-if="resizedImage"
            :src="resizedImage"
            alt="Resized"
            class="max-w-xs border rounded"
          />
          <div v-else class="text-gray-500">Select a country to resize</div>
        </div>
      </div>
    </div>

    <!-- Loading Spinner -->
    <div
      v-if="isLoading"
      class="absolute inset-0 flex flex-col justify-center items-center bg-white bg-opacity-80"
    >
      <div class="spinner"></div>
      <p>Processing your image...</p>
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
      isLoading: false,
    };
  },
  created() {
    // Load image from props (if provided) or localStorage (fallback)
    this.originalImage = this.imageData || localStorage.getItem("imageData");
  },
  methods: {
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
