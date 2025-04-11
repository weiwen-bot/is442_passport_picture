<template>

  <!-- Navbar -->
  <nav class="bg-white p-4">
    <div class="flex items-center">
      <button  @click="navigateToMainPage" class="text-black flex items-center bg-opacity-0">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7"></path>
        </svg>
        Back to Main Page
      </button>
    </div>
  </nav>
  <div class="min-h-screen bg-white text-gray-900 font-sans px-6 py-12">
    <h3 class="text-4xl font-semibold tracking-tight text-center mb-12">Instantly Generate Multiple Photo IDs</h3>

    <div class="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-10">
      <!-- Form Card -->
      <div class="bg-white rounded-3xl p-8 shadow-md space-y-6 border border-gray-200">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Upload Images</label>
          <input
            type="file"
            multiple
            accept="image/jpeg, image/png"
            @change="handleFiles"
            class="w-full px-4 py-3 bg-gray-100 border border-gray-300 text-gray-800 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Select Country</label>
          <select
            v-model="selectedCountry"
            class="w-full px-4 py-3 bg-gray-100 border border-gray-300 text-gray-800 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option disabled value="">-- Select a Country --</option>
            <option
          v-for="country in countryList"
          :key="country.code"
          :value="country.code"
        >
          {{ country.name }} ({{ country.dimensions }})
        </option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Images Per Page</label>
          <select
            v-model.number="imagesPerPage"
            class="w-full px-4 py-3 bg-gray-100 border border-gray-300 text-gray-800 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option :value="1">1</option>
            <option :value="4">4</option>
            <option :value="8">8</option>
          </select>
        </div>

        <div class="pt-4">
          <button
            @click="submitImages"
            :disabled="images.length === 0 || !selectedCountry"
            class="w-full px-4 py-3 bg-gray-100 hover:bg-gray-200 disabled:opacity-30 text-black font-medium rounded-xl transition-all border border-gray-300"
          >
            Submit All Images
          </button>
        </div>

        <!-- Only show this when resultImages exist AND processing is done -->
        <div v-if="!isProcessing && resultImages.length > 0">
          <button
            @click="showDownloadPopup = true"
            class="w-full mt-4 px-4 py-3 !bg-green-200 !hover:bg-green-300 text-black font-medium rounded-xl transition-all border border-gray-300"
          >
            Download All Images
          </button>
        </div>
      </div>

      <!-- Preview & Result Card -->
      <div class="bg-white rounded-3xl p-8 shadow-md border border-gray-200 relative">
        <!-- Placeholder message when no images are uploaded -->
        <div v-if="images.length === 0" class="absolute inset-0 flex items-center justify-center text-gray-500 text-xl font-semibold">
          Please upload 1 or more image(s)
        </div>

        <div v-if="totalPages > 1" class="flex justify-center items-center mb-6 gap-6">
          <button
            @click="prevPage"
            :disabled="currentPage === 1"
            class="px-4 py-2 rounded-xl bg-gray-100 hover:bg-gray-200 disabled:opacity-30 text-gray-800"
          >
            ← Prev
          </button>
          <span class="text-sm text-gray-500">Page {{ currentPage }} of {{ totalPages }}</span>
          <button
            @click="nextPage"
            :disabled="currentPage === totalPages"
            class="px-4 py-2 rounded-xl bg-gray-100 hover:bg-gray-200 disabled:opacity-30 text-gray-800"
          >
            Next →
          </button>
        </div>

        <div class="grid grid-cols-2 gap-5 mb-8">
          <div
            v-for="(img, index) in paginatedImages"
            :key="index"
            class="overflow-hidden rounded-xl border border-gray-200"
          >
            <img :src="img.url" class="w-full h-auto object-cover" />
          </div>
        </div>

        <div v-if="resultImages.length > 0">
          <h2 class="text-xl font-semibold mb-4 text-gray-800">Processed Images</h2>
          <div class="grid grid-cols-2 gap-5">
            <div
              v-for="(img, index) in resultImages"
              :key="index"
              class="overflow-hidden rounded-xl border border-gray-200"
            >
              <img :src="img" class="w-full h-auto object-cover" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Popup for Download Options -->
    <div v-if="showDownloadPopup" class="fixed inset-0 flex justify-center items-center z-50">
      <div class="absolute inset-0 backdrop-blur-lg"></div> <!-- This is the overlay with blur effect -->
      <div class="bg-gray-200 rounded-xl p-8 w-96 z-10">
        <h3 class="text-2xl font-semibold mb-6 text-center">Download Options</h3>

        <button
          @click="downloadImages"
          class="w-full px-4 py-3 mb-3 bg-blue-500 hover:bg-blue-700 text-black rounded-xl"
        >
          Download Image(s)
        </button>
        
        <button
          @click="downloadWithLayout(2, 2)"
          class="w-full px-4 py-3 mb-3 bg-blue-500 hover:bg-blue-600 text-black rounded-xl"
        >
          Download 2×2 Layout
        </button>
        
        <button
          @click="downloadWithLayout(4, 4)"
          class="w-full px-4 py-3 mb-3 bg-blue-500 hover:bg-blue-600 text-black rounded-xl"
        >
          Download 4×4 Layout
        </button>

        <!-- Custom Layout -->
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700">Custom Layout (Cols × Rows)</label>
          <div class="flex gap-4">
            <input
              v-model.number="customCols"
              type="number"
              min="1"
              class="w-full px-4 py-2 bg-gray-100 border border-gray-300 text-gray-800 rounded-xl focus:outline-none"
              placeholder="Columns"
            />
            <input
              v-model.number="customRows"
              type="number"
              min="1"
              class="w-full px-4 py-2 bg-gray-100 border border-gray-300 text-gray-800 rounded-xl focus:outline-none"
              placeholder="Rows"
            />
          </div>
        </div>

        <button
          @click="downloadCustomLayout"
          class="w-full px-4 py-3 mb-3 bg-blue-500 hover:bg-blue-600 text-black rounded-xl"
        >
          Download Custom Layout
        </button>

        <button
          @click="showDownloadPopup = false"
          class="w-full mt-4 px-4 py-3 bg-gray-200 text-black rounded-xl"
        >
          Close
        </button>
      </div>
    </div>

    <!-- Error Modal -->
    <div v-if="errorModal" class="fixed inset-0 flex justify-center items-center z-50">
      <div class="absolute inset-0 backdrop-blur-lg"></div>
      <div class="bg-gray-200 rounded-xl p-8 w-96 z-10">
        <h3 class="text-2xl font-semibold mb-6 text-center text-red-800">Error Processing Image</h3>
        <p class="text-gray-700 mb-6 text-center">There has been an error processing your image. Please try again or select a different image.</p>
        <div class="space-y-4">
          <button
            @click="errorModal = false"
            class="w-full px-4 py-3 !bg-red-500 !hover:bg-red-600 text-white rounded-xl"
          >
            Close
          </button>
          <button
            @click="navigateToMainPage"
            class="w-full px-4 py-3 bg-white hover:bg-white text-black rounded-xl"
          >
            Try Manually Processing
          </button>
        </div>
      </div>
    </div>


  </div>
</template>

<script>
export default {
  name: "ImageUploader",
  data() {
    return {
      images: [], // { file: File, url: string }
      imagesPerPage: 4,
      currentPage: 1,
      selectedCountry: "",
      template: "",
      countryList : [],
      resultImages: [],
      showDownloadPopup: false, 
      customCols: 1, // Default custom columns
      customRows: 1, // Default custom rows
      fileType: "image/png", // assuming PNG for this example
      errorModal: false, // Controls visibility of the error modal
    };
  },
  computed: {
    totalPages() {
      return Math.ceil(this.images.length / this.imagesPerPage);
    },
    paginatedImages() {
      const start = (this.currentPage - 1) * this.imagesPerPage;
      return this.images.slice(start, start + this.imagesPerPage);
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
    navigateToMainPage() {
      this.$router.push('/');
      this.errorModal = false; // Optionally close the modal upon navigation
    },
    handleFiles(event) {
      const files = Array.from(event.target.files);
      const validImages = files.filter((file) =>
        ["image/jpeg", "image/png"].includes(file.type)
      );

      this.images = [];
      this.currentPage = 1;

      validImages.forEach((file) => {
        const reader = new FileReader();
        reader.onload = (e) => {
          this.images.push({ file, url: e.target.result });
        };
        reader.readAsDataURL(file);
      });
    },
    nextPage() {
      if (this.currentPage < this.totalPages) {
        this.currentPage++;
      }
    },
    prevPage() {
      if (this.currentPage > 1) {
        this.currentPage--;
      }
    },
    async submitImages() {
      if (this.images.length === 0 || !this.selectedCountry) return;

      const formData = new FormData();
      this.images.forEach(({ file }) => {
        formData.append("image", file);
      });
      formData.append("country", this.selectedCountry);
      formData.append("template", this.template);

      try {
        const response = await fetch("http://localhost:8080/automate/batch/passportphoto", {
          method: "POST",
          body: formData,
        });

        if (!response.ok) throw new Error(`Upload failed: ${response.statusText}`);

        const result = await response.json();
        console.log(result);
        this.resultImages = result.processedImage || [];
        this.errorModal = false; // Hide error modal on success
      } catch (err) {
        console.error("Upload error:", err);
        this.errorModal = true; // Show error modal
        //reset resultImages in case of error
        this.resultImages = [];
        //reset images to empty array
        this.images = [];
      }
    },
    async downloadImages() {
      if (!this.resultImages || this.resultImages.length === 0) {
        alert("No processed images available!");
        return;
      }

      // Iterate over each processed image and trigger the download
      for (let i = 0; i < this.resultImages.length; i++) {
        const image = this.resultImages[i];
        
        // Check if image is a base64 URL or a Blob object
        if (typeof image === "string" && image.startsWith("data:image")) {
          // If it's a base64 string (data URL), create a Blob and trigger the download
          const blob = await this.base64ToBlob(image);
          this.triggerDownload(blob, `image_${i + 1}.${this.fileType.split("/").pop()}`);
        } else if (image instanceof Blob) {
          // If it's already a Blob object, trigger the download directly
          this.triggerDownload(image, `image_${i + 1}.${this.fileType.split("/").pop()}`);
        }
      }

      // Close the download popup after downloading images
      this.showDownloadPopup = false;
    },

    // Helper function to convert base64 to Blob
    base64ToBlob(base64) {
      const byteString = atob(base64.split(',')[1]);
      const arrayBuffer = new ArrayBuffer(byteString.length);
      const uintArray = new Uint8Array(arrayBuffer);
      
      for (let i = 0; i < byteString.length; i++) {
        uintArray[i] = byteString.charCodeAt(i);
      }

      const mimeString = base64.split(',')[0].split(':')[1].split(';')[0];
      return new Blob([uintArray], { type: mimeString });
    },



    async downloadWithLayout(cols, rows) {
      if (!this.resultImages || this.resultImages.length === 0) {
        alert("No processed images available!");
        return;
      }

      // Loop over each processed image and apply the chosen layout
      for (let i = 0; i < this.resultImages.length; i++) {
        const image = this.resultImages[i];
        this.imageData = image; // Set the image data to the current processed image

        // Generate the layout (e.g., 2x2, 4x4) for this image
        const blob = await this.generateCompositeImage(cols, rows);
        if (blob) {
          // Trigger the download for each image with the selected layout
          this.triggerDownload(
            blob,
            `image_${i + 1}_${cols}x${rows}_layout.${this.fileType.split("/").pop()}`
          );
        }
      }

      // Close the dropdown after download
      this.showDownloadPopup = false;
    },

    async downloadCustomLayout() {
      if (!this.resultImages || this.resultImages.length === 0) {
        alert("No processed images available!");
        return;
      }

      const cols = this.customCols;
      const rows = this.customRows;

      for (let i = 0; i < this.resultImages.length; i++) {
        const image = this.resultImages[i];
        this.imageData = image; // Set the image data to the current processed image

        const blob = await this.generateCompositeImage(cols, rows);
        if (blob) {
          this.triggerDownload(
            blob,
            `image_${i + 1}_${cols}x${rows}_custom_layout.${this.fileType.split("/").pop()}`
          );
        }
      }

      this.showDownloadPopup = false;
    },

    async generateCompositeImage(cols, rows) {
      return new Promise((resolve) => {
        if (!this.imageData) {
          console.error("No image data found!");
          resolve(null);
          return;
        }

        const img = new Image();
        img.onload = () => {
          const originalWidth = img.width;
          const originalHeight = img.height;
          const aspectRatio = originalWidth / originalHeight;

          const imgWidth = Math.floor(originalWidth / 2);
          const imgHeight = Math.floor(imgWidth / aspectRatio);

          const canvasWidth = cols * imgWidth + (cols + 1) * 10;
          const canvasHeight = rows * imgHeight + (rows + 1) * 10;

          const canvas = document.createElement("canvas");
          const ctx = canvas.getContext("2d");

          canvas.width = canvasWidth;
          canvas.height = canvasHeight;
          ctx.fillStyle = "white";
          ctx.fillRect(0, 0, canvas.width, canvas.height);

          for (let i = 0; i < rows; i++) {
            for (let j = 0; j < cols; j++) {
              ctx.drawImage(
                img,
                j * (imgWidth + 10) + 10,
                i * (imgHeight + 10) + 10,
                imgWidth,
                imgHeight
              );
            }
          }

          canvas.toBlob((blob) => resolve(blob), this.fileType);
        };
        img.src = this.imageData;
      });
    },

    triggerDownload(blob, filename) {
      const link = document.createElement("a");
      link.href = URL.createObjectURL(blob);
      link.download = filename;
      link.click();
    },
    
  },
};
</script>
