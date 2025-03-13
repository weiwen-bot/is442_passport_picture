<template>
  <div>
    <SidebarMenu
      :menu="menu"
      @click.native="handleSelect"
      :width="sidebarWidth"
      :hide-toggle="true"
      :collapsed="false"
    >
    <template v-slot:footer>
      <button class="reset-button" @click="resetData">Reset</button>
    </template>

    </SidebarMenu>

    <!-- :hide-toggle="true": Hide the collapse button -->
    <!-- :collapsed="false": Ensure it's not in collapsed state -->
  </div>
</template>

<script>
import { SidebarMenu } from "vue-sidebar-menu";
import "vue-sidebar-menu/dist/vue-sidebar-menu.css";

export default {
  components: { SidebarMenu },
  props: {
    sidebarWidth: {
      type: String,
      default: "250px",
    },
  },
  data() {
    return {
      menu: [
        { header: "Main Navigation", hiddenOnCollapse: true },
        { title: "Crop", icon: "fa-solid fa-crop", action: "crop" },
        { title: "Resize", icon: "fa-solid fa-expand", action: "resize" },
        {
          title: "Background Remover",
          icon: "fa-solid fa-eraser",
          action: "background-remover",
        },
        { 
          title: "Enhance", 
          icon: "fa-solid fa-wand-magic-sparkles", 
          action: "enhance" 
        },
      ],
    };
  },
  methods: {
    handleSelect(event) {
      const selectedItem = this.menu.find(
        (item) => item.title === event.target.innerText
      );
      if (selectedItem && selectedItem.action) {
        this.$emit("update-action", selectedItem.action); // Emit action to parent component
        console.log("Emitting action:", selectedItem.action);
      }
    },
    resetData() {
      this.$emit("reset-data");
    },
  },
};
</script>

<style scoped>
::v-deep(.vsm--title) {
  font-size: 15px; /* Adjust the font size */
}

.reset-button {
  margin: 11px 20px; 
  padding: 10px 20px; 
  color: white; 
  border: none; 
  border-radius: 5px; 
  cursor: pointer; 
  font-size: 14px; 
  transition: background-color 0.3s ease; 
}

.reset-button:hover {
  background-color: #b91d1d; 
}
</style>
