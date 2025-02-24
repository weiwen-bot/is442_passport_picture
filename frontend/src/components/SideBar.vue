<template>
  <div class="sidebar">
    <button @click="handleAction('crop')">Crop</button>
  </div>
  <!-- Main content area (image and inputs) -->
  <div class="main-content">
    <!-- Conditionally render the cropping component when "crop" action is selected -->
    <ImageCropping v-if="currentAction === 'crop'" />
  </div>
</template>

<script>
import ImageCropping from './ImageCropping.vue'; // Import the cropping component

export default {
  components: {
    ImageCropping, // Register the ImageCropping component
  },
  data() {
    return {
      currentAction: "", // Track the current action (crop or other)
    };
  },
  methods: {
    handleAction(action) {
      this.currentAction = action; // Set the action to "crop" when the button is clicked
    },
  },
};
</script>

<style>
  /* Set up the sidebar */
  .sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: 200px;
  height: 100%;
  background-color: #f4f4f4;
  padding: 20px;
  display: flex;
  flex-direction: column;
  }

  .sidebar button {
  margin-bottom: 10px;
  padding: 10px;
  background-color: #007bff;
  color: white;
  border: none;
  cursor: pointer;
  }

  .sidebar button:hover {
  background-color: #0056b3;
  }

  /* Main content area (image and inputs) */
  .main-content {
    margin-left: 220px; /* Add margin to push content to the right of the sidebar */
    padding: 20px;
  }

</style>