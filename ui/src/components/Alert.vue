<template>
  <div :class="`alert alert-${this.type} alert-dismissible`" role="alert">
    <slot></slot>
    <button
      @click="this.clearFunction"
      type="button"
      class="btn-close"
      aria-label="Close"
    ></button>
  </div>
</template>
<script lang="ts">
import { defineComponent } from "vue";
export default defineComponent({
  name: "Alert",
  props: {
    clearFunction: Function,
    type: String
    // TODO create enum with: "primary" | "secondary" | "success" |"warning" | "info" | "danger" |"light" | "dark"
  },
});
</script>
