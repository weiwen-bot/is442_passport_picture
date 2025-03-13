<template>
  <div class="col-span-6 w-full h-[600px]  p-8 bg-white rounded-lg">
    <h2 class="text-lg font-semibold mb-4">Photo Enhancement</h2>

    <div class="flex gap-x-8">
      <div class="image-preview-section flex-[2] bg-gray-100 p-4 rounded-lg">
        <div v-if="!imageData" class="empty-state">
          <p>No image available. Please go back and upload an image first.</p>
        </div>
        <div v-else class="preview-container flex justify-center items-center">
          <canvas ref="canvas" class="preview-canvas w-full h-auto border border-gray-200 rounded-lg"></canvas>
          <div v-if="isLoading" class="loading-overlay fixed inset-0 bg-gray-800 bg-opacity-50 flex items-center justify-center z-50">
            <div class="spinner h-12 w-12 border-4 border-t-blue-500 border-gray-200 rounded-full animate-spin"></div>
            <span class="ml-3 text-white">Processing...</span>
          </div>
        </div>
      </div>

      <div v-if="imageData" class="controls-section flex flex-col items-center bg-gray-50 p-4 rounded-lg">

        <div class="control-group mb-4 flex items-center">
          <label class="min-w-[100px] font-medium">Brightness</label>
          <input type="range" min="-100" max="100" v-model.number="brightness" @input="applyFilters" class="flex-1 mx-4" />
          <span class="min-w-[40px] text-right">{{ brightness }}</span>
        </div>

        <div class="control-group mb-4 flex items-center">
          <label class="min-w-[100px] font-medium">Contrast</label>
          <input type="range" min="-100" max="100" v-model.number="contrast" @input="applyFilters" class="flex-1 mx-4" />
          <span class="min-w-[40px] text-right">{{ contrast }}</span>
        </div>

        <div class="control-group mb-4 flex items-center">
          <label class="min-w-[100px] font-medium">Skin Smoothing</label>
          <input type="range" min="0" max="100" v-model.number="skinSmoothing" @input="applyFilters" class="flex-1 mx-4" />
          <span class="min-w-[40px] text-right">{{ skinSmoothing }}%</span>
        </div>

        <div class="action-buttons flex justify-end mt-6 space-x-3">
          <button @click="resetFilters" class="px-4 py-2 bg-gray-800 text-white rounded">
            Reset Filters
          </button>
          <button @click="applyChanges" class="px-4 py-2 bg-gray-800 text-white rounded">
            Apply Changes
          </button>
        </div>
      </div>
    </div>
  </div>
</template>


  
  <script>
  export default {
    name: 'PhotoEnhancement',
    props: {
      imageData: {
        type: String,
        required: true
      }
    },
    data() {
      return {
        brightness: 0,
        contrast: 0,
        skinSmoothing: 0,
        isLoading: false,
        originalImageData: null,
        originalImage: null
      }
    },
    watch: {
      imageData: {
        immediate: true,
        handler(newValue) {
          if (newValue) {
            this.$nextTick(() => {
              this.loadImageToCanvas(newValue);
            });
          }
        }
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
      
      // This method is now only used for skin smoothing since we're using CSS filters for brightness/contrast
      // Improved skin detection and smoothing that preserves facial features
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
        
        // Apply gaussian blur with CSS filter
        const radius = Math.floor(5 * intensity); // Adjust blur radius based on intensity
        
        // Create another canvas for the blurred version
        const blurCanvas = document.createElement('canvas');
        blurCanvas.width = width;
        blurCanvas.height = height;
        const blurCtx = blurCanvas.getContext('2d');
        
        // Apply blur
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
            // 2. Specifically detect dark areas in skin (eyebrows, lips, etc.)
            const isDarkFeature = this.isSkinPixel(r, g, b) && brightness < 100;
            const isEdge = gradientMagnitude > 30;
            
            // If it's an edge or dark feature, mark it in our feature map
            if (isEdge || isDarkFeature) {
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
            
            // Check if pixel is likely skin
            const isSkin = this.isSkinPixel(r, g, b);
            
            // Calculate feature preservation weight (0 = full blur, 1 = no blur)
            let featureWeight = 0;
            if (expandedFeatureMap[y * width + x] === 255) {
              featureWeight = 1.0; // Fully preserve features
            } else if (expandedFeatureMap[y * width + x] === 128) {
              featureWeight = 0.7; // Partially preserve areas near features
            }
            
            // If skin, blend with blurred version based on feature weight
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
        
        return basicDetection || lightSkinDetection || mediumSkinDetection;
      },
      
      resetFilters() {
        this.brightness = 0;
        this.contrast = 0;
        this.skinSmoothing = 0;
        
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
        // Get the canvas data as base64
        const canvas = this.$refs.canvas;
        if (!canvas) return;
        
        const enhancedImageData = canvas.toDataURL('image/jpeg', 0.92);
        
        // Emit event with enhanced image data
        this.$emit('enhancement-complete', enhancedImageData);
      }
    }
  }
  </script>
  
  <style scoped>
  .image-preview-section {
    display: flex;
    justify-content: center;
    background-color: #f5f5f5;
    border-radius: 8px;
    overflow: hidden;
    min-height: 300px;
    position: relative;
  }
  
  .preview-container {
    position: relative;
    max-width: 100%;
    overflow: hidden;
    display: flex;
    justify-content: center;
  }
  
  .preview-canvas {
    max-width: 100%;
    height: auto;
    object-fit: contain;
  }
  
  .loading-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: white;
  }
  
  .spinner {
    border: 5px solid #f3f3f3;
    border-top: 5px solid #3498db;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    animation: spin 2s linear infinite;
    margin-bottom: 1rem;
  }
  
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
  
  .controls-section {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
    background-color: #f5f5f5;
    padding: 1.5rem;
    border-radius: 8px;
  }
  
  .control-group {
    display: flex;
    align-items: center;
    gap: 1rem;
  }
  
  .control-group label {
    min-width: 120px;
    font-weight: 600;
  }
  
  .control-group input[type="range"] {
    flex: 1;
  }
  
  .control-group span {
    min-width: 40px;
    text-align: right;
  }
  
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 2rem;
    width: 100%;
    height: 300px;
    background-color: #f8f9fa;
    border-radius: 8px;
    color: #6c757d;
    text-align: center;
  }
  </style>