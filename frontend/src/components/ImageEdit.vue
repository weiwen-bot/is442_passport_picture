<template>
  <!-- Main Image Edit (Parent Component) -->
  <!-- Sidebar -->
  <SidebarWrapper
    @update-action="handleAction"
    :sidebar-width="sidebarWidth"
    @reset-data="resetData"
  />

  <div class="p-1 grid grid-cols-12 gap-2 ml-[180px] mr-0">
    <!-- Show ImageCropping Component when "Crop" is selected -->
    <ImageCropping
      v-if="currentAction === 'crop' && imageData && !isCropped"
      :key="imageData"
      v-model:imageData="imageData"
      @crop-complete="handleCropComplete"
      @discard-crop="handleDiscard"
    />

    <!-- Show Cropped Image when cropping is done -->
    <div
      v-if="currentAction === 'crop' && isCropped"
      class="col-span-4 shadow-lg"
    >
      <h2 class="text-lg font-semibold mb-2">Your Cropped Image</h2>
      <img
        :src="imageData"
        class="h-full w-auto max-w-full object-contain"
        alt="Cropped Image"
      />
    </div>

    <!-- Show BackgroundRemover Component when "Background Remover" is selected -->
    <BackgroundRemover
      v-if="currentAction === 'background-remover' && imageData"
      :key="imageData"
      v-model:imageData="imageData"
      @remove-background="handleRemoveBackground"
      @discard-background="handleDiscard"
    />

    <!-- Show ImageResizing Component when "Resize" is selected -->
    <ImageResizing
      v-if="currentAction === 'resize' && imageData"
      :key="imageData"
      :reset-counter="resetCounter"
      v-model:imageData="imageData"
      @resize-complete="handleResizeComplete"
      @discard-resize="handleDiscard"
    />

    <!-- Show PhotoEnhancement Component when "Enhance" is selected -->
    <PhotoEnhancement
      v-if="currentAction === 'enhance' && imageData"
      :key="imageData"
      v-model:imageData="imageData"
      @enhancement-complete="handleEnhancementComplete"
      @discard-resize="handleDiscard"
    />

    <div class="bg-black fixed bottom-0 left-0 w-full p-2 z-10">
      <div class="flex justify-end">
        <!-- Discard Button -->
        <button
          class="text-white bg-gray-800 p-2 rounded mr-3"
          @click="handleReset"
        >
          Reset Original Image
        </button>
        <button
          :disabled="imageHistory.length === 0"
          class="text-white bg-gray-800 p-2 rounded mr-3"
          @click="handleUndo"
        >
          Undo Previous Action
        </button>

        <!-- Download Button -->
        <!-- <button
          @click="downloadImage"
          class="text-white bg-gray-800 p-2 rounded"
        >
          Download
        </button> -->
        <div class="relative">
          <button
            @click="toggleDropdown"
            class="text-white bg-gray-800 p-2 rounded"
          >
            Download
          </button>
          <div
            v-if="showDropdown"
            class="absolute bottom-12 right-0 bg-gray-800 shadow-md rounded p-3 space-y-2 w-48"
          >
            <button
              @click="downloadImage"
              class="block w-full text-left p-2 hover:bg-gray-200"
            >
              Download Image
            </button>
            <button
              @click="handleGoogleDownload"
              class="block w-full text-left p-2 hover:bg-gray-200"
            >
              Upload to Google Drive
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Confirmation Modal for Reset (Overlay on top of content)-->
  <div
    v-if="showResetModal"
    class="fixed inset-0 bg-gray-800 bg-opacity-50 flex items-center justify-center z-50"
  >
    <div class="bg-white p-6 rounded shadow-lg w-120">
      <h3 class="text-xl font-semibold text-gray-900 mb-10">
        Are you sure you want to reset? <br />
        This will discard your photo and all changes.
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
import PhotoEnhancement from "./PhotoEnhancement.vue";

export default {
  components: {
    ImageCropping,
    SidebarWrapper,
    BackgroundRemover,
    ImageResizing,
    PhotoEnhancement,
  },
  data() {
    return {
      imageData: null, // Image data that will be passed to child and updated after crop
      originalImage: null, // Store the original image
      imageHistory: [], // Stack to Store Previous Image States
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
    };
  },
  async mounted() {
    this.imageData = localStorage.getItem("imageData") || null;
    this.originalImage = localStorage.getItem("imageData") || null;
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
    handleAction(action) {
      console.log("Handling action:", action);
      this.currentAction = action;

      if (action === "crop") {
        // If user clicks "Crop" again, allow re-cropping the already cropped image
        this.isCropped = false;
      }

      this.currentAction = action;
    },

    // After crop complete
    handleCropComplete(croppedImage) {
      if (this.imageData) {
        this.imageHistory.push(this.imageData); // Save current state before replacing it
      }
      this.imageData = croppedImage; // Update the imageData with the new cropped image
      this.isCropped = true; // Set the flag to true
    },
    // Reset Original Image
    handleReset() {
      this.imageData = this.originalImage;
      this.isCropped = false; // Reset the cropped state
      console.log("Current resetCounter:", this.resetCounter);
      this.resetCounter += 1; // Increment first
      console.log("resetCounter incremented:", this.resetCounter);
    },

    // After enhancement complete
    handleEnhancementComplete(enhancedImage) {
      if (this.imageData) {
        this.imageHistory.push(this.imageData); // Save current state before replacing it
      }
      this.imageData = enhancedImage; // Update the imageData with the enhanced image
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

    handleUndo() {
      if (this.imageHistory.length > 0) {
        this.imageData = this.imageHistory.pop(); // Go back to previous state
      } else {
        this.imageData = this.originalImage; // If no history, revert to original
      }

      this.isCropped = false; // Reset cropped state
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
        const blob = await this.base64ToBlob(this.imageData, "image/png");

        // File handling
        if (window.showSaveFilePicker) {
          const fileHandle = await window.showSaveFilePicker({
            suggestedName: "edited_image.png",
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
          link.download = "edited_image.png";
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
        }
      } catch (error) {
        console.error("Error downloading the image:", error);
      }
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
      const imageBlob = await this.base64ToBlob(this.imageData, "image/png");

      // Define metadata
      const metadata = {
        name: "uploaded_image.png",
        mimeType: "image/png",
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
