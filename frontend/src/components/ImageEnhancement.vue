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
        
        const brightnessValue = this.brightness < 0 
          ? 100 + (this.brightness / 2) // Scale -100 to 50%
          : 100 + this.brightness;
        
        const contrastValue = this.contrast < 0 
          ? 100 + (this.contrast / 2) // Scale -100 to 50%
          : 100 + this.contrast;
        
        // Apply the calculated filters
        ctx.filter = `brightness(${brightnessValue}%) contrast(${contrastValue}%)`;
        ctx.drawImage(this.originalImage, 0, 0);
        
        // If we need to apply skin smoothing
        if (parseInt(this.skinSmoothing) > 0) {
          // Reset filter before getting image data
          ctx.filter = 'none';
          
          // Get the current state of the canvas with brightness and contrast applied
          const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
          
          // Apply skin smoothing
          this.applySkinSmoothing(imageData, parseInt(this.skinSmoothing) / 100);
          
          // Put the processed image data back to canvas
          ctx.putImageData(imageData, 0, 0);
        }
      },
      
      // Improved skin smoothing function
      applySkinSmoothing(imageData, intensity) {
        const canvas = this.$refs.canvas;
        const ctx = canvas.getContext('2d');
        const width = imageData.width;
        const height = imageData.height;
        
        // Create a temporary canvas for blurring
        const tempCanvas = document.createElement('canvas');
        tempCanvas.width = width;
        tempCanvas.height = height;
        const tempCtx = tempCanvas.getContext('2d');
        
        // Draw current image to temp canvas
        tempCtx.putImageData(imageData, 0, 0);
        
        // Create another canvas for the blurred version
        const blurCanvas = document.createElement('canvas');
        blurCanvas.width = width;
        blurCanvas.height = height;
        const blurCtx = blurCanvas.getContext('2d');
        
        // Apply blur
        const radius = Math.floor(5 * intensity); // Adjust blur radius based on intensity
        blurCtx.filter = `blur(${radius}px)`;
        blurCtx.drawImage(tempCanvas, 0, 0);
        
        // Get blurred version
        const blurredData = blurCtx.getImageData(0, 0, width, height);
        
        // Create an edge detection canvas to identify features to preserve
        const edgeCanvas = document.createElement('canvas');
        edgeCanvas.width = width;
        edgeCanvas.height = height;
        const edgeCtx = edgeCanvas.getContext('2d');
        edgeCtx.putImageData(imageData, 0, 0);
        
        // Apply edge detection (simple sobel-like approach)
        edgeCtx.filter = 'grayscale(100%)';
        const edgeData = edgeCtx.getImageData(0, 0, width, height);
        
        // Create a feature map to preserve important facial features
        const featureMap = new Uint8Array(width * height);
        
        // First pass: detect edges and high-contrast areas that likely represent features
        for (let y = 1; y < height - 1; y++) {
          for (let x = 1; x < width - 1; x++) {
            const i = (y * width + x) * 4;
            
            // Get neighboring pixels for edge detection
            const topLeft = (y - 1) * width + (x - 1);
            const top = (y - 1) * width + x;
            const topRight = (y - 1) * width + (x + 1);
            const left = y * width + (x - 1);
            const right = y * width + (x + 1);
            const bottomLeft = (y + 1) * width + (x - 1);
            const bottom = (y + 1) * width + x;
            const bottomRight = (y + 1) * width + (x + 1);
            
            // Get current pixel color
            const r = imageData.data[i];
            const g = imageData.data[i + 1];
            const b = imageData.data[i + 2];
            
            // Calculate brightness
            const brightness = (r + g + b) / 3;
            
            // Calculate gradient magnitude (simple edge detection)
            const gx = 
              -1 * imageData.data[topLeft * 4] +
              -2 * imageData.data[left * 4] +
              -1 * imageData.data[bottomLeft * 4] +
              1 * imageData.data[topRight * 4] +
              2 * imageData.data[right * 4] +
              1 * imageData.data[bottomRight * 4];
              
            const gy = 
              -1 * imageData.data[topLeft * 4] +
              -2 * imageData.data[top * 4] +
              -1 * imageData.data[topRight * 4] +
              1 * imageData.data[bottomLeft * 4] +
              2 * imageData.data[bottom * 4] +
              1 * imageData.data[bottomRight * 4];
            
            const gradientMagnitude = Math.sqrt(gx * gx + gy * gy);
            
            // Feature detection:
            // 1. Mark high-gradient areas (edges) as features to preserve
            // 2. Specifically detect dark areas (eyebrows, lips, etc.)
            // 3. Detect hair (usually has higher contrast and may be darker)
            const isDarkFeature = brightness < 100;
            const isEdge = gradientMagnitude > 30;
            
            // Check for hair by looking at color and texture
            const potentialHair = (
              (r < g + 20 && r < b + 20) || // Darker than surrounding pixels
              (gradientMagnitude > 40) ||   // High texture content
              (brightness < 80)             // Dark areas (like eyebrows)
            );
            
            // If it's an edge, dark feature, or potential hair, mark it in our feature map
            if (isEdge || isDarkFeature || potentialHair) {
              featureMap[y * width + x] = 255; // Mark as feature to preserve
            } else {
              featureMap[y * width + x] = 0;
            }
          }
        }
        
        // Second pass: expand feature areas slightly to ensure full coverage
        const expandedFeatureMap = new Uint8Array(width * height);
        const expandRadius = 2; // How many pixels to expand features by
        
        for (let y = 0; y < height; y++) {
          for (let x = 0; x < width; x++) {
            // If this is already a feature, keep it
            if (featureMap[y * width + x] > 0) {
              expandedFeatureMap[y * width + x] = 255;
              continue;
            }
            
            // Check neighboring pixels
            let isNearFeature = false;
            for (let dy = -expandRadius; dy <= expandRadius && !isNearFeature; dy++) {
              for (let dx = -expandRadius; dx <= expandRadius && !isNearFeature; dx++) {
                const ny = y + dy;
                const nx = x + dx;
                
                // Skip if out of bounds
                if (ny < 0 || ny >= height || nx < 0 || nx >= width) continue;
                
                // If a neighbor is a feature, this is near a feature
                if (featureMap[ny * width + nx] > 0) {
                  isNearFeature = true;
                  break;
                }
              }
            }
            
            expandedFeatureMap[y * width + x] = isNearFeature ? 128 : 0;
          }
        }
        
        // Apply selective blurring with feature preservation
        for (let y = 0; y < height; y++) {
          for (let x = 0; x < width; x++) {
            const i = (y * width + x) * 4;
            
            // Get RGB values
            const r = imageData.data[i];
            const g = imageData.data[i + 1];
            const b = imageData.data[i + 2];
            
            // Check if pixel is likely skin using improved detection
            const isSkin = this.isSkinPixel(r, g, b);
            
            // Calculate feature preservation weight (0 = full blur, 1 = no blur)
            let featureWeight = 0;
            if (expandedFeatureMap[y * width + x] === 255) {
              featureWeight = 1.0; // Fully preserve important features
            } else if (expandedFeatureMap[y * width + x] === 128) {
              featureWeight = 0.7; // Partially preserve areas near features
            }
            
            // If it's skin and not a feature, apply smoothing
            if (isSkin) {
              const blendFactor = intensity * (1 - featureWeight);
              imageData.data[i] = r * (1 - blendFactor) + blurredData.data[i] * blendFactor;
              imageData.data[i + 1] = g * (1 - blendFactor) + blurredData.data[i + 1] * blendFactor;
              imageData.data[i + 2] = b * (1 - blendFactor) + blurredData.data[i + 2] * blendFactor;
              // Alpha channel remains unchanged
            }
          }
        }
      },
      
      isSkinPixel(r, g, b) {
        // Basic RGB rule-based detection for general skin tones
        const basicDetection = (
          r > 95 && g > 40 && b > 20 &&
          r > g && r > b &&
          Math.abs(r - g) > 15 &&
          r - g > 15 && r - b > 15
        );
        
        // Additional check for lighter skin tones
        const lightSkinDetection = (
          r > 220 && g > 180 && b > 170 &&
          Math.abs(r - g) < 15 &&
          r > b && g > b
        );
        
        // Medium skin tones
        const mediumSkinDetection = (
          r > 190 && g > 140 && b > 120 &&
          r > g && g > b &&
          r - g < 50 && g - b < 30
        );
        
        // Exclude features that are unlikely to be skin
        const notFeature = (
          !(r < 60 && g < 60 && b < 60) && // Exclude very dark areas (likely eyebrows/hair)
          !(Math.abs(r - g) < 5 && Math.abs(r - b) < 5 && Math.abs(g - b) < 5) && // Gray/black is not skin
          !(r < g && r < b) // Bluish or greenish pixels are not skin
        );
        
        return (basicDetection || lightSkinDetection || mediumSkinDetection) && notFeature;
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
        const canvas = this.$refs.canvas;
        if (!canvas) return;
        
        const enhancedData = canvas.toDataURL('image/jpeg', 0.92);
        this.enhancedImageData = enhancedData;
        
        this.$emit("enhance-complete", enhancedData);
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