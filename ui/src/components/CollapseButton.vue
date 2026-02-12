<template>
  <button
    class="btn-sm ms-0 btn btn-secondary me-2"
    @click="isCollapsed = !isCollapsed"
  >
    <i class="bi bi-chevron-down" v-if="isCollapsed"></i>
    <i class="bi bi-chevron-up" v-else></i>
  </button>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "CollapseButton",
  emits: ["update"],
  data() {
    return {
      isCollapsed: true,
    };
  },
  watch: {
    isCollapsed() {
      console.log("update!");
      this.$emit("update", this.isCollapsed);
    },
  },
});
</script>
