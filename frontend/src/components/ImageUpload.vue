<template>
  <div class="upload-container"
    @dragover.prevent="handleDragOver" 
    @dragenter.prevent="handleDragOver"
    @dragleave="handleDragLeave"
    @drop="handleDrop"
  >
  
    <h1>ID Photo Generator</h1>

    <p class="instruction">
      Upload your photo and preview it before editing. Maximum file size allowed
      is 10MB.
    </p>

    <!-- Main Upload Button Options -->
    <div class="button-container">
      <!-- Choose from Device -->
      <div class="button-wrapper">
        <input
          type="file"
          accept="image/*"
          @change="handleLocalFileChange"
          id="file-upload"
        />
        <label for="file-upload" class="upload-btn">Choose from Device</label>
      </div>

      <!-- Choose from Cloud -->
      <div class="button-wrapper">
        <button @click="handleGoogleDriveUpload" class="upload-btn">
          <img
            src="https://upload.wikimedia.org/wikipedia/commons/d/da/Google_Drive_logo.png"
            alt="Google Drive"
            class="cloud-icon"
          />
          Google Drive
        </button>
      </div>
      
      
    </div>

    <div class="button-container">
      <div class="button-wrapper">
        <button @click="navigateToBatch"  class="upload-btn">Batch Upload</button>
      </div>
    </div>

    <!-- Error message for upload failure -->
    <div v-if="uploadErrorMessage" class="error-message">
      {{ uploadErrorMessage }}
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
      imageData: null, // Stores the image data URL
      fileType: null, // Stores the image file type
      isDragging: false,
      uploadErrorMessage: "", // Store error message
      showCloudOptions: false, // Flag to toggle cloud upload options
      SCOPES: "https://www.googleapis.com/auth/drive.readonly",
      CLIENT_ID: import.meta.env.VITE_GOOGLE_CLIENT_ID,
      API_KEY: import.meta.env.VITE_GOOGLE_API_KEY,
      accessToken: null,
      pickerInited: false,
      gisInited: false,
      tokenClient: null,
    };
  },
  async mounted() {
    await this.loadScript("https://apis.google.com/js/api.js", this.gapiLoaded);
    await this.loadScript(
      "https://accounts.google.com/gsi/client",
      this.gisLoaded
    );
  },
  watch: {
    isDragging(newValue) {
      const container = document.querySelector(".upload-container");
      if (newValue) {
        container.classList.add("drag-over");
      } else {
        container.classList.remove("drag-over");
      }
    }
  },
  methods: {
    navigateToBatch() {
      this.$router.push("/batch");
    },
    // handle backend upload
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
          this.fileType = file.type;
          this.uploadErrorMessage = ""; // Clear error message if upload is successful
        } else {
          console.error("No image received from backend");
          this.uploadErrorMessage = "An error occurred. Please try again.";
        }

        return result; // Return response if needed
      } catch (error) {
        console.error("Error uploading image:", error);
        this.uploadErrorMessage = "An error occurred while uploading the image. Please try again.";
      }
    },

    // Handle file selection from the device (local upload)
    async handleLocalFileChange(event) {
      const file = event.target.files[0];

      if (!file) return;

      if (file.size > 10 * 1024 * 1024) {
        // 10MB limit
        alert("File size exceeds 10MB. Please choose a smaller file.");
        return;
      }

      if (file && file.type.startsWith("image")) {
        const reader = new FileReader();
        // reader.onload = (e) => {
        //   this.imageData = e.target.result;
        // };
        reader.readAsDataURL(file);

        // Upload to backend
        await this.uploadToBackend(file);
      } else {
        alert("Please select a valid image file");
      }
    },

    handleDrop(event) {
      event.preventDefault();
      this.isDragging = false; // Reset state

      const files = event.dataTransfer.files;
      if (files.length > 0) {
        const file = files[0];

        if (file.size > 10 * 1024 * 1024) {
          alert("File size exceeds 10MB. Please choose a smaller file.");
          return;
        }

        if (file.type.startsWith("image")) {
          this.uploadToBackend(file);
        } else {
          alert("Please select a valid image file");
        }
      }
    },

    handleDragOver() {
      this.isDragging = true;
    },

    handleDragLeave() {
      this.isDragging = false;
    },

    // Toggle the visibility of cloud options
    toggleCloudOptions() {
      this.showCloudOptions = !this.showCloudOptions;
    },

    // ----------------------------------------------------------------------------------------------------
    // google drive functions

    // load gapi and gdrive
    loadScript(src, callback) {
      return new Promise((resolve, reject) => {
        const existingScript = document.querySelector(`script[src="${src}"]`);
        if (existingScript) {
          if (callback) {
            callback();
          }
          resolve();
          return;
        }
        const script = document.createElement("script");
        script.src = src;
        script.async = true;
        script.defer = true;
        script.onload = () => {
          if (callback) {
            callback();
          }
          resolve();
        };
        script.onerror = (error) => {
          reject(new Error(`Failed to load script: ${src}`));
        };
        document.body.appendChild(script);
      });
    },

    // initialize picker when gapi is loaded
    gapiLoaded() {
      gapi.load("client:picker", this.initializePicker);
    },

    // initialize picker api
    async initializePicker() {
      await gapi.client.load(
        "https://www.googleapis.com/discovery/v1/apis/drive/v3/rest"
      );
      this.pickerInited = true;
    },

    // initialize Google Identity Services (GIS) for OAuth authentication
    gisLoaded() {
      this.tokenClient = google.accounts.oauth2.initTokenClient({
        client_id: this.CLIENT_ID,
        scope: this.SCOPES,
        callback: "",
      });
      this.gisInited = true;
    },

    // triggered on click
    // calls create picker function if no error
    handleGoogleDriveUpload() {
      this.tokenClient.callback = async (response) => {
        if (response.error !== undefined) {
          throw response;
        }
        this.accessToken = response.access_token;
        await this.createPicker();
      };

      if (this.accessToken === null) {
        this.tokenClient.requestAccessToken({ prompt: "consent" });
      } else {
        this.tokenClient.requestAccessToken({ prompt: "" });
      }
    },

    // create and show google picker to select file
    createPicker() {
      const view = new google.picker.View(google.picker.ViewId.DOCS);
      view.setMimeTypes("image/png,image/jpeg,image/jpg");
      const picker = new google.picker.PickerBuilder()
        .enableFeature(google.picker.Feature.NAV_HIDDEN)
        .enableFeature(google.picker.Feature.MULTISELECT_ENABLED)
        .setDeveloperKey(this.API_KEY)
        .setAppId(this.APP_ID)
        .setOAuthToken(this.accessToken)
        .addView(view)
        .addView(new google.picker.DocsUploadView())
        .setCallback(this.pickerCallback)
        .build();
      picker.setVisible(true);
    },

    // process selected file
    async pickerCallback(data) {
      if (data.action === google.picker.Action.PICKED) {
        const document = data[google.picker.Response.DOCUMENTS][0];
        const fileId = document[google.picker.Document.ID];
        this.getFile(this.accessToken, fileId);
        this.showCloudOptions = false;
      }
    },

    // convert to base64 for preview | upload to backend
    getFile(accessToken, fileId) {
      fetch(`https://www.googleapis.com/drive/v3/files/${fileId}?alt=media`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      })
        .then((response) => response.blob())
        .then(async (blob) => {
          if (blob.size > 10 * 1024 * 1024) {
            // 10MB limit
            alert("File size exceeds 10MB. Please choose a smaller file.");
            return;
          }

          const reader = new FileReader();
          reader.readAsDataURL(blob);

          // Upload to backend
          await this.uploadToBackend(blob);
        })
        .catch((error) => {
          console.error("Error fetching the file:", error);
        });
    },

    // ----------------------------------------------------------------------------------------------------
    // Placeholder for Dropbox upload functionality
    handleDropboxUpload() {
      alert("Dropbox upload functionality is not implemented yet.");
    },

    // Placeholder for OneDrive upload functionality
    handleOneDriveUpload() {
      alert("OneDrive upload functionality is not implemented yet.");
    },

    // Handle the "Continue" button click
    // TODO: handle passing image to next page
    handleProceed() {
      if (this.imageData) {
        // Navigate to the image cropping page, passing the image URL as a query parameter
        localStorage.setItem("imageData", this.imageData); // Save to localStorage
        localStorage.setItem("fileType", this.fileType); // Save to localStorage

        this.$router.push({ name: "ImageEdit" });
      } else {
        alert("Please upload an image first.");
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
  transition: background-color 0.3s ease-in-out;
}

.upload-container.drag-over {
  background-color: rgba(226, 226, 226, 0.887); /* Highlight when dragging */
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
  gap: 30px;
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
  display: flex;
  align-items: center;
  justify-content: center;
  width: 200px;
  padding: 12px 12px;
  background-color: #2d3748;
  color: white;
  font-size: 16px;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.upload-btn:hover {
  background-color: black;
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

.error-message {
  color: red;
  font-size: 14px;
  font-weight: bold;
  margin-top: 10px;
  text-align: center;
}

</style>
