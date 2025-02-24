<template>
  <div class="upload-container">
    <h1>ID Photo Generator</h1>
    
    <p class="instruction">Upload your photo and preview it before editing.</p>

    <!-- Main Upload Button Options -->
    <div class="button-container">
      <!-- Choose from Device -->
      <div class="button-wrapper">
        <input type="file" accept="image/*" @change="handleLocalFileChange" id="file-upload" />
        <label for="file-upload" class="upload-btn">Choose from Device</label>
      </div>

      <!-- Choose from Cloud -->
      <div v-if="false" class="button-wrapper">
        <button @click="toggleCloudOptions" class="upload-btn">Choose from Cloud</button>
        <div v-if="showCloudOptions" class="cloud-options">
          <button @click="handleGoogleDriveUpload" class="cloud-btn">
            <img src="https://upload.wikimedia.org/wikipedia/commons/4/4c/Google_Drive_icon.svg" alt="Google Drive" class="cloud-icon" />
            Google Drive
          </button>
          <button @click="handleDropboxUpload" class="cloud-btn">
            <img src="https://upload.wikimedia.org/wikipedia/commons/3/3b/Dropbox_logo_2015.svg" alt="Dropbox" class="cloud-icon" />
            Dropbox
          </button>
          <button @click="handleOneDriveUpload" class="cloud-btn">
            <img src="https://upload.wikimedia.org/wikipedia/commons/5/53/OneDrive_logo_2013.svg" alt="OneDrive" class="cloud-icon" />
            OneDrive
          </button>
        </div>
      </div>
    </div>

    <!-- Preview the uploaded image -->
    <div v-if="imageData" class="preview-container">
      <h2 class="preview-heading">Image Preview:</h2>
      <img :src="imageData" alt="Uploaded Image" class="preview-image" />
      
      <!-- Proceed button, placed below the image -->
      <button @click="handleProceed" class="proceed-btn">Continue</button>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      imageData: null,       // Stores the image data URL
      showCloudOptions: false, // Flag to toggle cloud upload options
    };
  },
  methods: {
    // Handle file selection from the device (local upload)
    handleLocalFileChange(event) {
      const file = event.target.files[0];
      
      if (!file) return; // No file selected
      if (file && file.type.startsWith('image')) {
        const reader = new FileReader();
        reader.onload = (e) => {
          this.imageData = e.target.result; // Store image data URL
          localStorage.setItem('imageData', this.imageData); // Save to localStorage
        };
        reader.readAsDataURL(file); // Read the image file as a Data URL
      } else {
        alert('Please select a valid image file');
      }
    },

    // Toggle the visibility of cloud options
    toggleCloudOptions() {
      this.showCloudOptions = !this.showCloudOptions;
    },

    // Placeholder for Google Drive upload functionality
    handleGoogleDriveUpload() {
      alert('Google Drive upload functionality is not implemented yet.');
    },

    // Placeholder for Dropbox upload functionality
    handleDropboxUpload() {
      alert('Dropbox upload functionality is not implemented yet.');
    },

    // Placeholder for OneDrive upload functionality
    handleOneDriveUpload() {
      alert('OneDrive upload functionality is not implemented yet.');
    },

    // Handle the "Continue" button click
    handleProceed() {
      // alert('Proceeding to the next step...');
      // Add the logic you want when the user clicks "Continue"
      // For example, navigating to the next page or form
      if (this.imageData) {
        // Navigate to the image cropping page, passing the image URL as a query parameter
        this.$router.push({ name: 'ImageEdit', query: { image: this.imageData } });
      } else {
        alert('Please upload an image first.');
      }
    },
  },
};
</script>

<style scoped>
.upload-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background-color: #f0f0f0;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  max-width: 500px;
  margin: auto;
  text-align: center;
}

h1 {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #333;
}

.instruction {
  color: #777;
  margin-bottom: 20px;
}

.button-container {
  display: flex;
  justify-content: space-evenly;
  align-items: center;
  width: 100%;
  margin-bottom: 20px;
}

.button-wrapper {
  display: flex;
  align-items: center;
  position: relative;
}

#file-upload {
  display: none;
}

.upload-btn {
  padding: 12px 24px;
  background-color: #007bff;
  color: white;
  font-size: 16px;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.upload-btn:hover {
  background-color: #0056b3;
}

.cloud-options {
  display: flex;
  flex-direction: column;
  margin-top: 10px;
  gap: 10px;
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  z-index: 1000;
  background-color: #fff;
  border-radius: 5px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.cloud-btn {
  padding: 12px 24px;
  background-color: #34a853;
  color: white;
  font-size: 16px;
  border-radius: 5px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: background-color 0.3s;
  border: none;
}

.cloud-btn:hover {
  background-color: #2c8d42;
}

.cloud-icon {
  width: 20px;
  height: 20px;
  margin-right: 10px;
}

.preview-container {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.preview-heading {
  color: black;
  font-size: 18px;
  margin-bottom: 10px;
}

.preview-image {
  max-width: 50%;
  height: auto;
  margin-top: 10px;
  border-radius: 5px;
  border: 2px solid #ddd;
}

.proceed-btn {
  padding: 12px 24px;
  background-color: #28a745;
  color: white;
  font-size: 16px;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.3s;
  margin-top: 20px;
}

.proceed-btn:hover {
  background-color: #218838;
}
</style>
