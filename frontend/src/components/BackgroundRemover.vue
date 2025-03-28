<template>
  <h2 class="col-span-12 font-bold p-4 text-2xl">Image</h2>
  <!-- Left side: Background Change Controls -->
  <div class="col-span-4 bg-white border rounded-lg p-4 space-y-4 text-black">
    <h2 class="font-bold text-lg">Change Background</h2>

    <!-- Tabs -->
    <div class="border-b border-gray-200">
      <ul
        class="flex flex-col space-y-2 text-sm font-medium text-center"
        role="tablist"
      >
        <li>
          <button
            @click="activeTab = 'color'"
            :class="tabClass('color')"
            role="tab"
          >
            Replace BG with Color
          </button>
        </li>
        <li>
          <button
            @click="activeTab = 'image'"
            :class="tabClass('image')"
            role="tab"
          >
            Replace BG with Image
          </button>
        </li>
      </ul>
    </div>

    <!-- Tab Content -->
    <div v-if="activeTab === 'color'" class="p-4">
      <label class="block text-gray-700 font-medium mb-2"
        >Select Background Color:</label
      >
      <div class="flex items-center space-x-2">
        <!-- Common Colors -->
        <div class="flex space-x-2">
          <div v-for="c in commonColors" :key="c" 
               :style="{ backgroundColor: c }" 
               @click="applyColor(c)" 
               class="w-10 h-10 rounded-lg shadow-md cursor-pointer hover:opacity-80"></div>
        </div>
      </div>

      <div class="flex items-center justify-center space-x-2 mt-6 mb-2">
        <label class="text-gray-700 font-medium">Or Choose A Custom Colour</label>

        <!-- Color Picker Button with Custom Image -->
        <label class="relative w-12 h-12  rounded-lg cursor-pointer flex items-center justify-center bg-white shadow-md">
          <input type="color" v-model="color" @input="applyColor" class="absolute inset-0 opacity-0 cursor-pointer">
          <img src="/color-wheel.png" alt="Color Picker" class="w-8 h-8">
        </label>
      </div>



      <div class="flex justify-center">
        <label class="text-gray-700 text-xs mb-2"
          >Click on the color box above to choose a custom color.</label
        >
      </div>
    </div>

    <div v-if="activeTab === 'image'" class="p-4">
      <label class="block text-gray-700 font-medium mb-2"
        >Select Background Image:</label
      >
      <div class="grid grid-cols-3 gap-2">
        <!-- Upload Button -->
        <div
          class="relative w-16 h-16 flex items-center justify-center border-2 border-dashed rounded-lg cursor-pointer"
        >
          <span class="text-2xl font-bold">+</span>
          <!-- Invisible Upload Button -->
          <UploadImageButton
            class="absolute inset-0 opacity-0 cursor-pointer"
            @image-uploaded="applyBackground"
          />
        </div>

        <!-- Sample Backgrounds -->
        <div
          v-for="bg in sampleBackgrounds"
          :key="bg.name"
          class="w-16 h-16 bg-cover bg-center rounded-lg cursor-pointer border border-gray-300"
          :style="{ backgroundImage: `url(${bg.url})` }"
          @click="applyBackground(bg.url)"
        ></div>
      </div>

      <!-- Hidden UploadImageButton -->
      <UploadImageButton
        ref="uploadButton"
        @image-uploaded="applyBackground"
        class="hidden"
      />
    </div>

    <!-- Loading Indicator -->
    <div v-if="isProcessing" class="text-gray-600 text-sm animate-pulse">
      Processing image, please wait...
    </div>

    <!-- Error Message -->
    <div v-if="errorMessage" class="text-red-500 text-sm">
      {{ errorMessage }}
    </div>
  </div>

  <!-- Right side: Image Display -->
  <div class="col-span-8 shadow-lg flex justify-center items-center">
    <img
      v-if="displayedImage"
      :src="displayedImage"
      alt="Displayed Image"
      class="max-h-full max-w-full object-contain"
    />
  </div>
</template>

<script>
import UploadImageButton from "./UploadImageButton.vue";

export default {
  props: {
    imageData: String, // Receive image updates from parent
  },
  components: {
    UploadImageButton,
  },
  data() {
    return {
      originalImage: this.imageData || localStorage.getItem("imageData"), // Keep the original
      displayedImage: this.imageData || localStorage.getItem("imageData"), // Shown to user
      isProcessing: false,
      errorMessage: "",
      color: "#FFFFFF",
      backgroundImage: null,
      activeTab: "color",
      commonColors: ["#FFFFFF", "#FF0000", "#0000FF", "#FFFF00", "#000000"],
      sampleBackgrounds: [
        { name: "Office", url: "/office.jpeg" },
      ],
    };
  },
  watch: {
    imageData(newImage) {
      if (!this.originalImage) {
        // Set only if not set before
        this.originalImage = newImage;
      }
      this.displayedImage = newImage; // Only update display, not original
    },
  },

  methods: {
    async applyBackground(image) {
      if (typeof image === "string") {
        this.backgroundImage = image;
        try {
          const base64Image = await this.convertImageToBase64(image);
          this.processImage({
            category: "background",
            backgroundString: base64Image,
            image: this.originalImage,
          });
        } catch (error) {
          console.error("Error converting image:", error);
          this.errorMessage = "Failed to load background image.";
        }
      } else if (image instanceof File || image instanceof Blob) {
        const reader = new FileReader();
        reader.onload = (e) => {
          this.backgroundImage = e.target.result;
          this.processImage({
            category: "background",
            backgroundString: this.backgroundImage,
            image: this.originalImage,
          });
        };
        reader.readAsDataURL(image);
      }
    },

    async convertImageToBase64(imageUrl) {
      return new Promise((resolve, reject) => {
        fetch(imageUrl)
          .then((response) => response.blob())
          .then((blob) => {
            const reader = new FileReader();
            reader.onloadend = () => resolve(reader.result);
            reader.onerror = reject;
            reader.readAsDataURL(blob);
          })
          .catch(reject);
      });
    },

    tabClass(tab) {
      return `w-full py-2 border-b-2 ${
        this.activeTab === tab
          ? "!bg-white !border-gray-500 !text-gray-500 !font-semibold" // Selected tab
          : "!bg-gray-600 !text-white !border-transparent  hover:!bg-gray-700 hover:!text-white" // Unselected tab
      }`;
    },

    triggerUpload() {
      this.$refs.uploadButton.$el.click();
    },

    applyColor(eventOrColor) {
      const color =
        typeof eventOrColor === "string"
          ? eventOrColor
          : eventOrColor.target.value;
      this.color = color;
      this.processImage({
        category: "color",
        colorString: color,
        image: this.originalImage,
      });
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

    async processImage(payload) {
      if (!this.originalImage) {
        // Ensure we always use the original
        this.errorMessage = "No image available for processing.";
        return;
      }

      this.isProcessing = true;
      this.errorMessage = "";

      try {
        // Always resize originalImage, not the modified one

        const formData = new FormData();

        for (const [key, value] of Object.entries(payload)) {
          if (key === "image") {
            const finalBase64 = await this.resizeToClosestMultipleOf32(
              payload.image
            );
            formData.append(
              key,
              this.base64ToFile(finalBase64, "uploaded-image.jpg")
            );
          } else {
            formData.append(key, value);
          }
        }

        for (let pair of formData.entries()) {
          console.log(pair[0] + ": " + pair[1]);
        }
        const response = await fetch("http://localhost:8080/bg/removebg", {
          method: "POST",
          body: formData,
        });

        if (!response.ok) throw new Error("Processing failed");

        const result = await response.json();
        if (result.processedImage) {
          this.displayedImage = result.processedImage; // Update only the display
          localStorage.setItem("imageData", result.processedImage);

          this.$emit("update:imageData", result.processedImage);
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
          const newWidth = this.roundToNearestMultiple(img.width, 32);
          const newHeight = this.roundToNearestMultiple(img.height, 32);

          const canvas = document.createElement("canvas");
          canvas.width = newWidth;
          canvas.height = newHeight;
          const ctx = canvas.getContext("2d");
          ctx.drawImage(img, 0, 0, newWidth, newHeight);

          resolve(canvas.toDataURL("image/png"));
        };
        img.onerror = () => reject(new Error("Failed to load image"));
      });
    },

    roundToNearestMultiple(value, m) {
      const remainder = value % m;
      return remainder <= m / 2 ? value - remainder : value + (m - remainder);
    },
  },
};
</script>
