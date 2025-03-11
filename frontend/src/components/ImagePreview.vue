<template>
    <div v-if="image" class="mt-4">
    <img :src="image" alt="Uploaded Image" class="w-64 h-64 object-cover rounded shadow" />
  </div>

</template>

<script>
export default {
    props: {
    image: String
  }
}


</script>