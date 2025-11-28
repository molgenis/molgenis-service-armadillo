<template>
  <button class="btn btn-link p-0 mt-1 status btn-sm" :disabled="disabled">
    <LoadingSpinner imageWidth="30" v-if="icon === 'spinner'"></LoadingSpinner>
    <h3 v-else class="p-0 m-0"><i :class="`bi bi-${icon}`"></i></h3>
    <span>{{ text }}</span>
  </button>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";

export default defineComponent({
  name: "ContainerStatus",
  components: {
    LoadingSpinner,
  },
  props: {
    icon: {
      type: String,
      required: true,
    },
    text: {
      type: String,
      required: true,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
});
</script>

<style scoped>
.status {
  width: 6rem;
}
</style>
