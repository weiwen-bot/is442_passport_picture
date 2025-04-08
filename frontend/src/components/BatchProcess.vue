<template>
    <div class="p-4">
      <h1 class="text-xl font-bold mb-4">Image Uploader</h1>
  
      <!-- File Upload -->
      <input
        type="file"
        multiple
        accept="image/jpeg, image/png"
        @change="handleFiles"
        class="mb-4"
      />
  
      <!-- Country Dropdown -->
      <div class="mb-4">
        <label class="mr-2 font-medium">Select Country:</label>
        <select v-model="selectedCountry" class="border px-2 py-1 rounded">
          <option disabled value="">-- Select a Country --</option>
          <option
            v-for="country in countries"
            :key="country.value"
            :value="country.value"
          >
            {{ country.name }}
          </option>
        </select>
      </div>
  
      <!-- Images Per Page Selector -->
      <div class="flex items-center mb-4">
        <label class="mr-2 font-medium">Images per page:</label>
        <select v-model.number="imagesPerPage" class="border px-2 py-1 rounded">
          <option :value="1">1</option>
          <option :value="4">4</option>
          <option :value="8">8</option>
        </select>
      </div>
  
      <!-- Image Previews (Before Submission) -->
      <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
        <div
          v-for="(img, index) in paginatedImages"
          :key="index"
          class="border rounded p-2"
        >
          <img :src="img.url" class="w-full h-auto object-cover" />
        </div>
      </div>
  
      <!-- Pagination Controls -->
      <div v-if="totalPages > 1" class="flex justify-center mt-6 space-x-2">
        <button
          @click="prevPage"
          :disabled="currentPage === 1"
          class="px-3 py-1 border rounded disabled:opacity-50"
        >
          Prev
        </button>
        <span class="px-2 py-1">Page {{ currentPage }} / {{ totalPages }}</span>
        <button
          @click="nextPage"
          :disabled="currentPage === totalPages"
          class="px-3 py-1 border rounded disabled:opacity-50"
        >
          Next
        </button>
      </div>
  
      <!-- Submit Button -->
      <div class="mt-6 text-center">
        <button
          @click="submitImages"
          :disabled="images.length === 0 || !selectedCountry"
          class="bg-blue-500 text-black px-4 py-2 rounded hover:bg-blue-600 disabled:opacity-50"
        >
          Submit All Images
        </button>
      </div>
  
      <!-- Display Processed Images from Backend -->
      <div v-if="resultImages.length" class="mt-6">
        <h2 class="text-xl font-bold mb-4">Processed Images</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
          <div
            v-for="(img, index) in resultImages"
            :key="index"
            class="border rounded p-2"
          >
            <img :src="img" class="w-full h-auto object-cover" />
          </div>
        </div>
      </div>
      <div class="mt-6 text-center">
        <button
          @click="downloadBase64Images"
          :disabled="images.length === 0 || !selectedCountry"
          class="bg-blue-500 text-black px-4 py-2 rounded hover:bg-blue-600 disabled:opacity-50"
        >
          Download all
        </button>
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
        countries: [
          { name: "Japan", value: "jpn" },
          { name: "United Kingdom", value: "UK" },
          { name: "Canada", value: "CA" },
          { name: "Germany", value: "DE" },
          { name: "India", value: "IN" },
        ],
        // Array to hold Base64 strings returned from the backend
        resultImages: [],
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
    methods: {
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

    //   downloadBase64Images() {
    //   // Loop through each Base64 image string and trigger a download.
    //   this.resultImages.forEach((base64, index) => {
    //     // Determine the file extension based on the Base64 header.
    //     let extension = "jpg"; // default extension
    //     if (base64.startsWith("data:image/png")) {
    //       extension = "png";
    //     }
    //     // Create an anchor element and set the Base64 string as its href.
    //     const a = document.createElement("a");
    //     a.href = base64;
    //     a.download = `image_${index + 1}.${extension}`;
    //     // Append the anchor to the document, click it, then remove it.
    //     document.body.appendChild(a);
    //     a.click();
    //     document.body.removeChild(a);
    //   })},

      async submitImages() {
        if (this.images.length === 0 || !this.selectedCountry)
          return;
  
        // Create FormData and append each file with key "image"
        const formData = new FormData();
        this.images.forEach(({ file }) => {
          formData.append("image", file); // Must match the backend parameter name
        });
        formData.append("country", this.selectedCountry);
        formData.append("template", this.template);
  
        try {
          const response = await fetch("http://localhost:8080/automate/batch/passportphoto", {
            method: "POST",
            body: formData,
          });
  
          if (!response.ok) {
            throw new Error(`Upload failed: ${response.statusText}`);
          }
  
          // Expecting a JSON response containing a list of Base64 image strings,
          // for example: { "images": ["data:image/jpeg;base64,...", ...] }
          const result = await response.json();
          // Update resultImages with the backend's returned images
          this.resultImages = result.processedImage || [];
          alert("Upload successful!");
          console.log("Server response:", result);
        } catch (err) {
          console.error("Upload error:", err);
          alert("Upload failed!");
        }
      },
        downloadBase64Images() {
      // Loop through each Base64 image string and trigger a download.
      this.resultImages.forEach((base64, index) => {
        // Determine the file extension based on the Base64 header.
        let extension = "jpg"; // default extension
        if (base64.startsWith("data:image/png")) {
          extension = "png";
        }
        // Create an anchor element and set the Base64 string as its href.
        const a = document.createElement("a");
        a.href = base64;
        a.download = `image_${index + 1}.${extension}`;
        // Append the anchor to the document, click it, then remove it.
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      })},
      
    },
  };
  </script>
  
  <style scoped>
  img {
    max-height: 300px;
  }
  </style>