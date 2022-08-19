<template>
  <div
    :class="`alert alert-${this.type} alert-dismissible fade show`"
    role="alert"
  >
    <slot></slot>
    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
  </div>
</template>
<script>
export default {
  name: "Alert",
  data() {
    return {
      show: true,
    };
  },
  methods: {
    toggleShow() {
      this.show = !this.show;
    },
  },
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
