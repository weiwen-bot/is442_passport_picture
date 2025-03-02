<template>
  <div class="flex items-center justify-center space-x-4">
    <!-- Left side: Country Selection and Custom Width/Height Inputs -->
    <div
      class="left-side bg-white border rounded-lg shadow-lg p-4 flex flex-col justify-start space-y-2 text-black"
    >
      <h2 class="font-bold">Crop Your Image</h2>
      <label for="country" class="font-semibold">Country:</label>
      <select
        id="country"
        v-model="selectedCountry"
        @change="updateCropBox"
        class="p-2 border border-gray-300 rounded-md"
      >
        <option
          v-for="country in countries"
          :key="country.name"
          :value="country"
        >
          {{ country.name }}
        </option>
      </select>

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

      <button class="text-white p-2 rounded mt-4" @click="cropImage">
        Crop
      </button>
    </div>

    <!-- Right side: Image Display -->
    <div class="right-side bg-white border rounded-lg shadow-lg p-4">
      <div class="image-container flex justify-center items-center">
        <vue-cropper
          v-if="imageData"
          ref="cropper"
          :src="imageData"
          :aspect-ratio="aspectRatio"
          :view-mode="2"
          :drag-mode="'crop'"
          guides
          :background="true"
          :auto-crop="true"
          :responsive="true"
          :auto-crop-area="0.8"
          :crop-box-resizable="true"
          :crop-box-draggable="true"
          @crop="updateInputFields"
        />
      </div>
    </div>

    <!-- Button to crop the image -->
    <button @click="cropImage">Crop</button>

    <!-- Display the cropped image -->
    <div v-if="processedImage" class="processed-image-container">
      <h3>Processed Image</h3>
      <img
        :src="processedImage"
        alt="Processed Image"
        class="processed-image"
      />
    </div>
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
      customWidth: 35, // Custom width in mm (default value)
      customHeight: 45, // Custom height in mm (default value)
      pxPerMm: 10, // Conversion factor from mm to pixels (adjust this value based on your image resolution)
      processedImage: "", // Cropped image URL
      isCropped: false, // Track if cropping is completed
      selectedCountry: { name: "Singapore", width: 35, height: 45 },
      countries: [
        { name: "Singapore", width: 35, height: 45 },
        { name: "USA", width: 40, height: 50 },
        { name: "Europe", width: 35, height: 45 },
      ],
      isResizing: false, // Track if the crop box is being resized
    };
  },
  computed: {
    aspectRatio() {
      return this.customWidth / this.customHeight;
    },
  },
  created() {
    // Retrieve the image passed from ImageUpload.vue via query params
    this.imageUrl = this.$route.query.image || "";
    console.log("Image URL for cropping:", this.imageUrl); // Debugging log

    // Once the image is loaded, update the cropper dimensions
    const img = new Image();
    img.src = this.imageUrl;
    img.onload = () => {
      this.cropperWidth = img.width; // Set the width of the cropper container in pixels
      this.cropperHeight = img.height; // Set the height of the cropper container in pixels

      // Convert the image dimensions from pixels to millimeters (optional)
      this.customWidth = img.width / this.pxPerMm; // Convert pixel width to millimeters
      this.customHeight = img.height / this.pxPerMm; // Convert pixel height to millimeters

      console.log(
        `Image Dimensions in mm: ${this.customWidth} mm x ${this.customHeight} mm`
      );

      // After setting the custom dimensions, update the cropper box
      this.updateCropBox();
    };
  },
  mounted() {
    // Ensure cropper is initialized with custom width/height
    this.$nextTick(() => {
      this.updateCropBox(); // Manually set crop box size after image is loaded
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

      const byteCharacters = atob(this.imageData.split(",")[1]);
      const byteArrays = [];
      for (let offset = 0; offset < byteCharacters.length; offset += 1024) {
        const slice = byteCharacters.slice(offset, offset + 1024);
        const byteNumbers = new Array(slice.length);
        for (let i = 0; i < slice.length; i++) {
          byteNumbers[i] = slice.charCodeAt(i);
        }
        byteArrays.push(new Uint8Array(byteNumbers));
      }

      const imageBlob = new Blob(byteArrays, { type: "image/jpeg" });

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
          alert(
            "Error cropping image: " + responseBody.message || "Unknown error"
          );
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
      this.updateCropperBox();
    },
    updateCropperBox() {
      const cropper = this.$refs.cropper;
      if (cropper) {
        const defaultWidthInPx = this.customWidth * this.pxPerMm;
        const defaultHeightInPx = this.customHeight * this.pxPerMm;

        cropper.setAspectRatio(this.aspectRatio);
        cropper.setCropBoxData({
          width: defaultWidthInPx,
          height: defaultHeightInPx,
        });
      }
    },
    onCropStart() {
      this.isResizing = true; // Flag for resizing start
    },
    onCropMove() {
      if (this.isResizing) {
        const cropper = this.$refs.cropper;
        if (cropper) {
          const cropBoxData = cropper.getCropBoxData();
          // Only update dimensions during resizing, not moving
          if (
            this.cropBoxData &&
            (cropBoxData.width !== this.cropBoxData.width ||
              cropBoxData.height !== this.cropBoxData.height)
          ) {
            this.customWidth = (cropBoxData.width / this.pxPerMm).toFixed(2);
            this.customHeight = (cropBoxData.height / this.pxPerMm).toFixed(2);
          }
          this.cropBoxData = cropBoxData; // Save the latest crop box data
        }
      }
    },
    onCropEnd() {
      this.isResizing = false; // Reset resizing flag
      const cropper = this.$refs.cropper;
      if (cropper) {
        const cropBoxData = cropper.getCropBoxData();
        this.customWidth = Math.round(cropBoxData.width / this.pxPerMm);
        this.customHeight = Math.round(cropBoxData.height / this.pxPerMm);
      }
    },
  },
};
</script>

<style>
.crop-container {
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: auto;
}

.processed-image-container {
  width: 100%;
  text-align: center;
  margin-top: 20px;
}

.processed-image {
  width: auto;
  height: auto;
  object-fit: contain;
}
</style>
