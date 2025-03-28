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
        <button class="bg-red-800 text-white px-4 py-2 m-2 rounded text-center items-center" @click="resetData">
          <i class="fa-solid fa-upload mr-2"></i> Upload New Image
        </button>
      </template>
    </SidebarMenu>
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
        { header: "Instant Passport Photo", hiddenOnCollapse: true },
        { title: "Quick Generate", icon: "fa-solid fa-bolt", action: "quick-generate" },
        { header: "Edit Photo", hiddenOnCollapse: true },
        { title: "Crop", icon: "fa-solid fa-crop", action: "crop" },
        { title: "Resize", icon: "fa-solid fa-expand", action: "resize" },
        { title: "Background Remover", icon: "fa-solid fa-eraser", action: "background-remover" },
        { title: "Enhance Photo", icon: "fa-solid fa-wand-magic-sparkles", action: "enhance" },
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
</style>
