<template>
    <div class="sidebar">
      <button @click="handleAction('crop')">Crop</button>
    </div>
  </template>
  
  <script>
  export default {
    emits: ["actionSelected"], // Emit event to parent when a button is clicked
    methods: {
      handleAction(action) {
        this.$emit("actionSelected", action);
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
  </style>
  