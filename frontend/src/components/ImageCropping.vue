<template>
  <div v-if="imageUrl">
    <h2>Crop Image</h2>
    <div class="crop-container" :style="cropperStyles">
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
        :auto-crop="false"
        :responsive="true"
        :auto-crop-area="0.8"
        :crop-box-resizable="true"
        :crop-box-draggable="true"
        @cropmove="onCropMove"
      />
    </div>

    <!-- Inputs for custom width and height -->
    <!-- Show width and height input only after image is uploaded -->
    <div v-if="imageUrl">
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
    };
  },
  computed: {
    aspectRatio() {
      return this.customWidth / this.customHeight;
    },
    cropperStyles() {
      return {
        width: this.cropperWidth + "px", // Use dynamically set cropper width
        height: this.cropperHeight + "px", // Use dynamically set cropper height
        maxWidth: "100%",   // Ensure it remains responsive
        maxHeight: "100%",  // Prevent overflow issues
      };
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
  methods: {
    updateCropBox() {
      if (this.$refs.cropper) {
        // Get the aspect ratio of the image
        const aspectRatio = this.cropperWidth / this.cropperHeight;

        // Set the crop box to the full width and height of the image, with aspect ratio handling
        this.$refs.cropper.setAspectRatio(aspectRatio);  // Enforce the aspect ratio

        // Now set the crop box data to cover the entire image (you can adjust the size if necessary)
        this.$refs.cropper.setCropBoxData({
          left: 0,
          top: 0,
          width: this.cropperWidth,
          height: this.cropperHeight,
        });
      }
    },
    onCropMove() {
      const cropper = this.$refs.cropper;
      
      if (cropper) {
        const cropBoxData = cropper.getCropBoxData();
        console.log(cropBoxData);
        
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
