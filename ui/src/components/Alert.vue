<template>
  <div :class="`alert alert-${type} alert-dismissible`" role="alert">
    <slot></slot>
    <button
      v-on:click="$emit('clear')"
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
    type: String,
    // TODO create enum with: "primary" | "secondary" | "success" |"warning" | "info" | "danger" |"light" | "dark"
  },
  emits: ["clear"],
});
</script>
