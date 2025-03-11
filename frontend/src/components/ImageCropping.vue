<template>
  <!-- Left side: Country Selection and Custom Width/Height Inputs -->
  <div class="col-span-4 flex flex-col bg-white border rounded-lg shadow-lg p-2 space-y-2 text-black">
    <h2 class="font-bold">Crop Your Image</h2>

    <label for="width" class="font-semibold">Width (mm):</label>
    <input
      id="width"
      type="number"
      v-model="customWidth"
      @input="updateCropBoxManually"
      @change="updateCropBoxManually"
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
      @change="updateCropBoxManually"
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
  <div class="col-span-8 shadow-lg">
    <vue-cropper
      v-if="imageData"
      ref="cropper"
      class="h-full w-auto max-w-full object-contain cropper"
      :src="imageData"
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
    customWidth: 35, // Custom width in mm (default value)
    customHeight: 45, // Custom height in mm (default value)
    pxPerMm: 10, // Conversion factor from mm to pixels (adjust this value based on your image resolution)
    processedImage: "", // Cropped image URL
    isCropped: false, // Track if cropping is completed    
    isResizing: false, // Track if the crop box is being resized
  };
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
    if (!this.isResizing) { 
      this.customWidth = 35;
      this.customHeight = 45;
    }
    this.updateCropperBox();
  },

  updateCropBoxManually() {
    const cropper = this.$refs.cropper;
    if (!cropper) return;

    const requiredWidthPx = this.customWidth * this.pxPerMm;
    const requiredHeightPx = this.customHeight * this.pxPerMm;
    cropper.setCropBoxData({
      width: requiredWidthPx,
      height: requiredHeightPx,
    });
  },


  updateCropperBox() {
    const cropper = this.$refs.cropper;
    if (!cropper) return;

    const imageData = cropper.getImageData(); // Get image size in pixels
    const containerData = cropper.getContainerData(); // Get cropper container size

    const requiredWidthPx = 35 * this.pxPerMm; // Convert mm to pixels
    const requiredHeightPx = 45 * this.pxPerMm;



    cropper.setCropBoxData({
      width: requiredWidthPx,
      height: requiredHeightPx,
      left: (containerData.width - requiredWidthPx) / 2, // Center horizontally
      top: (containerData.height - requiredHeightPx) / 2, // Center vertically
    });

    // 🔹 Ensure the image fits inside the crop box
    const scaleX = requiredWidthPx / imageData.naturalWidth;
    const scaleY = requiredHeightPx / imageData.naturalHeight;
    const scaleFactor = Math.max(scaleX, scaleY);

    cropper.zoomTo(scaleFactor); // Adjust zoom to fit crop box

    // ✅ Allow crop box resizing so users can select different countries dynamically
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