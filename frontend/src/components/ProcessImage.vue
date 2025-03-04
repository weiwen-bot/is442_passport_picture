<template>
  <div class="process-container">
    <h2>Testing Background Removal</h2>

    <!-- File Upload -->
    <input type="file" @change="handleImageUpload" accept="image/*" />

    <!-- Show original image -->
    <div v-if="originalImage" class="image-preview">
      <h3>Original Image</h3>
      <img :src="originalImage" alt="Original" class="preview-img" />
    </div>

    <!-- Show processed image when available -->
    <div v-if="processedImage" class="image-preview">
      <h3>Processed Image</h3>
      <img :src="processedImage" alt="Processed" class="preview-img" />
    </div>

    <!-- Show loading indicator while processing -->
    <div v-if="isProcessing" class="loading">
      <p>Processing image, please wait...</p>
    </div>

    <!-- Error Message -->
    <div v-if="errorMessage" class="error">
      <p>{{ errorMessage }}</p>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      originalImage: null, // Base64 image from localStorage
      processedImage: null, // Processed image from backend
      isProcessing: false,
      errorMessage: "", // Error message handling
    };
  },
  created() {
    // Load stored image from localStorage
    const storedImage = localStorage.getItem("imageData");
    if (storedImage) {
      this.originalImage = storedImage;
      this.processImage();
    }
  },
  methods: {
    async handleImageUpload(event) {
      const file = event.target.files[0];
      if (!file) return;

      try {
        // Resize and store the image
        const resizedBase64 = await this.resizeImage(file, 512, 512);
        this.originalImage = resizedBase64;
        localStorage.setItem("imageData", resizedBase64);

        // Process the resized image
        this.processImage();
      } catch (error) {
        console.error("Error resizing image:", error);
        this.errorMessage = "Failed to resize image.";
      }
    },

    async processImage() {
      this.isProcessing = true;
      this.errorMessage = "";
      try {
        const payload = { image: this.originalImage };

        const response = await fetch("http://localhost:8080/image/process", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });

        if (!response.ok) throw new Error("Processing failed");

        const result = await response.json();
        if (result.processedImage) {
          this.processedImage = result.processedImage; // Base64 processed image
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

    async resizeImage(file, width, height) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = (event) => {
          const img = new Image();
          img.src = event.target.result;
          img.onload = () => {
            const canvas = document.createElement("canvas");
            canvas.width = width;
            canvas.height = height;
            const ctx = canvas.getContext("2d");
            ctx.drawImage(img, 0, 0, width, height);
            resolve(canvas.toDataURL("image/png"));
          };
          img.onerror = () => reject(new Error("Failed to load image"));
        };
        reader.onerror = () => reject(new Error("Failed to read file"));
      });
    },
  },
};
</script>

<style scoped>
.process-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background: white;
  border-radius: 10px;
  box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.1);
}

.image-preview {
  margin-top: 20px;
  text-align: center;
}

.preview-img {
  max-width: 80%;
  border-radius: 5px;
  border: 2px solid #ddd;
}

.loading {
  margin-top: 10px;
  font-size: 14px;
  color: gray;
}

.error {
  color: red;
  margin-top: 10px;
  font-size: 14px;
}
</style>
