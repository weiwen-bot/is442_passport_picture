<template>
    <!-- Left side: Country Selection and Custom Width/Height Inputs -->
    <div class="col-span-1 flex flex-col bg-white border rounded-lg shadow-lg p-2 space-y-2 text-black">
      <h2 class="font-bold">Crop Your Image</h2>
      <label for="country" class="font-semibold">Country:</label>
      <select
        id="country"
        v-model="selectedCountry"
        @change="updateCropBox"
        class="p-2 border border-gray-300 rounded-md"
      >
        <option v-for="country in countries" :key="country.name" :value="country">
          {{ country.name }}
        </option>
      </select>

      <label for="aspect" class="font-semibold">Aspect Ratio:</label>
      <select
        id="aspect"
        v-model="selectedAspectRatio"
        @change="updateAspectRatio"
        class="p-2 border border-gray-300 rounded-md"
      >
        <option value="null">Custom</option>
        <option v-for="ratio in aspectRatios" :key="ratio.value" :value="ratio.value">
          {{ ratio.label }}
        </option>
       
      </select>
      <label class="flex items-center space-x-2">
        <input
          type="checkbox"
          v-model="keepAspectRatio"
          @change="toggleAspectRatio"
          class="form-checkbox"
        />
        <span>Keep Aspect Ratio</span>
      </label>


      <label for="width" class="font-semibold">Width (mm):</label>
      <input
        id="width"
        type="number"
        v-model="customWidth"
        @input="updateCropBoxManually"
        placeholder="Enter width"
        step="0.01"
        class="p-2 border border-gray-300 rounded-md"
      />

      <label for="height" class="font-semibold">Height (mm):</label>
      <input
        id="height"
        type="number"
        v-model="customHeight"
        @input="updateCropBoxManually"
        placeholder="Enter height"
        step="0.01"
        class="p-2 border border-gray-300 rounded-md"
      />

      <button
        class="text-white bg-gray-800 p-2 rounded mt-4"
        @click="cropImage">
        Crop
      </button>
  
    </div>

    <!-- Right side: Image Display -->
    <div class="col-span-4 shadow-lg">
      <vue-cropper
        v-if="imageData"
        ref="cropper"
        class="h-full w-auto max-w-full object-contain cropper"
        :src="imageData"
        :aspect-ratio="aspectRatio"
        :view-mode="2"
        :drag-mode="'crop'"
        guides
        :background="false"
        :auto-crop="true"
        :responsive="true"
        :crop-box-resizable="true"
        :crop-box-draggable="true"
        @ready="updateCropBox"
        @crop="updateInputFields"
      />
    </div>
</template>

<script>
import VueCropper from "vue-cropperjs";
import "cropperjs/dist/cropper.css";

export default {
  components: {
    VueCropper,
  },
  props: {
    imageData: String,
  },
  emits: ["update:imageData", "crop-complete"],
  data() {
    return {
      keepAspectRatio: false,
      customWidth: 35, // Custom width in mm (default value)
      customHeight: 45, // Custom height in mm (default value)
      pxPerMm: 10, // Conversion factor from mm to pixels (adjust this value based on your image resolution)
      processedImage: "", // Cropped image URL
      isCropped: false, // Track if cropping is completed
      selectedAspectRatio: null,
      selectedCountry: { name: "Singapore", width: 35, height: 45 },
      countries: [
        { name: "Singapore", width: 35, height: 45 },
        { name: "USA", width: 40, height: 50 },
        { name: "Europe", width: 35, height: 45 },
      ],
      
      aspectRatios: [
        { label: "1:1", value: 1 },
        { label: "4:5", value: 4 / 5 },
        { label: "3:4", value: 3 / 4 },
        { label: "16:9", value: 16 / 9 },
      ],
      isResizing: false, // Track if the crop box is being resized
    };
  },
  computed: {
    aspectRatio() {
      return this.customWidth / this.customHeight;
    },
  },
  mounted() {
    this.selectedCountry = this.countries.find(c => c.name === "Singapore");
    this.customWidth = this.selectedCountry.width;
    this.customHeight = this.selectedCountry.height;
    this.selectedAspectRatio = "null"; // Set to Custom by default

    this.$nextTick(() => {
      if (this.$refs.cropper) {
        this.updateCropBox();
        this.$refs.cropper.setAspectRatio(NaN); // Allow free resizing
      }
    });
  },


  methods: {
    // Backend
    async cropImage() {
      const cropper = this.$refs.cropper;
      if (!cropper) return;

      const cropData = cropper.getData();
      const cropX = Math.round(cropData.x * 100) / 100;
      const cropY = Math.round(cropData.y * 100) / 100;
      const cropWidth = Math.round(cropData.width * 100) / 100;
      const cropHeight = Math.round(cropData.height * 100) / 100;

      console.log("Crop Data:", { cropX, cropY, cropWidth, cropHeight });

      const byteCharacters = atob(this.imageData.split(',')[1]);
      const byteArrays = [];
      for (let offset = 0; offset < byteCharacters.length; offset += 1024) {
        const slice = byteCharacters.slice(offset, offset + 1024);
        const byteNumbers = new Array(slice.length);
        for (let i = 0; i < slice.length; i++) {
          byteNumbers[i] = slice.charCodeAt(i);
        }
        byteArrays.push(new Uint8Array(byteNumbers));
      }

      const imageBlob = new Blob(byteArrays, { type: 'image/jpeg' });

      let formData = new FormData();
      formData.append("image", imageBlob, "cropped_image.jpg");
      formData.append("cropX", cropX);
      formData.append("cropY", cropY);
      formData.append("cropWidth", cropWidth);
      formData.append("cropHeight", cropHeight);

      try {
        const response = await fetch("http://localhost:8080/image/crop", {
          method: "POST",
          body: formData,
          headers: { Accept: "application/json" },
        });

        const responseBody = await response.json();
        if (response.ok) {
          this.$emit("crop-complete", responseBody.image); // Send cropped image back to parent
          this.isCropped = true; // Set the flag to true to display the cropped image
          console.log("isCropped:", this.isCropped); // Should log 'true' after cropping
          
        } else {
          alert("Error cropping image: " + responseBody.message || 'Unknown error');
        }

      } catch (error) {
        alert("Error cropping image:", error);
      }
    },

    updateCropBox() {
      this.customWidth = this.selectedCountry.width;
      this.customHeight = this.selectedCountry.height;
      this.updateCropperBox();
    },
    updateCropBoxManually() {
      if (this.keepAspectRatio) {
        this.customHeight = Math.round((this.customWidth * 45) / 35);
      }

      this.updateCropperBox();
    },


    updateCropperBox() {
      const cropper = this.$refs.cropper;
      if (!cropper) return;

      const imageData = cropper.getImageData(); // Get image size in pixels
      const containerData = cropper.getContainerData(); // Get cropper container size

      const requiredWidthPx = this.selectedCountry.width * this.pxPerMm; // Convert mm to pixels
      const requiredHeightPx = this.selectedCountry.height * this.pxPerMm;

      //Ensure the crop box has the correct aspect ratio
      //cropper.setAspectRatio(requiredWidthPx / requiredHeightPx);

      if (this.keepAspectRatio) {
        cropper.setAspectRatio(35 / 45); // Default to 7:9 aspect ratio
      }

      cropper.setCropBoxData({
        width: requiredWidthPx,
        height: requiredHeightPx,
        left: (containerData.width - requiredWidthPx) / 2, // Center horizontally
        top: (containerData.height - requiredHeightPx) / 2, // Center vertically
      });

      //Ensure the image fits inside the crop box
      const scaleX = requiredWidthPx / imageData.naturalWidth;
      const scaleY = requiredHeightPx / imageData.naturalHeight;
      const scaleFactor = Math.max(scaleX, scaleY);

      cropper.zoomTo(scaleFactor); // Adjust zoom to fit crop box

      // Allow crop box resizing so users can select different countries dynamically
      cropper.setDragMode("crop");
      cropper.setCropBoxResizable(true);
    },

    updateInputFields(event) {
      const cropper = this.$refs.cropper;
      if (cropper) {
        const cropBoxData = cropper.getCropBoxData();
        this.customWidth = Math.round(cropBoxData.width / this.pxPerMm);
        this.customHeight = Math.round(cropBoxData.height / this.pxPerMm);
      }
    },
    updateAspectRatio() {
      const cropper = this.$refs.cropper;
      if (!cropper) return;

      if (this.selectedAspectRatio === "null") {
        // Custom mode: Allow free resizing, but keep the width and height unchanged
        this.keepAspectRatio = false;
        cropper.setAspectRatio(NaN);
      } else {
        // If selecting a predefined aspect ratio, automatically enable "Keep Aspect Ratio"
        this.keepAspectRatio = true;
        cropper.setAspectRatio(this.selectedAspectRatio);
      }

      // Preserve current crop box size
      const cropBoxData = cropper.getCropBoxData();
      this.$nextTick(() => {
        cropper.setCropBoxData({
          left: cropBoxData.left,
          top: cropBoxData.top,
          width: cropBoxData.width,
          height: cropBoxData.height,
        });
      });
    },

    toggleAspectRatio() {
      const cropper = this.$refs.cropper;
      if (!cropper) return;

      // Store current crop box before changing aspect ratio
      const cropBoxData = cropper.getCropBoxData();

      if (this.keepAspectRatio) {
        if (this.selectedAspectRatio === "null") {
          // If "Custom" is selected and "Keep Aspect Ratio" is checked, maintain the current aspect ratio
          const customAspectRatio = this.customWidth / this.customHeight;
          cropper.setAspectRatio(customAspectRatio);

          // Ensure precise height calculation
          this.customHeight = parseFloat((this.customWidth / customAspectRatio).toFixed(2));
        } else {
          // Apply the predefined aspect ratio
          cropper.setAspectRatio(this.selectedAspectRatio);
        }
      } else {
        // When "Keep Aspect Ratio" is unchecked, switch to "Custom"
        this.selectedAspectRatio = "null";
        cropper.setAspectRatio(NaN);
      }

      // Restore previous crop box dimensions
      this.$nextTick(() => {
        cropper.setCropBoxData({
          left: cropBoxData.left,
          top: cropBoxData.top,
          width: cropBoxData.width,
          height: cropBoxData.height,
        });
      });
    }









  },
};
</script>

<style>
.bg-gray-800 {
    background-color: #2d3748 !important;
}
.cropper {
  max-width: 100%;
  height: auto;
  background: transparent;
}
</style>