<template>
    <div>
      <h2>Upload Image</h2>
      <input type="file" @change="onFileChange" accept="image/*" />
      <button v-if="selectedImage" @click="uploadImage">Upload</button>
      
      <div v-if="imagePreview">
        <h3>Preview</h3>
        <img :src="imagePreview" alt="Uploaded Image" width="300" />
        <button @click="goToCropPage">Crop Image</button>
      </div>
      
    </div>
  </template>
  
  <script>
  export default {
    data() {
      return {
        selectedImage: null,
        imagePreview: null,
      };
    },
    methods: {
      onFileChange(event) {
        const file = event.target.files[0];
        if (file) {
          this.selectedImage = file;
        }
      },
  
      async uploadImage() {
        const formData = new FormData();
        formData.append("image", this.selectedImage);
  
        try {
          const response = await fetch("http://localhost:8080/image/upload", {
            method: "POST",
            body: formData,
            headers: { Accept: "application/json" },
          });
  
          if (!response.ok) throw new Error("Upload failed");
  
          const result = await response.json();
          this.imagePreview = result.image; // Base64 image
  
        } catch (error) {
          console.error("Error uploading image:", error);
        }
      },
  
      goToCropPage() {
        this.$router.push({ name: "ImageCropping", query: { image: this.imagePreview } });
      },
    },
  };
  </script>
  