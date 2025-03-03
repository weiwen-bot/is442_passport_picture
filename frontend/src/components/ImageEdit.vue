<template> <!--Main Image Edit (Parent Component)-->
    <!-- Sidebar -->
    <SidebarWrapper @update-action="handleAction" :sidebar-width="sidebarWidth" />

    <div class="p-1 grid grid-cols-5 gap-2 ml-[180px] mr-0">
      <!-- Show ImageCropping Component when "Crop" is selected -->
      <ImageCropping 
        v-if="currentAction === 'crop' && imageData" 
        :key="imageData"
        v-model:imageData="imageData"
        @crop-complete="handleCropComplete"
        @discard-crop="handleDiscardCrop" 
      />

      <!-- Show BackgroundRemover Component when "Background Remover" is selected -->
      <BackgroundRemover 
        v-if="currentAction === 'background-remover' && imageData" 
        :key="imageData"
        v-model:imageData="imageData"
        @remove-background="handleRemoveBackground"
        @discard-background="handleDiscardBackground"
      />

    
      <div class="bg-black fixed bottom-0 left-0 w-full p-2 z-10">
        <div class="flex justify-end">
          <!--Discard Button -->
          <button :disabled="!isCropped"
            class="text-white p-2 rounded mr-3"
            @click="handleDiscardCrop">
            Discard
          </button>

          <!--Download Button -->
          <button
            class="text-white p-2 rounded">
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


export default {
  components: {
    ImageCropping,
    SidebarWrapper,
    BackgroundRemover,
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
  },
};
</script>
