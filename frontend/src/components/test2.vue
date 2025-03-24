<template>
  <div class="test2-page">
    <h2>Upload a face image</h2>
    <input type="file" @change="handleFile" accept="image/*" />
    <button @click="uploadImage" :disabled="!selectedFile">Upload</button>

    <div v-if="imageUrl" class="preview">
      <h3>Result:</h3>
      <img :src="imageUrl" alt="Centered Face" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const selectedFile = ref(null)
const imageUrl = ref(null)

function handleFile(event) {
  selectedFile.value = event.target.files[0]
}

async function uploadImage() {
  const formData = new FormData()
  formData.append('image', selectedFile.value)

  try {
    const response = await fetch("http://localhost:8080/api/face/center", {
      method: 'POST',
      body: formData
    })

    if (!response.ok) {
      throw new Error('Failed to process image')
    }

    const blob = await response.blob()
    imageUrl.value = URL.createObjectURL(blob)
  } catch (error) {
    alert(error.message)
  }
}
</script>

<style scoped>
.test2-page {
  max-width: 600px;
  margin: 2rem auto;
  text-align: center;
}
input[type="file"] {
  margin-bottom: 1rem;
}
button {
  padding: 0.5rem 1rem;
  margin-bottom: 1rem;
}
.preview img {
  max-width: 100%;
  border: 1px solid #ccc;
  margin-top: 1rem;
}
</style>
