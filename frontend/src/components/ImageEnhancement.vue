<template>
  <!-- Left side: Enhancement Controls -->
  <h2 class="col-span-12 font-bold p-4 text-2xl">Image</h2>
  <div class="col-span-4 bg-white border rounded-lg p-4 space-y-4 text-black">
    <h2 class="font-bold text-lg">Enhance Your Image</h2>
    
    <!-- Brightness Adjustment -->
    <div class="space-y-2">
      <label for="brightness" class="block font-medium font-semibold text-left">Brightness: {{ brightness }}</label>
      <input 
        type="range" 
        id="brightness" 
        min="-100" 
        max="100" 
        step="1" 
        v-model.number="brightness" 
        class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
        @input="applyFilters"
      >
    </div>
    
    <!-- Contrast Adjustment -->
    <div class="space-y-2">
      <label for="contrast" class="block font-medium font-semibold text-left">Contrast: {{ contrast }}</label>
      <input 
        type="range" 
        id="contrast" 
        min="-100" 
        max="100" 
        step="1" 
        v-model.number="contrast" 
        class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
        @input="applyFilters"
      >
    </div>
    
    <!-- Skin Smoothing Slider -->
    <div class="space-y-2">
      <label for="skinSmoothing" class="block font-medium font-semibold text-left">Skin Smoothing: {{ skinSmoothing }}%</label>
      <input 
        type="range" 
        id="skinSmoothing" 
        min="0" 
        max="100" 
        step="1" 
        v-model.number="skinSmoothing" 
        class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
        @input="applyFilters"
      >
    </div>
    
    
    <!-- Action Buttons -->
    <div class="pt-3 flex space-x-2">
      <!-- Reset Button -->
      <button
        class="sm:py-3 px-4 rounded-lg bg-gray-500 text-white"
        @click="resetFilters"
        :disabled="isLoading"
      >
        Reset Filters
      </button>
      
      <!-- Apply Button -->
      <button
        class="sm:py-3 px-4 rounded-lg bg-green-500 text-white flex-1"
        @click="applyChanges"
        :disabled="isLoading"
      >
        {{ isLoading ? 'Processing...' : 'Apply' }}
      </button>
    </div>
  </div>

  <!-- Right side: Image Display -->
  <div class="col-span-8 shadow-lg flex justify-center items-center">
    <!-- Enhanced Image Display (shows after applying enhancements) -->
    <img
      v-if="enhancedImageData"
      :src="enhancedImageData"
      alt="Enhanced Image"
      class="max-w-full max-h-[500px] w-auto h-auto object-contain border rounded shadow-md"
    />
    <!-- Canvas for Preview while adjusting -->
    <div v-else class="relative w-full h-full flex justify-center items-center">
      <canvas 
        v-if="imageData" 
        ref="canvas" 
        class="h-full w-auto max-w-full object-contain border shadow-md">
      </canvas>
      
      <!-- Loading overlay -->
      <div 
        v-if="isLoading" 
        class="absolute inset-0 bg-black bg-opacity-40 flex items-center justify-center text-white z-10"
      >
        <span>Processing...</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    imageData: String,
  },
  emits: ["update:imageData", "enhance-complete"],
  data() {
    return {
      brightness: 0,
      contrast: 0,
      skinSmoothing: 0,
      isLoading: false,
      originalImage: null,
      originalImageData: null,
      enhancedImageData: null
    };
  },
  watch: {
    imageData: {
      handler(newVal) {
        if (newVal) {
          this.enhancedImageData = null; // Reset enhanced image when new image is loaded
          this.$nextTick(() => {
            this.loadImageToCanvas(newVal);
          });
        }
      },
      immediate: true
    }
  },
  methods: {
    loadImageToCanvas(src) {
      this.isLoading = true;
      
      const canvas = this.$refs.canvas;
      if (!canvas) return;
      
      const ctx = canvas.getContext('2d');
      
      this.originalImage = new Image();
      this.originalImage.crossOrigin = "Anonymous"; // Handle CORS if needed
      
      this.originalImage.onload = () => {
        // Set canvas dimensions to match the image
        canvas.width = this.originalImage.width;
        canvas.height = this.originalImage.height;
        
        // Draw image on canvas
        ctx.drawImage(this.originalImage, 0, 0);
        
        // Store original image data for reset
        this.originalImageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        
        this.isLoading = false;
      };
      
      this.originalImage.onerror = (err) => {
        console.error("Error loading image:", err);
        this.isLoading = false;
      };
      
      this.originalImage.src = src;
    },
    
    applyFilters() {
      // Reset enhanced image when user adjusts filters
      this.enhancedImageData = null;
      
      if (!this.originalImage || !this.$refs.canvas) return;
      
      const canvas = this.$refs.canvas;
      const ctx = canvas.getContext('2d');
      
      // Clear the canvas
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      
      // Apply brightness and contrast first
      const brightnessValue = this.brightness < 0 
        ? 100 + (this.brightness / 2) // Scale -100 to 50%
        : 100 + this.brightness;
      
      const contrastValue = this.contrast < 0 
        ? 100 + (this.contrast / 2) // Scale -100 to 50%
        : 100 + this.contrast;
      
      // Apply brightness and contrast filters
      ctx.filter = `brightness(${brightnessValue}%) contrast(${contrastValue}%)`;
      
      // Draw original image with filters applied
      ctx.drawImage(this.originalImage, 0, 0);
      
      // Reset filters before getting image data
      ctx.filter = 'none';
      
      // If skin smoothing is enabled, apply it as a separate step
      if (this.skinSmoothing > 0) {
        // Get the current canvas image data
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        
        // Apply skin smoothing with intensity based on slider value (0-100)
        const smoothingIntensity = this.skinSmoothing / 100;
        this.applySkinSmoothing(imageData, smoothingIntensity);
        
        // Put the processed image data back to canvas
        ctx.putImageData(imageData, 0, 0);
      }
    },
    
    // Improved skin smoothing function
    applySkinSmoothing(imageData, intensity) {
      const width = imageData.width;
      const height = imageData.height;
      const data = imageData.data;
      
      // Step 1: Create a temporary canvas for the blur effect
      const tempCanvas = document.createElement('canvas');
      tempCanvas.width = width;
      tempCanvas.height = height;
      const tempCtx = tempCanvas.getContext('2d');
      tempCtx.putImageData(imageData, 0, 0);
      
      // Step 2: Create a skin mask to identify skin pixels
      const skinMask = new Uint8Array(width * height);
      let skinCount = 0;
      
      for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
          const i = (y * width + x) * 4;
          const r = data[i];
          const g = data[i + 1];
          const b = data[i + 2];
          
          if (this.isSkinPixel(r, g, b)) {
            skinMask[y * width + x] = 1;
            skinCount++;
          }
        }
      }
      
      // Step 3: Calculate adaptive blur radius based on skin ratio and intensity
      const skinRatio = skinCount / (width * height);
      console.log(`Skin ratio: ${skinRatio.toFixed(2)}`);
      const adaptiveRadius = Math.max(2, Math.min(10, Math.floor(10 * intensity)));
      
      // Step 4: Apply blur to the temp canvas (for skin areas)
      const blurCanvas = document.createElement('canvas');
      blurCanvas.width = width;
      blurCanvas.height = height;
      const blurCtx = blurCanvas.getContext('2d');
      
      // Setup blur strength based on intensity
      blurCtx.filter = `blur(${adaptiveRadius}px)`;
      blurCtx.drawImage(tempCanvas, 0, 0);
      
      // Get the blurred image data
      const blurredData = blurCtx.getImageData(0, 0, width, height).data;
      
      // Step 5: Detect edges to preserve facial features
      const edgeData = this.detectEdges(imageData);
      
      // Step 6: Apply smoothing selectively
      for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
          const idx = (y * width + x) * 4;
          const i = y * width + x;
          
          // Only smooth skin pixels
          if (skinMask[i]) {
            const edgeStrength = edgeData[i] / 255; // Normalize to 0-1
            const preserveDetail = Math.max(0, Math.min(1, edgeStrength * 2)); // Amplify edge importance
            
            // Calculate how much to smooth (less on edges, more on flat areas)
            const smoothFactor = intensity * (1 - preserveDetail);
            
            // Apply smoothing as weighted average between original and blurred
            data[idx] = Math.round(data[idx] * (1 - smoothFactor) + blurredData[idx] * smoothFactor);
            data[idx + 1] = Math.round(data[idx + 1] * (1 - smoothFactor) + blurredData[idx + 1] * smoothFactor);
            data[idx + 2] = Math.round(data[idx + 2] * (1 - smoothFactor) + blurredData[idx + 2] * smoothFactor);
            // Alpha channel remains unchanged
          }
        }
      }
      
      console.log(`Applied skin smoothing with intensity ${intensity} and radius ${adaptiveRadius}px`);
      return imageData;
    },
    
    // Detect edges to preserve facial features
    detectEdges(imageData) {
      const width = imageData.width;
      const height = imageData.height;
      const data = imageData.data;
      const edgeMask = new Uint8Array(width * height);
      
      // Convert to grayscale for edge detection
      const grayData = new Uint8Array(width * height);
      for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
          const idx = (y * width + x) * 4;
          // Convert RGB to grayscale
          grayData[y * width + x] = Math.round(
            0.299 * data[idx] + 0.587 * data[idx + 1] + 0.114 * data[idx + 2]
          );
        }
      }
      
      // Apply Sobel operator for edge detection
      for (let y = 1; y < height - 1; y++) {
        for (let x = 1; x < width - 1; x++) {
          const i = y * width + x;
          
          // 3x3 grid for Sobel operator
          const tl = grayData[(y - 1) * width + (x - 1)];
          const t = grayData[(y - 1) * width + x];
          const tr = grayData[(y - 1) * width + (x + 1)];
          const l = grayData[y * width + (x - 1)];
          const r = grayData[y * width + (x + 1)];
          const bl = grayData[(y + 1) * width + (x - 1)];
          const b = grayData[(y + 1) * width + x];
          const br = grayData[(y + 1) * width + (x + 1)];
          
          // Sobel X gradient
          const gx = -1 * tl + 1 * tr + 
                     -2 * l + 2 * r + 
                     -1 * bl + 1 * br;
                     
          // Sobel Y gradient 
          const gy = -1 * tl + -2 * t + -1 * tr + 
                     1 * bl + 2 * b + 1 * br;
          
          // Calculate gradient magnitude
          const g = Math.sqrt(gx * gx + gy * gy);
          
          // Normalize and store edge strength (0-255)
          edgeMask[i] = Math.min(255, Math.round(g));
        }
      }
      
      // Apply slight Gaussian blur to the edge mask to smooth it
      const tempEdgeMask = new Uint8Array(width * height);
      for (let y = 1; y < height - 1; y++) {
        for (let x = 1; x < width - 1; x++) {
          const i = y * width + x;
          
          // Simple 3x3 gaussian kernel
          const center = edgeMask[i] * 0.4;
          const sides = (
            edgeMask[(y - 1) * width + x] + 
            edgeMask[y * width + (x - 1)] + 
            edgeMask[y * width + (x + 1)] + 
            edgeMask[(y + 1) * width + x]
          ) * 0.1;
          const corners = (
            edgeMask[(y - 1) * width + (x - 1)] + 
            edgeMask[(y - 1) * width + (x + 1)] + 
            edgeMask[(y + 1) * width + (x - 1)] + 
            edgeMask[(y + 1) * width + (x + 1)]
          ) * 0.05;
          
          tempEdgeMask[i] = Math.round(center + sides + corners);
        }
      }
      
      return tempEdgeMask;
    },
    
    // Improved skin detection function
    isSkinPixel(r, g, b) {
      // Rule 1: Basic RGB-based skin detection
      const basicSkinCondition = (
        r > 95 && g > 40 && b > 20 &&  // Skin is reasonably bright
        r > g && r > b &&             // Red channel is the strongest
        Math.abs(r - g) > 15 &&       // Red and green have enough difference
        (r - g) > 15 && (r - b) > 15   // Red is much stronger than the others
      );
      
      // Rule 2: Calculate the max difference between RGB channels
      const maxDiff = Math.max(r, g, b) - Math.min(r, g, b);
      const colorVariety = maxDiff > 15;  // Ensures it's not just a gray pixel
      
      // Rule 3: Detection for lighter skin tones
      const lightSkinCondition = (
        r > 200 && g > 160 && b > 140 &&
        Math.abs(r - g) <= 35 &&
        r > b && g > b
      );
      
      // Rule 4: Exclude certain colors that are definitely not skin
      const notSkin = (
        (r < 35 && g < 35 && b < 35) ||   // Too dark, likely hair or eyebrows
        (r > 250 && g > 250 && b > 250) || // Too white, likely background
        Math.abs(r - g) < 5 && Math.abs(r - b) < 5 && Math.abs(g - b) < 5 || // Gray-ish colors
        (b > r && b > g) ||                // Blue is dominant, unlikely to be skin
        (g > r && g > b + 10)              // Green is too dominant
      );
      
      return (basicSkinCondition || lightSkinCondition) && colorVariety && !notSkin;
    },
    
    resetFilters() {
      this.brightness = 0;
      this.contrast = 0;
      this.skinSmoothing = 0;
      this.enhancedImageData = null; 
      
      if (this.originalImage && this.$refs.canvas) {
        const canvas = this.$refs.canvas;
        const ctx = canvas.getContext('2d');
        
        // Clear canvas and redraw original image
        ctx.filter = 'none';
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.drawImage(this.originalImage, 0, 0);
      }
    },
    
    applyChanges() {
      if (!this.$refs.canvas) return;
      
      this.isLoading = true;
      
      // Use setTimeout to allow the UI to update with loading state
      setTimeout(() => {
        try {
          const canvas = this.$refs.canvas;
          
          // Get the final enhanced image as a data URL
          const enhancedData = canvas.toDataURL('image/jpeg', 0.92);
          this.enhancedImageData = enhancedData;
          
          // Emit the enhanced image data to the parent component
          this.$emit("enhance-complete", enhancedData);
        } catch (error) {
          console.error("Error applying changes:", error);
        } finally {
          this.isLoading = false;
        }
      }, 50); // Small delay to ensure loading state is visible
    }
  }
};
</script>

<style scoped>
.bg-green-500 {
  background-color: #48bb78 !important; 
}

.bg-gray-500 {
  background-color: #2d3748 !important;
}

/* Custom styling for range inputs */
input[type="range"] {
  -webkit-appearance: none;
  height: 8px;
  background: #e2e8f0;
  border-radius: 5px;
  outline: none;
}

input[type="range"]::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #2d3748;
  cursor: pointer;
}

input[type="range"]::-moz-range-thumb {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #2d3748;
  cursor: pointer;
  border: none;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>