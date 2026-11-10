<template>
  <span>
    <ContainerTypeLogo :name="name" :template="template" />
  </span>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ContainerTypeLogo from "@/components/ContainerTypeLogo.vue";

export default defineComponent({
  name: "Icon",
  components: { ContainerTypeLogo },
});
</script>

<style scoped></style>
