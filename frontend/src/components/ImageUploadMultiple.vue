<template>
  <div class="upload-container">
    <h1>ID Photo Generator</h1>
    
    <p class="instruction">Upload your photos and preview them before editing.</p>

    <!-- Main Upload Button Options -->
    <div class="button-container">
      <!-- Choose from Device -->
      <div class="button-wrapper">
        <input type="file" accept="image/*" @change="handleLocalFileChange" id="file-upload" multiple ref="fileInput"/>
        <label for="file-upload" class="upload-btn">Choose from Device</label>
      </div>

      <!-- Choose from Cloud -->
      <div class="button-wrapper">
        <button @click="toggleCloudOptions" class="upload-btn">Choose from Cloud</button>
        <!-- Cloud options dropdown -->
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

    <!-- Preview the uploaded images -->
    <div v-if="imageData.length > 0" class="preview-container">
      <h2 class="preview-heading">Image Preview(s):</h2>
      <div class="image-thumbnails">
        <div v-for="(image, index) in imageData" :key="index" class="image-thumbnail">
          <img :src="image" alt="Uploaded Image" class="preview-image" />
          <button class="remove-btn" @click="removeImage(index)">×</button>
        </div>
      </div>

      <!-- Proceed button, placed below the images -->
      <button @click="handleProceed" class="proceed-btn">Continue</button>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      imageData: [],       // Stores the image data URLs for multiple images
      showCloudOptions: false, // Flag to toggle cloud upload options
    };
  },
  methods: {
    // Handle file selection from the device (local upload)
    handleLocalFileChange(event) {
      const files = event.target.files;
      
      if (files.length === 0) return; // No files selected

      // Process each selected file
      Array.from(files).forEach(file => {
        if (file.type.startsWith('image')) {
          const reader = new FileReader();
          reader.onload = (e) => {
            const newImage = e.target.result; // Data URL of the image
            
            // Check if the image already exists in imageData (using data URL comparison)
            if (this.imageData.includes(newImage)) {
              console.log("This image is already uploaded.");
            } else {
              this.imageData.push(newImage); // Add image data to the array
              localStorage.setItem('imageData', JSON.stringify(this.imageData)); // Save to localStorage
            }
          };
          reader.readAsDataURL(file); // Read the image file as a Data URL
        } else {
          alert('Please select valid image files');
        }
      });
      
      // Reset the file input after the image is selected, so the same image can be uploaded again
      this.$refs.fileInput.value = '';
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
      alert('Proceeding to the next step...');
      // Add the logic you want when the user clicks "Continue"
    },

    // Remove the selected image
    removeImage(index) {
      this.imageData.splice(index, 1); // Remove the image at the specified index
      localStorage.setItem('imageData', JSON.stringify(this.imageData)); // Save the updated array to localStorage
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
  gap: 15px; /* Add space between buttons */
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
  min-width: 150px; /* Ensure buttons have a minimum width */
  text-align: center; /* Make sure the text is centered */
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
  min-width: 150px; /* Ensure cloud buttons have a minimum width */
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

.image-thumbnails {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.image-thumbnail {
  width: 100px;
  height: 100px;
  overflow: hidden;
  border-radius: 5px;
  border: 2px solid #ddd;
  position: relative;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  font-size: 20px;
  padding: 1px 1px 5px 1px;
  border-radius: 50%;
  width: 25px;
  height: 25px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1;
}

.remove-btn:hover {
  background-color: rgba(0, 0, 0, 0.8);
}

.remove-btn:focus {
  outline: none;
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
