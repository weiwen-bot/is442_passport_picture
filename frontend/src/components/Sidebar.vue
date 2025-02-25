<template>
  <div class="sidebar">
    <h3>Modify Image</h3>
    <button @click="handleAction('resize')">Resize</button>
    <button @click="handleAction('crop')">Crop</button>
    <button @click="handleAction('backgroundremover')">Background Remover</button>

  </div>
  <div class="main-content">
    <ImageCropping v-if="currentAction === 'crop'" />
  </div>
</template>

<script>
import ImageCropping from './ImageCropping.vue';

export default {
  components: {
    ImageCropping,
  },
  data() {
    return {
      currentAction: "", // Track current action
    };
  },
  methods: {
    handleAction(action) {
      this.currentAction = action;
    },
  },
};
</script>

<style>
/* Sidebar Styling */
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: 220px;
  height: 100%;
  background-color: white;
  border-right: 2px solid #ddd;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar h3 {
  font-size: 18px;
  color: #007bff;
  margin-bottom: 10px;
}

.sidebar button {
  padding: 12px;
  background-color: white;
  color: #007bff;
  border: 1px solid #007bff;
  cursor: pointer;
  border-radius: 6px;
  transition: 0.3s;
  text-align: left;
}

.sidebar button:hover {
  background-color: #007bff;
  color: white;
}

/* Main Content Styling */
.main-content {
  margin-left: 240px;
  padding: 20px;
}
</style>
