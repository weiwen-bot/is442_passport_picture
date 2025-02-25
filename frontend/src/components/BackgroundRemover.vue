<template>
  <div class="container">
    <h2>Upload Image for Background Removal</h2>

    <p class="instructions" v-if="imageUrl">
    Adjust the box to fit around the face. Ensure the entire face is inside the box before proceeding.
    </p>
    <input type="file" @change="handleFileUpload" accept="image/png, image/jpeg" />
    <br />

    <div v-if="imageUrl" class="image-container">
      <img :src="imageUrl" ref="imageRef" @load="setImageDimensions" />

      <div
        v-if="boundingBox.visible"
        class="bounding-box"
        :style="{
          left: boundingBox.x + 'px',
          top: boundingBox.y + 'px',
          width: boundingBox.width + 'px',
          height: boundingBox.height + 'px',
        }"
        @mousedown="startDragging"
      >
        <div v-for="handle in resizeHandles" :key="handle.position"
          class="resize-handle"
          :class="handle.position"
          @mousedown.stop="startResizing(handle.position, $event)"
        ></div>
      </div>
    </div>

    <button @click="processImage" :disabled="!imageFile">Remove Background</button>
  </div>
</template>

<script>
import axios from "axios";

export default {
  data() {
    return {
      imageFile: null,
      imageUrl: null,
      imageWidth: 0,
      imageHeight: 0,
      boundingBox: {
        x: 50,
        y: 50,
        width: 100,
        height: 100,
        visible: false,
      },
      dragging: false,
      resizing: false,
      resizeDirection: null,
      startX: 0,
      startY: 0,
    };
  },
  computed: {
    resizeHandles() {
      return [
        { position: "top-left" }, { position: "top" }, { position: "top-right" },
        { position: "left" }, { position: "right" },
        { position: "bottom-left" }, { position: "bottom" }, { position: "bottom-right" },
      ];
    }
  },
  methods: {

    async processImage() {
        if (!this.imageFile) {
            alert("Please upload an image first.");
            return;
        }

        const imgElement = this.$refs.imageRef;
        const imageRect = imgElement.getBoundingClientRect();

        const relativeX = this.boundingBox.x / this.imageWidth; // X & Y are relative to the image, meaning that 0 = left/top, 1 = right/bottom
        const relativeY = this.boundingBox.y / this.imageHeight; // X & Y are relative to the image, meaning that 0 = left/top, 1 = right/bottom
        const relativeWidth = this.boundingBox.width / this.imageWidth; // Width & height are relative to the image, meaning that 1 = 100% of the image width/height
        const relativeHeight = this.boundingBox.height / this.imageHeight; // Width & height are relative to the image, meaning that 1 = 100% of the image width/height

        const formData = new FormData();
        formData.append("file", this.imageFile);
        formData.append("x", relativeX);
        formData.append("y", relativeY);
        formData.append("width", relativeWidth);
        formData.append("height", relativeHeight);

        alert(`Bounding Box Data:
        X: ${relativeX}
        Y: ${relativeY}
        Width: ${relativeWidth}
        Height: ${relativeHeight}`);

        /*
        try {
            const response = await axios.post("http://localhost:8080/api/remove-background", formData, {
                headers: {
                    "Content-Type": "multipart/form-data",
                },
            });

            // Handle response (assuming backend returns processed image URL)
            console.log("Processed Image:", response.data);
            alert("Background removed successfully!");

        } catch (error) {
            console.error("Error processing image:", error);
            alert("Failed to process image.");
        }
        */
    },

    handleFileUpload(event) {
      const file = event.target.files[0];
      if (file) {
        this.imageFile = file;
        this.imageUrl = URL.createObjectURL(file);
        this.boundingBox.visible = false; // Hide box until image loads
      }
    },

    setImageDimensions() {
      const img = this.$refs.imageRef;
      this.imageWidth = img.width;
      this.imageHeight = img.height;
      this.boundingBox.visible = true;
    },

    startDragging(event) {
      this.dragging = true;
      this.startX = event.clientX - this.boundingBox.x;
      this.startY = event.clientY - this.boundingBox.y;
      document.addEventListener("mousemove", this.dragBoundingBox);
      document.addEventListener("mouseup", this.stopDragging);
    },
    dragBoundingBox(event) {
      if (this.dragging) {
        let newX = event.clientX - this.startX;
        let newY = event.clientY - this.startY;

        newX = Math.max(0, Math.min(newX, this.imageWidth - this.boundingBox.width));
        newY = Math.max(0, Math.min(newY, this.imageHeight - this.boundingBox.height));

        this.boundingBox.x = newX;
        this.boundingBox.y = newY;
      }
    },
    stopDragging() {
      this.dragging = false;
      document.removeEventListener("mousemove", this.dragBoundingBox);
      document.removeEventListener("mouseup", this.stopDragging);
    },

    startResizing(position, event) {
      this.resizing = true;
      this.resizeDirection = position;
      this.startX = event.clientX;
      this.startY = event.clientY;
      document.addEventListener("mousemove", this.resizeBoundingBox);
      document.addEventListener("mouseup", this.stopResizing);
    },
    resizeBoundingBox(event) {
      if (!this.resizing) return;

      const dx = event.clientX - this.startX;
      const dy = event.clientY - this.startY;

      let newX = this.boundingBox.x;
      let newY = this.boundingBox.y;
      let newWidth = this.boundingBox.width;
      let newHeight = this.boundingBox.height;

      switch (this.resizeDirection) {
        case "top-left":
          newX = Math.max(0, this.boundingBox.x + dx);
          newY = Math.max(0, this.boundingBox.y + dy);
          newWidth = Math.min(this.imageWidth - newX, this.boundingBox.width - dx);
          newHeight = Math.min(this.imageHeight - newY, this.boundingBox.height - dy);
          break;
        case "top":
          newY = Math.max(0, this.boundingBox.y + dy);
          newHeight = Math.min(this.imageHeight - newY, this.boundingBox.height - dy);
          break;
        case "top-right":
          newY = Math.max(0, this.boundingBox.y + dy);
          newWidth = Math.min(this.imageWidth - this.boundingBox.x, this.boundingBox.width + dx);
          newHeight = Math.min(this.imageHeight - newY, this.boundingBox.height - dy);
          break;
        case "left":
          newX = Math.max(0, this.boundingBox.x + dx);
          newWidth = Math.min(this.imageWidth - newX, this.boundingBox.width - dx);
          break;
        case "right":
          newWidth = Math.min(this.imageWidth - this.boundingBox.x, this.boundingBox.width + dx);
          break;
        case "bottom-left":
          newX = Math.max(0, this.boundingBox.x + dx);
          newWidth = Math.min(this.imageWidth - newX, this.boundingBox.width - dx);
          newHeight = Math.min(this.imageHeight - this.boundingBox.y, this.boundingBox.height + dy);
          break;
        case "bottom":
          newHeight = Math.min(this.imageHeight - this.boundingBox.y, this.boundingBox.height + dy);
          break;
        case "bottom-right":
          newWidth = Math.min(this.imageWidth - this.boundingBox.x, this.boundingBox.width + dx);
          newHeight = Math.min(this.imageHeight - this.boundingBox.y, this.boundingBox.height + dy);
          break;
      }

      if (newWidth > 20 && newHeight > 20) {
        this.boundingBox.x = newX;
        this.boundingBox.y = newY;
        this.boundingBox.width = newWidth;
        this.boundingBox.height = newHeight;
      }

      this.startX = event.clientX;
      this.startY = event.clientY;
    },
    stopResizing() {
      this.resizing = false;
      document.removeEventListener("mousemove", this.resizeBoundingBox);
      document.removeEventListener("mouseup", this.stopResizing);
    }
  }
};
</script>

<style scoped>
.instructions {
  font-size: 16px;
  font-weight: bold;
  color: #ff5722; 
  margin-bottom: 10px;
}

.container {
  text-align: center;
  margin: 20px;
}

.image-container {
  position: relative;
  display: inline-block;
  background: rgba(0, 0, 0, 0.5); 
}


.bounding-box {
  position: absolute;
  border: 2px solid red;
  background: transparent;
  outline: 9999px solid rgba(0, 0, 0, 0.5); 
  cursor: move;
}


.resize-handle {
  width: 10px;
  height: 10px;
  background: red;
  position: absolute;
}

.top-left { top: -5px; left: -5px; cursor: nwse-resize; }
.top { top: -5px; left: 50%; transform: translateX(-50%); cursor: ns-resize; }
.top-right { top: -5px; right: -5px; cursor: nesw-resize; }
.left { left: -5px; top: 50%; transform: translateY(-50%); cursor: ew-resize; }
.right { right: -5px; top: 50%; transform: translateY(-50%); cursor: ew-resize; }
.bottom-left { bottom: -5px; left: -5px; cursor: nesw-resize; }
.bottom { bottom: -5px; left: 50%; transform: translateX(-50%); cursor: ns-resize; }
.bottom-right { bottom: -5px; right: -5px; cursor: nwse-resize; }
</style>
