<template>
  <div>
    <SidebarMenu 
      :menu="menu" 
      @click.native="handleSelect" 
      :width="sidebarWidth" 
      :hide-toggle="true" 
      :collapsed="false"
    />

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
      default: '250px',
    },
  },
  data() {
    return {
      menu: [
        { header: "Main Navigation", hiddenOnCollapse: true },
        { title: "Crop", icon: "fa-solid fa-crop", action: "crop" },
        { title: "Background Remover", icon: "fa-solid fa-eraser", action: "background-remover" },
      ],
    };
  },
  methods: {
    handleSelect(event) {
      const selectedItem = this.menu.find(item => item.title === event.target.innerText);
      if (selectedItem && selectedItem.action) {
        this.$emit("update-action", selectedItem.action); // Emit action to parent component
        console.log("Emitting action:", selectedItem.action);
      }
    },
  },
};
</script>

<style scoped>
::v-deep(.vsm--title) {
  font-size: 15px; /* Adjust the font size */
}
</style>