<template> <!--Main Image Edit (Parent Component)-->
  <!-- Sidebar -->
  <SidebarWrapper @update-action="handleAction" :sidebar-width="sidebarWidth" />

  <div class="p-1 grid grid-cols-5 gap-2 ml-[180px] mr-0">
    <!-- Show ImageCropping Component when "Crop" is selected -->
    <ImageCropping 
      v-if="currentAction === 'crop' && imageData && !isCropped"  
      :key="imageData"
      v-model:imageData="imageData"
      @crop-complete="handleCropComplete"
      @discard-crop="handleDiscardCrop" 
    />

    <!-- Show Cropped Image when cropping is done -->
    <div v-if="currentAction === 'crop' && isCropped" class="col-span-4 shadow-lg">
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
      @discard-background="handleDiscardBackground"
    />

    <!-- Show ProcessImage Component when "Process Image" is selected -->
    <ProcessImage 
      v-if="currentAction === 'process-image' && imageData" 
      :key="imageData"
      v-model:imageData="imageData"
      @process-complete="handleProcessComplete"
      @discard-process="handleDiscardProcess"
    />
  
    <div class="bg-black fixed bottom-0 left-0 w-full p-2 z-10">
      <div class="flex justify-end">
        <!--Discard Button -->
        <button :disabled="!isCropped"
          class="text-white bg-gray-800 p-2 rounded mr-3"
          @click="handleDiscardCrop">
          Discard
        </button>

        <!-- Download Button -->
        <button @click="downloadImage" 
          class="text-white bg-gray-800 p-2 rounded">
          Download
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import ImageCropping from "./ImageCropping.vue";
import SidebarWrapper from "./SidebarWrapper.vue";
import BackgroundRemover from "./BackgroundRemover.vue";
import ProcessImage from "./ProcessImage.vue";


export default {
components: {
  ImageCropping,
  SidebarWrapper,
  BackgroundRemover,
  ProcessImage,
},
data() {
  return {
    imageData: null, // Image data that will be passed to child and updated after crop
    originalImage: null, // Store the original image
    currentAction: 'crop', // Start with crop selected
    sidebarWidth: '240px', // Default width for expanded sidebar
    isCropped: false, // Track cropped state in the parent
  };
},
mounted() {
  this.imageData = localStorage.getItem("imageData") || null;
  this.originalImage = localStorage.getItem("imageData") || null;
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
  }
},
methods: {
  handleAction(action) {
    console.log("Handling action:", action);
    this.currentAction = action; 
  },
  // After crop complete
  handleCropComplete(croppedImage) {
    this.imageData = croppedImage; // Update the imageData with the new cropped image
    this.isCropped = true; // Set the flag to true
    
  },
  // Discard Crop
  handleDiscardCrop() {
    this.imageData = this.originalImage;
    this.isCropped = false; // Reset the cropped state
  },

  // Method to toggle sidebar width (collapsed or expanded)
  // toggleSidebar() {
  //   if (this.sidebarWidth === '250px') {
  //     this.sidebarWidth = '65px'; // Collapsed state width
  //   } else {
  //     this.sidebarWidth = '250px'; // Expanded state width
  //   }
  // },

  // After process complete
  handleProcessComplete(processedImage) {
    this.imageData = processedImage;
  },

  // Discard Processing
  handleDiscardProcess() {
    this.imageData = this.originalImage;
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
                  types: [{ description: "Images", accept: { "image/*": [".png", ".jpg", ".jpeg"] } }]
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
},
};
</script>

<style>
.bg-gray-800 {
  background-color: #2d3748 !important;
}
</style>