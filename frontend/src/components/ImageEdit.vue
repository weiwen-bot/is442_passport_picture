<template>
  <!-- Main Image Edit (Parent Component) -->
  <!-- Sidebar -->
  <SidebarWrapper
    @update-action="handleAction"
    :sidebar-width="sidebarWidth"
    @reset-data="resetData"
  />

  <div class="p-1 grid grid-cols-12 gap-2 ml-[180px] mr-0">
    
    <!-- Show QuickGenerate Component when quick-generate is selected -->
    <QuickGenerate
      v-if="currentAction === 'quick-generate' && imageData"
      :imageData="imageData"
      @resize-complete="handleResizeComplete"
      @request-undo="handleUndo"
      @request-revert="handleReset"
      @request-redo="handleRedo"
    />
    
    <!-- Show ImageCropping Component when "Crop" is selected -->
    <!-- <ImageCropping
      v-if="currentAction === 'crop' && imageData && !isCropped"
      :key="imageData"
      v-model:imageData="imageData"
      @crop-complete="handleCropComplete"
    /> -->

    <!-- Show Cropped Image when cropping is done -->
    <!-- <div
      v-if="currentAction === 'crop' && isCropped"
      class="col-span-12 flex flex-col items-center justify-center h-screen"
    >
      <h2 class="text-lg font-semibold mb-4">Your Cropped Image</h2>
      <div class="flex justify-center items-center w-full">
        <img
          :src="imageData"
          class="h-auto max-w-full object-contain max-h-[70vh]"
          alt="Cropped Image"
        />
      </div>
    </div> -->
    <!-- Show Cropped Image when cropping is done -->
    <!-- <div
      v-if="currentAction === 'crop' && isCropped"
      class="col-span-4 shadow-lg"
    >
      <h2 class="text-lg font-semibold mb-2">Your Cropped Image</h2>
      <img
        :src="imageData"
        class="h-full w-auto max-w-full object-contain"
        alt="Cropped Image"
      />
    </div> -->

    <!-- Crop feature: Either show cropping interface or recrop interface based on state -->
    <template v-if="currentAction === 'crop' && imageData">
      <!-- Show cropper when not cropped yet -->
      <ImageCropping
        v-if="!isCropped"
        :key="imageData"
        v-model:imageData="imageData"
        @crop-complete="handleCropComplete"
      />

      <!-- Show recrop interface when already cropped -->
      <div v-else class="col-span-12 grid grid-cols-12 gap-4 p-4">
      <h2 class="col-span-12 font-bold p-4 text-2xl">Image</h2>
        <!-- Left side: Recrop options -->
        <div class="col-span-4 bg-white border rounded-lg shadow-lg p-4 space-y-2 text-black">
          <h2 class="font-bold text-lg">Your Cropped Image</h2>
          <div class="max-w-sm space-y-4 pt-3">
            <button
              class="sm:py-3 ps-3 pe-10 block w-full rounded-lg bg-green-500 text-white"
              @click="handleRecrop">
              Crop
            </button>
          </div>
        </div>
        
        <!-- Right side: Cropped image display -->
        <div class="col-span-8 shadow-lg flex justify-center items-center">
          <img
            :src="imageData"
            class="max-h-full w-auto object-contain"
            style="max-width: 100%; width: auto; height: auto;"
            alt="Cropped Image"
          />
        </div>
      </div>
    </template>

    <!-- Show BackgroundRemover Component when "Background Remover" is selected -->
    <BackgroundRemover
      v-if="currentAction === 'background-remover' && imageData"
      :key="imageData"
      v-model:imageData="imageData"
      @remove-background="handleRemoveBackground"
    />

    <!-- Show ImageResizing Component when "Resize" is selected -->
    <ImageResizing
      v-if="currentAction === 'resize' && imageData"
      :imageData="imageData"
      @resize-complete="handleResizeComplete"
      @request-undo="handleUndo"
      @request-revert="handleReset"
      @request-redo="handleRedo"
    />
    <!-- Show ImageEnhancement Component when "Enhance" is selected -->
    <ImageEnhancement
      v-if="currentAction === 'enhance' && imageData"
      :key="imageData"
      v-model:imageData="imageData"
      @enhance-complete="handleEnhanceComplete"
    />

    <div class="bg-black fixed bottom-0 left-0 w-full p-2 z-10">
      <div class="flex justify-end">
        <!-- Reset Button -->
        <button
          class="text-white bg-gray-800 p-2 rounded mr-3 flex items-center"
          @click="handleReset"  
        >
          <i class="fas fa-eraser mr-2"></i> Revert to Original
        </button>

        <!-- Undo Button -->
        <button
          :disabled="imageHistory.length === 0"
          class="text-white bg-gray-800 p-2 rounded mr-3 flex items-center"
          @click="handleUndo"
        >
          <i class="fas fa-undo mr-2"></i> Undo
        </button>

        <!-- Redo Button -->
        <button
          :disabled="redoHistory.length === 0"
          class="text-white bg-gray-800 p-2 rounded mr-3 flex items-center"
          @click="handleRedo"
        >
          <i class="fas fa-redo mr-2"></i> Redo
        </button>

        <div class="relative">
          <button
            @click="toggleDropdown"
            class="text-white bg-gray-800 p-2 rounded"
          >
            <i class="fa-solid fa-download"></i> Download
          </button>
          <div
            v-if="showDropdown"
            class="absolute bottom-12 right-0 bg-gray-800 shadow-md rounded p-3 space-y-2 w-52"
          >
            <button
              @click="downloadImage"
              class="block w-full text-left p-2 hover:bg-gray-200"
            >
              Download Image
            </button>
            <button
              @click="openLayoutPopup"
              class="block w-full text-left p-2 hover:bg-gray-200"
            >
              <i class="fa-solid fa-th-large"></i> Multiple Layouts
            </button>
            <button
              @click="handleGoogleDownload"
              class="block w-full text-left p-2 hover:bg-gray-200"
            >
              <i class="fa-solid fa-cloud-arrow-down"></i> Upload to Google
              Drive
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Revert Confirmation Modal -->
  <div v-if="showRevertModal" class="fixed inset-0 bg-gray-800 bg-opacity-50 flex items-center justify-center z-50">
    <div class="bg-white p-6 rounded shadow-lg w-96">
      <h3 class="text-xl font-semibold text-gray-900 mb-6">Revert to Original Image?</h3>
      <p class="text-gray-700 mb-4">This will discard all changes. Are you sure?</p>
      <div class="flex justify-end space-x-4">
        <button @click="cancelRevert" class="bg-gray-800 text-white px-4 py-2 rounded">Cancel</button>
        <button @click="confirmRevert" class="bg-red-600 text-white px-4 py-2 rounded">Confirm</button>
      </div>
    </div>
  </div>

  <!-- Layout Selection Modal -->
  <div
    v-if="showLayoutPopup"
    class="fixed inset-0 bg-gray-800 bg-opacity-50 flex items-center justify-center z-50"
  >
    <div class="bg-white p-6 rounded shadow-lg w-96">
      <h2 class="text-xl font-semibold text-gray-900 mb-6">Select Layout</h2>
      <button
        @click="downloadWithLayout(2, 2)"
        class="block w-full text-left p-2 bg-gray-100 hover:bg-gray-200 rounded mb-2"
      >
        2x2 Layout (4 images)
      </button>
      <button
        @click="downloadWithLayout(4, 6)"
        class="block w-full text-left p-2 bg-gray-100 hover:bg-gray-200 rounded mb-2"
      >
        4x6 Layout (24 images)
      </button>

      <!-- Custom Layout -->
      <h3 class="text-md font-bold mt-4 text-gray-900">Custom Layout</h3>
      <div class="flex space-x-4 pt-2">
        <!-- Columns Input (X) -->
        <div class="w-1/2">
          <label
            for="columns"
            class="block font-medium mb-2 font-semibold text-left text-gray-900"
          >
            Columns:
          </label>
          <div class="relative">
            <input
              type="number"
              id="columns"
              name="columns"
              class="sm:py-3 ps-3 pe-3 block w-full rounded-lg border border-gray-300 text-gray-900"
              v-model.number="columns"
              placeholder="Enter columns"
              step="1"
              min="1"
            />
          </div>
        </div>

        <!-- Rows Input (Y) -->
        <div class="w-1/2">
          <label
            for="rows"
            class="block font-medium mb-2 font-semibold text-left text-gray-900"
          >
            Rows:
          </label>
          <div class="relative">
            <input
              type="number"
              id="rows"
              name="rows"
              class="sm:py-3 ps-3 pe-3 block w-full rounded-lg border border-gray-300 text-gray-900"
              v-model.number="rows"
              placeholder="Enter rows"
              step="1"
              min="1"
            />
          </div>
        </div>
      </div>

      <button
        @click="downloadWithLayout(columns, rows)"
        class="mt-2 block w-full bg-blue-600 text-white p-2 rounded hover:bg-blue-700"
      >
        Download Custom Layout
      </button>

      <button
        @click="closeLayoutPopup"
        class="mt-4 block w-full bg-gray-800 text-white p-2 rounded"
      >
        Cancel
      </button>
    </div>
  </div>

  <!-- Confirmation Modal for Reset (Overlay on top of content)-->
  <div
    v-if="showResetModal"
    class="fixed inset-0 bg-gray-800 bg-opacity-50 flex items-center justify-center z-50"
  >
    <div class="bg-white p-6 rounded shadow-lg w-140">
      <h3 class="text-xl font-semibold text-gray-900 mb-10">
        Are you sure you want to upload new Image? <br />
        This will discard your current photo and all changes.
      </h3>
      <div class="flex justify-end space-x-4">
        <!-- Cancel Button (Same as Discard) -->
        <button
          @click="cancelReset"
          class="bg-gray-800 text-white px-4 py-2 rounded"
        >
          Cancel
        </button>
        <!-- Confirm Button (Dark Red) -->
        <button
          @click="confirmReset"
          class="bg-red-800 text-white px-4 py-2 rounded"
        >
          Confirm
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import ImageCropping from "./ImageCropping.vue";
import SidebarWrapper from "./SidebarWrapper.vue";
import BackgroundRemover from "./BackgroundRemover.vue";
import ImageResizing from "./ImageResizing.vue";
import ImageEnhancement from "./ImageEnhancement.vue";
import QuickGenerate from "./QuickGenerate.vue";

export default {
  components: {
    ImageCropping,
    SidebarWrapper,
    BackgroundRemover,
    ImageResizing,
    ImageEnhancement,
    QuickGenerate,
  },
  data() {
    return {
      imageData: null, // Image data that will be passed to child and updated after crop
      fileType: null,
      originalImage: null, // Store the original image
      imageHistory: [], // Stack for undo
      redoHistory: [], // Stack for redo
      currentAction: "crop", // Start with crop selected
      sidebarWidth: "240px", // Default width for expanded sidebar
      isCropped: false, // Track cropped state in the parent
      showResetModal: false, // Flag to show confirmation modal
      showDropdown: false, // Flag to show download dropdown
      accessToken: null, // Stores access token for Google authentication
      gisInited: false,
      tokenClient: null,
      SCOPES: "https://www.googleapis.com/auth/drive.file",
      CLIENT_ID: import.meta.env.VITE_GOOGLE_CLIENT_ID,
      resetCounter: 0,
      showLayoutPopup: false,
      columns: 2,
      rows: 2,
      padding: 20,
      showRevertModal: false,

    };
  },
  async mounted() {
    this.imageData = localStorage.getItem("imageData") || null;
    this.fileType = localStorage.getItem("fileType") || null;
    // Store the first uploaded image only ONCE
    if (!this.originalImage) {
      this.originalImage = this.imageData;
    }
    await this.loadScript(
      "https://accounts.google.com/gsi/client",
      this.gisLoaded
    );
  },
  watch: {
    imageData(newImageData) {
      if (newImageData) {
        // If imageData changes, you may still need to handle cropper updates
        const cropper = this.$refs.cropperComponent?.cropperInstance; // Access the cropper instance
        if (cropper) {
          cropper.replace(newImageData); // This updates the cropper with the new image
        }
      }
    },
  },
  methods: {
    // Add this new method to handle recropping
    handleRecrop() {
      // Reset the cropping state to show the cropper again
      this.isCropped = false;
    },

    handleAction(action) {
      console.log("Handling action:", action);
      this.currentAction = action;

      // if (action === "crop") {
      //   // If user clicks "Crop" again, allow re-cropping the already cropped image
      //   this.isCropped = false;
      // }

      this.$nextTick(() => {
        console.log("✅ currentAction updated to:", this.currentAction);
      });
    },

    handleUndo() {
      console.log("Undo button clicked! Current Action:", this.currentAction);
      if (this.imageHistory.length > 0) {
        console.log("Undoing in ImageEdit.vue");

        this.redoHistory.push(this.imageData); // Save current state to redo
        this.imageData = this.imageHistory.pop(); // Go back to previous state
        this.isCropped = false; // Reset cropped state
      }
    },
    handleRedo() {
      if (this.redoHistory.length > 0) {
        console.log("Redoing in ImageEdit.vue");

        this.imageHistory.push(this.imageData); // Save current state to undo
        this.imageData = this.redoHistory.pop(); // Restore last undone state
        this.isCropped = false; // Reset cropped state
      }
    },
    handleReset() {
      if (this.imageData !== this.originalImage) {
        console.log("Reverting to original in ImageEdit.vue");
        this.showRevertModal = true;
        this.imageHistory.push(this.imageData); // Save for undo
        this.redoHistory = []; // Clear redo history
        this.imageData = this.originalImage;
        this.isCropped = false; // Reset cropped state
      } else {
        console.log("No changes detected, modal will not be shown.");
      }
      console.log("Reverting to original in ImageEdit.vue");
    },

    // Revert Modal
    cancelRevert() {
      this.showRevertModal = false;
      this.$nextTick(() => {
        console.log("Modal should be hidden now");
      });
      this.$forceUpdate(); 
    }, 
    confirmRevert() {
      this.imageHistory.push(this.imageData);
      this.redoHistory = [];
      this.imageData = this.originalImage;
      this.showRevertModal = false;
    },

    // After crop complete
    handleCropComplete(croppedImage) {
      if (this.imageData) {
        this.imageHistory.push(this.imageData); // Save current state before replacing it
      }
      this.imageData = croppedImage; // Update the imageData with the new cropped image
      this.isCropped = true; // Set the flag to true
    },

    // Method to toggle sidebar width (collapsed or expanded)
    // toggleSidebar() {
    //   if (this.sidebarWidth === '250px') {
    //     this.sidebarWidth = '65px'; // Collapsed state width
    //   } else {
    //     this.sidebarWidth = '250px'; // Expanded state width
    //   }
    // },

    // after resize complete
    handleResizeComplete(resizedImage) {
      if (this.imageData) {
        this.imageHistory.push(this.imageData);
      }
      this.imageData = resizedImage;
    },

    // After process complete
    handleProcessComplete(processedImage) {
      if (this.imageData) {
        this.imageHistory.push(this.imageData); // Save current state before replacing it
      }
      this.imageData = processedImage;
    },

    // After enhancement complete
    handleEnhanceComplete(enhancedImage) {
      if (this.imageData) {
        this.imageHistory.push(this.imageData); // Save current state before replacing it
      }
      this.imageData = enhancedImage;
    },

    toggleDropdown() {
      this.showDropdown = !this.showDropdown;
    },

    async downloadImage() {
      try {
        if (!this.imageData) {
          console.error("No image data available for download.");
          return;
        }

        // Convert base64 to Blob
        const blob = await this.base64ToBlob(this.imageData, this.fileType);

        // File handling
        if (window.showSaveFilePicker) {
          const fileHandle = await window.showSaveFilePicker({
            suggestedName: `edited_image.${this.fileType.split("/").pop()}`,
            types: [
              {
                description: "Images",
                accept: { "image/*": [".png", ".jpg", ".jpeg"] },
              },
            ],
          });

          const writable = await fileHandle.createWritable();
          await writable.write(blob);
          await writable.close();
        } else {
          // Fallback method
          const link = document.createElement("a");
          link.href = URL.createObjectURL(blob);
          link.download = `edited_image.${this.fileType.split("/").pop()}`;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
        }
      } catch (error) {
        console.error("Error downloading the image:", error);
      }
    },

    // Multiple layout
    openLayoutPopup() {
      this.showLayoutPopup = true;
    },
    closeLayoutPopup() {
      this.showLayoutPopup = false;
    },
    async downloadWithLayout(cols, rows) {
      if (!this.imageData) {
        alert("No image available!");
        return;
      }

      // console.log(`Generating layout: ${cols}x${rows}`);

      const blob = await this.generateCompositeImage(cols, rows);
      if (blob) {
        this.triggerDownload(
          blob,
          `${cols}x${rows}_layout.${this.fileType.split("/").pop()}`
        );
      } else {
        console.error("Failed to generate image.");
      }
      this.closeLayoutPopup();
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
          // console.log("Image loaded successfully.");

          const originalWidth = img.width;
          const originalHeight = img.height;
          const aspectRatio = originalWidth / originalHeight;

          // Calculate new image size based on original resolution
          const imgWidth = Math.floor(originalWidth / 2); // Reduce slightly to fit layout
          const imgHeight = Math.floor(imgWidth / aspectRatio);

          // Canvas size: (cols * image width) + padding
          const canvasWidth = cols * imgWidth + (cols + 1) * this.padding;
          const canvasHeight = rows * imgHeight + (rows + 1) * this.padding;

          const canvas = document.createElement("canvas");
          const ctx = canvas.getContext("2d");

          // Set high resolution for better quality
          canvas.width = canvasWidth;
          canvas.height = canvasHeight;
          ctx.fillStyle = "white";
          ctx.fillRect(0, 0, canvas.width, canvas.height);

          // Draw images in the grid
          for (let i = 0; i < rows; i++) {
            for (let j = 0; j < cols; j++) {
              const x = this.padding + j * (imgWidth + this.padding);
              const y = this.padding + i * (imgHeight + this.padding);
              ctx.drawImage(img, x, y, imgWidth, imgHeight);
            }
          }

          // console.log("Canvas image created successfully.");
          canvas.toBlob((blob) => {
            resolve(blob);
          }, this.fileType);
        };

        img.onerror = () => {
          console.error("Error loading image.");
          alert("Error loading the image. Please try again.");
          resolve(null);
        };

        img.src = this.imageData; // Ensure this is set **after** onload
      });
    },
    triggerDownload(blob, filename) {
      if (!blob) {
        console.error("No blob data for download.");
        return;
      }

      const link = document.createElement("a");
      link.href = URL.createObjectURL(blob);
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      // console.log(`Download triggered for: ${filename}`);
    },

    // Show the reset modal
    resetData() {
      this.showResetModal = true;
    },

    // Close the modal without resetting data
    cancelReset() {
      this.showResetModal = false;
    },

    // Reset data and image state
    confirmReset() {
      localStorage.removeItem("imageData");
      this.imageData = null;
      this.isCropped = false;
      this.imageHistory = [];

      // Close the modal
      this.showResetModal = false;

      // Navigate to the 'image-upload' page
      this.$router.push({ name: "ImageUpload" });
    },

    // Convert base64 image to Blob
    base64ToBlob(base64, contentType = "") {
      const byteCharacters = atob(base64.split(",")[1]); // Remove header from base64
      const byteNumbers = new Array(byteCharacters.length);
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }
      const byteArray = new Uint8Array(byteNumbers);
      return new Blob([byteArray], { type: contentType });
    },

    // load gdrive
    loadScript(src, callback) {
      return new Promise((resolve, reject) => {
        const existingScript = document.querySelector(`script[src="${src}"]`);
        if (existingScript) {
          if (callback) {
            callback();
          }
          resolve();
          return;
        }
        const script = document.createElement("script");
        script.src = src;
        script.async = true;
        script.defer = true;
        script.onload = () => {
          if (callback) {
            callback();
          }
          resolve();
        };
        script.onerror = (error) => {
          reject(new Error(`Failed to load script: ${src}`));
        };
        document.body.appendChild(script);
      });
    },

    // initialize Google Identity Services (GIS) for OAuth authentication
    gisLoaded() {
      this.tokenClient = google.accounts.oauth2.initTokenClient({
        client_id: this.CLIENT_ID,
        scope: this.SCOPES,
        callback: "",
      });
      this.gisInited = true;
    },

    // Trigger Google Drive download
    async handleGoogleDownload() {
      this.tokenClient.callback = async (response) => {
        if (response.error !== undefined) {
          throw response;
        }
        this.accessToken = response.access_token;
        this.uploadImageToGoogleDrive();
      };

      if (this.accessToken === null) {
        this.tokenClient.requestAccessToken({ prompt: "consent" });
      } else {
        this.tokenClient.requestAccessToken({ prompt: "" });
      }
    },

    // Upload image to Google Drive
    async uploadImageToGoogleDrive() {
      if (!this.imageData || !this.accessToken) {
        console.error("No image data or access token available.");
        return;
      }

      // Convert base64 image data to a Blob
      const imageBlob = await this.base64ToBlob(this.imageData, this.fileType);

      // Define metadata
      const metadata = {
        name: `edited_image.${this.fileType.split("/").pop()}`,
        mimeType: this.fileType,
      };

      const metadataBlob = new Blob([JSON.stringify(metadata)], {
        type: "application/json",
      });

      const formData = new FormData();
      formData.append("metadata", metadataBlob);
      formData.append("file", imageBlob);

      try {
        const uploadResponse = await fetch(
          "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart",
          {
            method: "POST",
            headers: {
              Authorization: `Bearer ${this.accessToken}`,
            },
            body: formData,
          }
        );

        const result = await uploadResponse.json();

        if (uploadResponse.ok) {
          console.log("Image uploaded successfully!");
          console.log(`https://drive.google.com/file/d/${result.id}/view`);
        } else {
          console.error("Upload failed:", result.error);
        }
      } catch (error) {
        console.error("Error during upload:", error);
      }
    },
  },
};
</script>

<style>
.bg-gray-800 {
  background-color: #2d3748 !important;
}
.bg-red-800 {
  background-color: #c53030 !important; /* Dark Red */
}
</style>
