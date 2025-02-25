<script>
import HelloWorld from './components/HelloWorld.vue'
import { RouterLink, RouterView } from 'vue-router'
import BackgroundRemover from "./components/BackgroundRemover.vue"

export default { 
  name: "App",

  components:{

    // Dashboard,
    BackgroundRemover
  },
  methods:{

  }
};
</script>

<template>
  <div id="app" class="">
    
    
    <!-- Content -->
    <!-- <div class="col-lg-10 offset-lg-2 col-12 px-0"> -->
    <router-view/>
    <div>DDDDDDD</div>
    <BackgroundRemover />
    <!-- </div> -->
  </div>
</template>


<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}
</style>
