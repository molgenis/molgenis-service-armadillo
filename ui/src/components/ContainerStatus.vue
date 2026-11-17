<template>
  <button
    class="btn p-0 mt-1 status"
    :disabled="disabled"
    :class="`text-${color}`"
  >
    <LoadingSpinner imageWidth="40" v-if="icon === 'spinner'"></LoadingSpinner>
    <h1 v-else class="p-0 m-0"><i :class="`bi bi-${icon}`"></i></h1>
    <span>{{ text }}</span>
  </button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import { BootstrapType } from "@/types/types";

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
    color: {
      type: String as PropType<BootstrapType>,
      default: "primary",
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
