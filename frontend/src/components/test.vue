<template>
    <div class="image-preview">
      <h2>Original Image</h2>
      <img v-if="originalImage" :src="originalImage" alt="Original" class="image" />
  
      <h2>Processed (Background Removed)</h2>
      <img v-if="processedImage" :src="processedImage" alt="Processed" class="image" />
  
      <div v-if="errorMessage" class="error">{{ errorMessage }}</div>
  
      <button @click="processImage" :disabled="isProcessing">
        {{ isProcessing ? "Processing..." : "Remove Background" }}
      </button>
    </div>
  </template>
  
  <script>
  export default {
    data() {
      return {
        originalImage: null,    // Holds the original base64 image (e.g., data:image/png;base64,...)
        processedImage: null,   // Holds the final result after background removal
        isProcessing: false,
        errorMessage: ""
      };
    },
    mounted() {
      // Example: load from localStorage or set from a parent component
      this.originalImage = localStorage.getItem("imageData") || null;
    },
    methods: {
      /**
       * Main method: (1) pad image => 1024x1024, (2) send to backend, (3) crop result.
       */
      async processImage() {
        if (!this.originalImage) {
          this.errorMessage = "No image available for processing.";
          return;
        }
  
        this.isProcessing = true;
        this.errorMessage = "";
  
        try {
          // 1) Load the image to get original dimensions
          const img = new Image();
          img.src = this.originalImage;
          await new Promise((resolve, reject) => {
            img.onload = resolve;
            img.onerror = () => reject(new Error("Failed to load image"));
          });
  
          const originalWidth = img.width;
          const originalHeight = img.height;
          console.log("Original dimensions:", originalWidth, "x", originalHeight);

  
          // Send to your backend
          const payload = { image: this.originalImage }; // data:image/png;base64,...
            const response = await fetch("http://localhost:8080/image2/process", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
            });

  
          if (!response.ok) {
            throw new Error("Processing failed");
          }
  
          const result = await response.json();
          if (!result.processedImage) {
            throw new Error("No processed image received from backend.");
          }
  
          // 3) Crop the returned image back to the original size
          this.processedImage = result.processedImage;
  
          // Optionally store or emit the final image
          localStorage.setItem("imageData", this.processedImage);
          // this.$emit("update:imageData", this.processedImage);
  
        } catch (error) {
          console.error("Error processing image:", error);
          this.errorMessage = "Error processing image.";
        } finally {
          this.isProcessing = false;
        }
      },
  
    }
  };
  </script>
  
  <style scoped>
  .image-preview {
    text-align: center;
    margin: 20px;
  }
  .image {
    max-width: 100%;
    height: auto;
    margin-bottom: 20px;
  }
  .error {
    color: red;
    margin-top: 10px;
  }
  button {
    padding: 10px 15px;
    font-size: 16px;
    cursor: pointer;
  }
  </style>
  