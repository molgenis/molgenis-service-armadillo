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
<script>
export default {
  name: "Alert",
  props: {
    clearFunction: Function,
    type:
      "primary" |
      "secondary" |
      "success" |
      "warning" |
      "info" |
      "danger" |
      "light" |
      "dark",
  },
};
</script>
