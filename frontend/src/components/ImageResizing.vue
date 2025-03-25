<template>
  <!-- Left side: Country Selection -->
  <h2 class="col-span-12 font-bold p-4 text-2xl">Image</h2>
  <div
    class="col-span-4 bg-white border rounded-lg shadow-lg p-4 space-y-2 text-black"
  >
    <h2 class="font-bold text-lg">Resize Your Image</h2>

    <div class="max-w-sm space-y-3 pt-2">
      <label
        for="country"
        class="block font-medium mb-2 font-semibold text-left"
        >Select a Country
      </label>
      <select
        v-model="selectedCountry"
        @change="handleResize"
        id="country"
        class="sm:py-3 ps-3 pe-10 block w-full rounded-lg border border-gray-300"
        :disabled="hasResized || isSelectionLocked('country')"
      >
        <option value="">-- Select Country --</option>
        <option
          v-for="country in countryList"
          :key="country.code"
          :value="country.code"
        >
          {{ country.name }} ({{ country.dimensions }})
        </option>
      </select>
    </div>

    <div class="max-w-sm space-y-3 pt-2">
      <label
        for="template"
        class="block font-medium mb-2 font-semibold text-left"
        >Or Choose a Template
      </label>
      <select
        v-model="selectedTemplate"
        @change="handleResize"
        id="template"
        class="sm:py-3 ps-3 pe-10 block w-full rounded-lg border border-gray-300"
        :disabled="hasResized || isSelectionLocked('template')"
      >
        <option value="">-- Select Template --</option>
        <option
          v-for="template in templateList"
          :key="template.label"
          :value="template.size"
        >
          {{ template.label }} ({{ template.size }})
        </option>
      </select>
    </div>

    <div class="max-w-sm space-y-3 pt-2">
      <label class="block font-medium mb-2 font-semibold text-left">
        Custom Size (Aspect Ratio Locked)
      </label>
      <div class="flex items-center space-x-2">
        <!-- Width Input -->
        <div class="flex items-center space-x-1">
          <input
            type="number"
            v-model.number="customWidth"
            @input="updateHeight"
            @change="handleResize"
            class="resize-input"
            :placeholder="originalWidth ? originalWidth + ' px' : 'Width'"
            :disabled="hasResized || isSelectionLocked('custom')"
          />
          <span class="text-gray-600 text-sm px-label">px</span>
        </div>

        <!-- "×" Symbol -->
        <span class="text-gray-700 font-semibold text-lg">×</span>

        <!-- Height Input -->
        <div class="flex items-center space-x-1">
          <input
            type="number"
            v-model.number="customHeight"
            @input="updateWidth"
            @change="handleResize"
            class="resize-input"
            :placeholder="originalHeight ? originalHeight + ' px' : 'Height'"
            :disabled="hasResized || isSelectionLocked('custom')"
          />
          <span class="text-gray-600 text-sm px-label">px</span>
        </div>
      </div>
    </div>
  </div>

  <!-- Right side: Image Display -->
  <div class="col-span-8 shadow-lg flex justify-center items-center">
    <img
      v-if="resizedImage"
      :src="resizedImage"
      :key="'resized-' + Date.now()"
      alt="Resized"
      class="max-w-full max-h-[500px] w-auto h-auto object-contain border rounded shadow-md"
    />

    <img
      v-else-if="baseImage"
      :src="baseImage"
      :key="'base-' + Date.now()"
      alt="Original"
      class="h-full w-auto max-w-full object-contain border rounded shadow-md"
    />

    <div v-else class="text-gray-500 text-center">
      No image yet. Click "Resize" to generate an image.
    </div>
  </div>
</template>

<script>
export default {
  props: {
    imageData: String, // Parent passes the original image
  },
  emits: [
    "update:imageData",
    "resize-complete",
    "request-undo",
    "request-revert",
    "request-redo",
  ],
  data() {
    return {
      baseImage: this.imageData,
      originalImage: this.imageData,
      resizedImage: null, // Stores resized image
      selectedCountry: "",
      selectedTemplate: "",
      countryList: [],
      templateList: [],
      hasResized: false,
      aspectRatio: 1,
      customWidth: null,
      customHeight: null,
      userSelection: null, // Tracks which option the user selected first!
    };
  },
  watch: {
    async imageData(newImage, oldImage) {
      console.log("watch: imageData triggered");
      if (!newImage || newImage === oldImage) return;

      // Extract dimensions from the new image
      await this.extractImageDimensions(newImage);

      this.baseImage = newImage;

      // Check if we're reverting to original (can be a deep comparison if needed)
      const isRevertToOriginal =
        !this.hasResized ||
        (this.originalImage && newImage === this.originalImage);

      if (isRevertToOriginal) {
        // Case: Reverting to the original uploaded image
        console.log("Reverting UI state to original.");

        this.resizedImage = null;
        this.hasResized = false; // Unlock the UI
        this.selectedCountry = ""; // Reset country selection
        this.selectedTemplate = ""; // Reset template selection
        this.userSelection = null; // Clear user selection mode

        // Reset input fields to original image dimensions
        this.$nextTick(() => {
          this.customWidth = this.extractedWidth;
          this.customHeight = this.extractedHeight;
        });
      } else if (this.hasResized) {
        // Case: After resizing, update fields but keep them disabled
        console.log(
          "Updating fields with resized dimensions:",
          this.extractedWidth,
          this.extractedHeight
        );
        this.$nextTick(() => {
          this.customWidth = this.extractedWidth;
          this.customHeight = this.extractedHeight;
        });
      } else {
        // Case: Undo operation (go back one step)
        this.resizedImage = null;
        this.hasResized = false;
        this.$nextTick(() => {
          this.customWidth = this.originalWidth;
          this.customHeight = this.originalHeight;
        });
      }
    },
  },
  mounted() {
    this.fetchCountryList();
    this.fetchTemplateList();
    if (this.imageData) {
      this.originalImage = this.imageData; // Store original image
      this.extractImageDimensions(this.imageData); // Extract dimensions
    }
  },
  methods: {
    async fetchCountryList() {
      try {
        const response = await fetch("http://localhost:8080/image/countries");
        if (!response.ok) throw new Error("Failed to fetch country list");

        this.countryList = await response.json();
        return this.countryList;
      } catch (error) {
        console.error("Error fetching country list:", error);
        this.countryList = [];
        return [];
      }
    },
    async fetchTemplateList() {
      try {
        const response = await fetch("http://localhost:8080/image/templates");
        if (!response.ok) throw new Error("Failed to fetch template list");
        this.templateList = await response.json();
      } catch (error) {
        console.error("Error fetching template list:", error);
      }
    },
    extractImageDimensions(imageSrc) {
      if (!imageSrc) return Promise.resolve;

      return new Promise((resolve) => {
        const img = new Image();
        img.onload = () => {
          this.originalWidth = img.width;
          this.originalHeight = img.height;
          this.aspectRatio = img.width / img.height;

          // Store extracted values in separate variables
          this.extractedWidth = img.width;
          this.extractedHeight = img.height;

          console.log(
            `Image dimensions extracted: ${this.extractedWidth}x${this.extractedHeight}`
          );
          // Use $nextTick to ensure Vue updates fields properly
          this.$nextTick(() => {
            this.customWidth = this.extractedWidth;
            this.customHeight = this.extractedHeight;
            console.log(
              `Input fields updated: ${this.customWidth}x${this.customHeight}`
            );
            resolve(); // Resolve the Promise after dimensions are updated
          });
        };
        img.src = imageSrc;
      });
    },
    // New methods for handling selection
    countrySelected() {
      if (this.selectedCountry) {
        this.userSelection = "country";
        this.handleResize();
      }
    },

    templateSelected() {
      if (this.selectedTemplate) {
        this.userSelection = "template";
        this.handleResize();
      }
    },

    customSizeChanged() {
      if (
        (this.customWidth && this.customWidth !== this.originalWidth) ||
        (this.customHeight && this.customHeight !== this.originalHeight)
      ) {
        this.userSelection = "custom";
        // Don't resize immediately with custom size - wait for user to finish typing
      }
    },
    isSelectionLocked(type) {
      // If the image has been resized, lock everything
      if (this.hasResized) {
        return true;
      }
      // Otherwise, only lock fields that weren't the first selected
      return this.userSelection !== null && this.userSelection !== type;
    },
    async handleResize() {
      console.log("handleResize() triggered!");

      // Store selections before resizing (so they are not reset)
      const prevSelection = {
        country: this.selectedCountry,
        template: this.selectedTemplate,
        customWidth: this.customWidth,
        customHeight: this.customHeight,
      };

      // Determine which option was selected
      if (!this.userSelection) {
        if (this.selectedCountry) {
          this.userSelection = "country";
        } else if (this.selectedTemplate) {
          this.userSelection = "template";
        } else if (
          this.customWidth !== this.originalWidth ||
          this.customHeight !== this.originalHeight
        ) {
          this.userSelection = "custom";
        }
      }

      if (this.hasResized) {
        console.log("Image already resized, skipping request.");
        return;
      }

      // Ensure dimensions are updated based on selection
      let newWidth = this.customWidth;
      let newHeight = this.customHeight;

      if (this.selectedCountry) {
        // Find the country's passport size from the country list
        const countryObj = this.countryList.find(
          (c) => c.code === this.selectedCountry
        );
        if (countryObj) {
          const [w, h] = countryObj.dimensions.split("x").map(Number);
          newWidth = w;
          newHeight = h;
        }
      } else if (this.selectedTemplate) {
        // Find template dimensions if selected
        const templateObj = this.templateList.find(
          (t) => t.size === this.selectedTemplate
        );
        if (templateObj) {
          const [w, h] = templateObj.size.split("x").map(Number);
          newWidth = w;
          newHeight = h;
        }
      }

      let resizeParams = {
        country: this.selectedCountry || null,
        template: this.selectedTemplate || null,
        customWidth: newWidth || null,
        customHeight: newHeight || null,
      };

      console.log("Resize Parameters:", resizeParams);

      try {
        const formData = new FormData();
        formData.append(
          "image",
          this.base64ToFile(this.imageData, "uploaded-image.jpg")
        );
        for (const [key, value] of Object.entries(resizeParams)) {
          if (value !== null) formData.append(key, value);
        }

        const response = await fetch("http://localhost:8080/image/resize", {
          method: "POST",
          body: formData,
        });

        if (!response.ok) throw new Error("Image resizing failed");
        const result = await response.json();
        console.log("Resize API Response:", result);

        if (result.status === "success") {
          this.resizedImage = result.image;

          this.$emit("resize-complete", result.image);
          console.log("Image updated successfully!");

          // Restore selections after resizing (instead of resetting them)
          this.selectedCountry = prevSelection.country;
          this.selectedTemplate = prevSelection.template;
          this.customWidth = resizeParams.customWidth;
          this.customHeight = resizeParams.customHeight;

          // Set hasResized after successful resize
          this.hasResized = true;
        } else {
          console.error("Resize response did not contain a valid image.");
        }
      } catch (error) {
        console.error("Error resizing image:", error);
      }
    },
    updateHeight() {
      if (this.customWidth) {
        this.customHeight = Math.round(this.customWidth / this.aspectRatio);
      }
    },
    updateWidth() {
      if (this.customHeight) {
        this.customWidth = Math.round(this.customHeight * this.aspectRatio);
      }
    },
    requestUndo() {
      console.log("Undo button clicked! Emitting event to parent.");
      this.resizedImage = null; // Clear the resized image
      this.$emit("request-undo");
    },

    requestRevert() {
      console.log(
        "Revert to Original button clicked! Emitting event to parent."
      );
      this.resizedImage = null; // Clear the resized image
      this.hasResized = false;
      this.$emit("request-revert");
    },

    requestRedo() {
      console.log("Redo button clicked! Emitting event to parent.");
      this.$emit("request-redo");
    },

    base64ToFile(base64String, fileName) {
      const arr = base64String.split(",");
      const mime = arr[0].match(/:(.*?);/)[1];
      const bstr = atob(arr[1]);
      let n = bstr.length;
      const u8arr = new Uint8Array(n);
      while (n--) {
        u8arr[n] = bstr.charCodeAt(n);
      }
      return new File([u8arr], fileName, { type: mime });
    },
  },
};
</script>

<style scoped>
select:disabled,
input:disabled {
  background-color: #f0f0f0;
  cursor: not-allowed;
  opacity: 0.7;
}

.px-label {
  display: flex;
  align-items: center;
  height: 100%;
}

.text-gray-700 {
  display: flex;
  align-items: center;
}

.resize-input {
  width: 80px; /* Set a fixed width to ensure consistency */
  min-width: 80px;
  max-width: 80px;
  padding: 6px;
  text-align: center;
  box-sizing: border-box; /* Ensures padding doesn't affect width */
  border: 1px solid #ccc; /* Explicitly define the border */
  border-radius: 6px; /* Keeps the rounded edges consistent */
  background-color: white; /* Ensures no transparency issues */
}

/* Loading spinner styling */
.spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border-left-color: #007bff;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

/* Keyframes for spinner animation */
@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
