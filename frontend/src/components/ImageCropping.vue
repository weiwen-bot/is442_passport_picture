<template>
  <div v-if="imageUrl">
    <h2>Crop Image</h2>
    <div class="crop-container">
      <!-- VueCropper initialized with the correct image URL -->
      <vue-cropper
        v-if="imageUrl"
        ref="cropper"
        :src="imageUrl"
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
        @cropstart="onCropStart"
        @cropmove="onCropMove"
        @cropend="onCropEnd"
      />
    </div>

    <!-- Inputs for custom width and height -->
    <!-- Show width and height input only after image is uploaded -->
    <div v-if="imageUrl">

      <!--Select Country-->
      <label>Country:</label>
      <select v-model="selectedCountry" @change="updateCropBox">
        <option v-for="country in countries" :key="country.name" :value="country">
          {{ country.name }}
        </option>
      </select>

      <label>Custom Width (mm):</label>
      <input type="number" v-model.number="customWidth" @input="updateCropBox" />

      <label>Custom Height (mm):</label>
      <input type="number" v-model.number="customHeight" @input="updateCropBox" />
    </div>

    <!-- Button to crop the image -->
    <button @click="cropImage">Crop</button>

    <!-- Display the cropped image -->
    <div v-if="processedImage" class="processed-image-container">
      <h3>Processed Image</h3>
      <img :src="processedImage" alt="Processed Image" class="processed-image" />
    </div>
  </div>
</template>

<script>
import VueCropper from "vue-cropperjs";
import "cropperjs/dist/cropper.css";

export default {
  components: { VueCropper },
  data() {
    return {
      imageUrl: "", // Will hold the Base64 image URL
      customWidth: 35, // Custom width in mm
      customHeight: 45, // Custom height in mm
      pxPerMm: 10, // Conversion factor from mm to pixels
      imageSrc: "", // Holds the cropped image URL
      cropperWidth: 0, // Dynamic width for the cropper container
      cropperHeight: 0, // Dynamic height for the cropper container
      processedWidth: 0, // Width of the processed image
      processedHeight: 0, // Height of the processed image
      processedImage: "", // Define processedImage to store the cropped image URL
      maxDisplayWidth: 800, // Maximum width for the display image
      maxDisplayHeight: 600, // Maximum height for the display image
      selectedCountry: { name: "Default", width: 35, height: 45 },
      countries: [
        { name: "Default", width: 35, height: 45 },
        { name: "USA", width: 40, height: 50 },
        { name: "Europe", width: 35, height: 45 },
      ],
      isResizing: false, // To track if resizing
      cropBoxData: null, // Store crop box data (initial dimensions)
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
      this.customWidth = img.width / this.pxPerMm;  // Convert pixel width to millimeters
      this.customHeight = img.height / this.pxPerMm; // Convert pixel height to millimeters

      console.log(`Image Dimensions in mm: ${this.customWidth} mm x ${this.customHeight} mm`);

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
    updateCropBox() {
      // Update the custom width/height based on selected country
      this.customWidth = this.selectedCountry.width;
      this.customHeight = this.selectedCountry.height;

      // Ensure crop box is updated when country selection changes
      const cropper = this.$refs.cropper;
      if (cropper) {
        const defaultWidthInPx = this.customWidth * this.pxPerMm;
        const defaultHeightInPx = this.customHeight * this.pxPerMm;

        cropper.setAspectRatio(this.aspectRatio);

        // Manually set crop box data (position and dimensions)
        cropper.setCropBoxData({
          left: (cropper.cropper.width - defaultWidthInPx) / 2,
          top: (cropper.cropper.height - defaultHeightInPx) / 2,
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
          if (this.cropBoxData && (
            cropBoxData.width !== this.cropBoxData.width ||
            cropBoxData.height !== this.cropBoxData.height
          )) {
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
        this.customWidth = (cropBoxData.width / this.pxPerMm).toFixed(2);
        this.customHeight = (cropBoxData.height / this.pxPerMm).toFixed(2);
      }
    },
    cropImage() {
      const cropper = this.$refs.cropper;
      if (!cropper) return;

      // Get cropped canvas with the current crop box size
      const croppedCanvas = cropper.getCroppedCanvas({
        width: this.customWidth * this.pxPerMm, // Maintain the current crop box width
        height: this.customHeight * this.pxPerMm, // Maintain the current crop box height
        imageSmoothingQuality: "high",
      });

      // Convert canvas to a blob and upload the cropped image
      croppedCanvas.toBlob((blob) => {
        if (blob) {
          const croppedImageUrl = URL.createObjectURL(blob); // Create URL from Blob
          this.processedImage = croppedImageUrl; // Store the cropped image URL

          // Resize the processed image to fit within the max display size
          const ratio = Math.min(
            this.maxDisplayWidth / croppedCanvas.width,
            this.maxDisplayHeight / croppedCanvas.height
          );
          this.processedWidth = croppedCanvas.width * ratio; // Scale the width
          this.processedHeight = croppedCanvas.height * ratio; // Scale the height

          console.log("Cropped Image Blob:", croppedImageUrl); // Debugging log
        }
      }, "image/png");
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
