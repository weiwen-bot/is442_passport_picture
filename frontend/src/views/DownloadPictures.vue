<template>
  <div class="container">
    <h2>Download Pictures</h2>

    <div v-for="(image, index) in images" :key="index" class="image-card">
      <img :src="image.url" :alt="image.name" class="preview" />
      <p>{{ image.name }}</p>

      <!-- Download Button -->
      <button @click="downloadImage(image)">Download</button>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      images: [
        { name: "ID_Photo_1.jpg", url: "https://placehold.co/200" },
        { name: "ID_Photo_2.jpg", url: "https://placehold.co/200" },
        { name: "ID_Photo_3.jpg", url: "https://placehold.co/200" }
      ]
    };
  },
  methods: {
    // Download Image - Opens "Save As" Dialog
    async downloadImage(image) {
      try {
        const response = await fetch(image.url);
        const blob = await response.blob();

        if (window.showSaveFilePicker) {
          const fileHandle = await window.showSaveFilePicker({
            suggestedName: image.name,
            types: [{ description: "Images", accept: { "image/*": [".jpg", ".png"] } }]
          });

          const writable = await fileHandle.createWritable();
          await writable.write(blob);
          await writable.close();
        } else {
          const link = document.createElement("a");
          link.href = URL.createObjectURL(blob);
          link.download = image.name;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
        }
      } catch (error) {
        console.error("Error downloading the image:", error);
      }
    },
  }
};
</script>

<style>
.container {
  text-align: center;
  padding: 20px;
}
.image-card {
  display: inline-block;
  margin: 10px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
}
.preview {
  width: 200px;
  height: auto;
  display: block;
  margin: 0 auto 10px;
}
button {
  padding: 5px 10px;
  border: none;
  margin: 5px;
  background-color: #007bff;
  color: white;
  cursor: pointer;
}
button:hover {
  background-color: #0056b3;
}
</style>
