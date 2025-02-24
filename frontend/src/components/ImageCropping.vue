<template>
  <div v-if="imageUrl" class="image-editor-container">
    <h2 class="title">Crop Image</h2>

    <div v-if="!isCropped" class="editor-content">
      <!-- Cropper for Cropping (Hidden after cropping) -->
      <div class="cropper-container">
        <!-- <h3 class="subtitle">Crop Image</h3> -->
        <div class="cropper-wrapper">
          <vue-cropper
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
            @crop="updateInputFields"
          />
        </div>
      </div>

      <!-- Custom Input Fields for Crop -->
      <div class="input-container">
        <label>Country:</label>
        <select v-model="selectedCountry" @change="updateCropBox">
          <option v-for="country in countries" :key="country.name" :value="country">
            {{ country.name }}
          </option>
        </select>

        <label>Width (px):</label>
        <input type="number" v-model="customWidth" @input="updateCropBoxManually" placeholder="Enter width" />
        <label>Height (px):</label>
        <input type="number" v-model="customHeight" @input="updateCropBoxManually" placeholder="Enter height" />

        <button class="btn-primary" @click="cropImage">Crop</button>
      </div>
    </div>

    <!-- Display the cropped image after cropping -->
    <div v-if="isCropped" class="processed-image-container">
      <h3 class="subtitle">Processed Image</h3>
      <img :src="processedImage" alt="Processed Image" class="processed-image" />
      <div class="button-group">
        <button class="btn-secondary" @click="discardCrop">Discard</button>
        <button class="btn-primary" @click="downloadImage">Download</button>
      </div>
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
      processedImage: "", // Cropped image URL
      isCropped: false, // Track if cropping is completed
      selectedCountry: { name: "Default", width: 35, height: 45 },
      countries: [
        { name: "Default", width: 35, height: 45 },
        { name: "USA", width: 40, height: 50 },
        { name: "Europe", width: 35, height: 45 },
      ],
    };
  },
  computed: {
    aspectRatio() {
      return this.customWidth / this.customHeight;
    },
  },
  created() {
    this.imageUrl = this.$route.query.image || "";
  },
  methods: {
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
    updateInputFields(event) {
      const cropper = this.$refs.cropper;
      if (cropper) {
        const cropBoxData = cropper.getCropBoxData();
        this.customWidth = Math.round(cropBoxData.width / this.pxPerMm);
        this.customHeight = Math.round(cropBoxData.height / this.pxPerMm);
      }
    },
    cropImage() {
      const cropper = this.$refs.cropper;
      if (!cropper) return;

      const croppedCanvas = cropper.getCroppedCanvas({
        width: this.customWidth * this.pxPerMm,
        height: this.customHeight * this.pxPerMm,
        imageSmoothingQuality: "high",
      });

      croppedCanvas.toBlob((blob) => {
        if (blob) {
          this.processedImage = URL.createObjectURL(blob);
          this.isCropped = true; // Hide the cropper and input fields after cropping
        }
      }, "image/png");
    },
    discardCrop() {
      // Reset cropping state and show cropper again
      this.isCropped = false;
      this.processedImage = "";
    },
    downloadImage(){

    }
  },
};
</script>

<style>
.editor-content {
  display: flex;
  align-items: center;
  gap: 40px;
  width: 100%;
  justify-content: center;
}

.cropper-container {
  flex: 3;
  text-align: center;
  max-width: 600px;
}

.input-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 250px;
}

.image-editor-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  width: 100%;
}

.title {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
}

.subtitle {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
}

.cropper-container {
  text-align: center;
}

.cropper-wrapper {
  max-width: 100%;
  max-height: 600px;
  overflow: hidden;
}

.input-fields {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 15px 0;
}

.btn-primary, .btn-secondary {
  padding: 10px;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  cursor: pointer;
  width: 100%;
  margin-top: 10px;
}

.btn-primary {
  background-color: #007bff;
  color: white;
}

.btn-primary:hover {
  background-color: #0056b3;
}

.btn-secondary {
  background-color: #dc3545;
  color: white;
}

.btn-secondary:hover {
  background-color: #a71d2a;
}

.processed-image-container {
  text-align: center;
  margin-top: 15px;
}

.processed-image {
  max-width: 90%;
  height: auto;
 
  box-shadow: 0px 4px 6px rgba(0, 0, 0, 0.1);
}
.button-group {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-top: 10px;
}
</style>
