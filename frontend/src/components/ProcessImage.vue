<template>
  <div class="flex h-[85vh] w-[calc(100vw-240px)] mx-auto">
    <!-- Left Side: Original Image -->
    <div class="flex flex-col w-1/2 h-full justify-center items-center border-r border-gray-300">
      <h3 class="text-lg font-semibold mb-4">Original Image</h3>
      <div v-if="originalImage" class="w-4/5 max-h-[75%] flex justify-center items-center">
        <img :src="originalImage" alt="Original" class="max-w-full max-h-full rounded-lg shadow-md border border-gray-400" />
      </div>
    </div>

    <!-- Right Side: Processed Image -->
    <div class="flex flex-col w-1/2 h-full justify-center items-center">
      <h3 class="text-lg font-semibold mb-4">Processed Image</h3>
      <div v-if="processedImage" class="w-4/5 max-h-[75%] flex justify-center items-center">
        <img :src="processedImage" alt="Processed" class="max-w-full max-h-full rounded-lg shadow-md border border-gray-400" />
      </div>
      <div v-else class="text-gray-500 text-sm">No processed image yet</div>

      <!-- Show loading indicator -->
      <div v-if="isProcessing" class="mt-4 text-gray-600 text-sm animate-pulse">Processing image, please wait...</div>

      <!-- Error Message -->
      <div v-if="errorMessage" class="mt-4 text-red-500 text-sm">{{ errorMessage }}</div>

      <!-- Process Button -->
      <button v-if="originalImage && !processedImage" @click="processImage"
        class="mt-6 px-5 py-2 bg-blue-500 hover:bg-blue-600 text-black font-semibold rounded shadow-md transition">
        Process Image
      </button>
      <!-- <UploadButton @input="updateImage" ></UploadButton> -->
      <label for="favcolor">Select your favorite color:</label>
      <input type="color" id="favcolor" name="favcolor" v-model="color">
      <ImagePreview :image="backgroundImage" />
      <UploadImageButton @image-uploaded="updateImage" />
    </div>
  </div>
</template>

<script>
import UploadImageButton from "./UploadImageButton.vue";
import ImagePreview from "./ImagePreview.vue"
export default {
  props: {
    imageData: String, // Receive imageData as a prop from ImageEdit.vue
  },
  components:{
  UploadImageButton,
  ImagePreview,
  },
  data() {
    return {
      originalImage: null, // Base64 image from parent/localStorage
      processedImage: null, // Processed image from backend
      isProcessing: false,
      errorMessage: "", // Error message handling
      uploadImage: null,
      backgroundImage: null,
      color: "#FFFFFF",
      payload :{}
    };
  },
  created() {
    // Load image from props (from ImageEdit.vue) or localStorage
    this.originalImage = this.imageData || localStorage.getItem("imageData");
  },
  methods: {
    

    updateImage(image){
      
      this.backgroundImage = image;
    },
    async processImage() {
      if (!this.originalImage) {
        this.errorMessage = "No image available for processing.";
        return;
      }

      this.isProcessing = true;
      this.errorMessage = "";

      try {
        // Crop + scale to multiple-of-32 dimension
        // const finalBase64 = await this.cropToSquareMultipleOf32(this.originalImage);
        const finalBase64 = await this.resizeToClosestMultipleOf32(this.originalImage);

        console.log(this.backgroundImage,"HEELLO")
        const category = "";
        if (this.backgroundImage == null){
          this.payload = { image: finalBase64, category: "color", colorString: this.color };
        } else {
          this.payload = { image: finalBase64, category: "background", backgroundString: this.backgroundImage };
        }

        

        const response = await fetch("http://localhost:8080/bg/removebg", {
          // const response = await fetch("http://localhost:8080/image/process", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(this.payload),
        });

        if (!response.ok) throw new Error("Processing failed");

        const result = await response.json();
        if (result.processedImage) {
          this.processedImage = result.processedImage; 
        } else {
          throw new Error("No processed image received from backend.");
        }
      } catch (error) {
        console.error("Error processing image:", error);
        this.errorMessage = "Error processing image.";
      } finally {
        this.isProcessing = false;
      }
    },

    async resizeToClosestMultipleOf32(base64) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.src = base64;
      img.onload = () => {
        const originalWidth = img.width;
        const originalHeight = img.height;

        // 1) Round each dimension to the nearest multiple of 32
        const newWidth = this.roundToNearestMultiple(originalWidth, 32);
        const newHeight = this.roundToNearestMultiple(originalHeight, 32);

        // 2) Create a canvas at the new size
        const canvas = document.createElement("canvas");
        canvas.width = newWidth;
        canvas.height = newHeight;
        const ctx = canvas.getContext("2d");

        // 3) Draw the entire original image scaled to the new dimension
        ctx.drawImage(img, 0, 0, newWidth, newHeight);

        // 4) Convert to base64 and return
        const resizedBase64 = canvas.toDataURL("image/png");
        resolve(resizedBase64);
      };
      img.onerror = () => reject(new Error("Failed to load image"));
    });
  },

  // Helper to round a number to the nearest multiple of 'm'
  roundToNearestMultiple(value, m) {
    // e.g. if value=185, remainder=185 % 32=25, down=160, up=192 -> 185 is closer to 192 => returns 192
    const remainder = value % m;
    const down = value - remainder; // e.g. 185 - 25 = 160
    const up = down + m;           // e.g. 160 + 32 = 192

    // Decide which is closer to 'value'
    if (remainder <= m / 2) {
      return down;   // e.g. if remainder < 16, we pick 160
    } else {
      return up;     // e.g. remainder >=16 => we pick 192
    }
  },


  },
};
</script>
