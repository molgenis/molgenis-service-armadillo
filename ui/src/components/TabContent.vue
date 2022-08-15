<template>
  <div
    :class="
      this.isActive
        ? 'tab-pane fade show active'
        : 'tab-pane fade'
    "
    id="users-tab-pane"
    role="tabpanel"
    :aria-labelledby="`${menuItem}-tab`"
    :tabindex="this.menuIndex"
  >
    <slot></slot>
  </div>
</template>

<script>
export default {
  props: {
    menuItem: String,
    menuIndex: Number,
    isActive: Boolean,
  },
};
</script>
