<template>
<div class="flex flex-col items-center">
    <!-- Hidden file input -->
    <input 
      ref="fileInput" 
      type="file" 
      accept="image/*" 
      @change="handleLocalFileChange" 
      class="hidden"
    />

    <!-- Button to trigger file selection -->
    <button 
      @click="triggerFileInput" 
      class="px-4 py-2 bg-blue-500 text-black rounded-lg cursor-pointer hover:bg-blue-600 transition"
    >
      Upload Image
    </button>
  </div>
</template>

<script>
export default {
  name: "UploadImageButton",
  // props: {
  //   className: {
  //     type: String,
  //     default: "",
  //   },
  // },
  methods: {
    triggerFileInput() {
      this.$refs.fileInput.click(); // Manually triggers the file input
    },

    async uploadToBackend(file) {
      const formData = new FormData();
      formData.append("image", file);

      try {
        const response = await fetch("http://localhost:8080/image/upload", {
          method: "POST",
          body: formData,
          headers: { Accept: "application/json" },
        });

        if (!response.ok) throw new Error("Upload failed");

        const result = await response.json();
        console.log("Upload Success:", result);
        
        // Update imageData with processed image from backend
        if (result.image) {
          this.imageData = result.image; // Set the processed image
        } else {
          console.error("No image received from backend");
        }

        return result; // Return response if needed
      } catch (error) {
        console.error("Error uploading image:", error);
      }
    },

    async handleLocalFileChange(event){
      console.log("testing")
      const file = event.target.files[0];
      if (!file) return;
      if (file && file.type.startsWith("image")){
        const reader = new FileReader();
        reader.onload = () => {
          this.$emit('image-uploaded', reader.result);

        };
        reader.readAsDataURL(file);
        await this.uploadToBackend(file);
      } else {
        alert("Please select a valid image file");
      }
    }

    

  },
};
</script>
