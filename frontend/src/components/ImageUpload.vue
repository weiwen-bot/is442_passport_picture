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
      <div class="button-wrapper">
        <button @click="toggleCloudOptions" class="upload-btn">Choose from Cloud</button>
        <div v-if="showCloudOptions" class="cloud-options">
          <button @click="handleGoogleDriveUpload" class="cloud-btn">
            <img src="https://upload.wikimedia.org/wikipedia/commons/4/4c/Google_Drive_icon.svg" alt="Google Drive" class="cloud-icon" />
            Google Drive
          </button>
          <button v-if="false" @click="handleDropboxUpload" class="cloud-btn">
            <img src="https://upload.wikimedia.org/wikipedia/commons/3/3b/Dropbox_logo_2015.svg" alt="Dropbox" class="cloud-icon" />
            Dropbox
          </button>
          <button v-if="false" @click="handleOneDriveUpload" class="cloud-btn">
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

      SCOPES: 'https://www.googleapis.com/auth/drive.metadata.readonly',
      CLIENT_ID: import.meta.env.VITE_GOOGLE_CLIENT_ID,
      API_KEY: import.meta.env.VITE_GOOGLE_API_KEY,
      accessToken: null,
      pickerInited: false,
      gisInited: false,
      tokenClient: null,
    };
  },
  async mounted() {
    await this.loadScript('https://apis.google.com/js/api.js', this.gapiLoaded);
    await this.loadScript('https://accounts.google.com/gsi/client', this.gisLoaded);
  },
  methods: {
  

    // Handle file selection from the device (local upload)
    async handleLocalFileChange(event) {
      const file = event.target.files[0];
      
      if (!file) return; // No file selected
      if (file && file.type.startsWith('image')) {
        const reader = new FileReader();
        reader.onload = (e) => {
          this.imageData = e.target.result; // Store image data URL
        };
        reader.readAsDataURL(file); // Read the image file as a Data URL

        const formData = new FormData();
        formData.append("image", file); // Corrected from `this.selectedImage`

        // Backend Upload
        try {
          const response = await fetch("http://localhost:8080/image/upload", {
            method: "POST",
            body: formData,
            headers: { Accept: "application/json" },
          });
  
          if (!response.ok) throw new Error("Upload failed");
  
          const result = await response.json();
          console.log("Upload Success:", result);
  
        } catch (error) {
          console.error("Error uploading image:", error);
        }

      } else {
        alert('Please select a valid image file');
      }
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
        const script = document.createElement('script');
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
      gapi.load('client:picker', this.initializePicker);
    },

    // initialize picker api
    async initializePicker() {
      await gapi.client.load('https://www.googleapis.com/discovery/v1/apis/drive/v3/rest');
      this.pickerInited = true;
    },

    // initialize Google Identity Services (GIS) for OAuth authentication
    gisLoaded() {
      this.tokenClient = google.accounts.oauth2.initTokenClient({
        client_id: this.CLIENT_ID,
        scope: this.SCOPES,
        callback: '',
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
        this.tokenClient.requestAccessToken({ prompt: 'consent' });
      } else {
        this.tokenClient.requestAccessToken({ prompt: '' });
      }
    },

    // create and show google picker to select file
    createPicker() {
      const view = new google.picker.View(google.picker.ViewId.DOCS);
      view.setMimeTypes('image/png,image/jpeg,image/jpg');
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

    // process selected image
    async pickerCallback(data) {
      if (data.action === google.picker.Action.PICKED) {
        const document = data[google.picker.Response.DOCUMENTS][0];
        const fileId = document[google.picker.Document.ID];
        this.getFile(this.accessToken, fileId);
        this.showCloudOptions = false;
      }
    },

    // convert selected image to base64
    getFile(accessToken, fileId) {
      fetch(`https://www.googleapis.com/drive/v3/files/${fileId}?alt=media`, {
        method: "GET",
        headers: {
          "Authorization": `Bearer ${accessToken}`,
        },
      })
      .then(response => response.blob())
      .then(blob => {
        const reader = new FileReader();
        reader.onloadend = () => {
          const base64String = reader.result;
          this.imageData = base64String; 
        };
        reader.readAsDataURL(blob);  
        localStorage.setItem('imageData', this.imageData); // Save to localStorage
      })
      .catch(error => {
        console.error("Error fetching the file:", error);
      });
    },

    // ----------------------------------------------------------------------------------------------------
    // Placeholder for Dropbox upload functionality
    handleDropboxUpload() {
      alert('Dropbox upload functionality is not implemented yet.');
    },

    // Placeholder for OneDrive upload functionality
    handleOneDriveUpload() {
      alert('OneDrive upload functionality is not implemented yet.');
    },

    // Handle the "Continue" button click
    // TODO: handle passing image to next page
    handleProceed() {
      if (this.imageData) {
        // Navigate to the image cropping page, passing the image URL as a query parameter
        localStorage.setItem('imageData', this.imageData); // Save to localStorage
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
  gap: 20px;
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
