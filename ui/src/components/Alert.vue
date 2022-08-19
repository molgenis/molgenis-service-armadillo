<template>
  <div :class="`alert alert-${this.type} alert-dismissible`" role="alert">
    <slot></slot>
  </div>
</template>
<script>
export default {
  name: "Alert",
  props: {
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
