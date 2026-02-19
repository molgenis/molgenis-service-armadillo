<template>
  <div class="row mb-1">
    <div class="col-4 col-md-4">
      <!-- alternative icon -->
      <slot name="icon"></slot>
      <i v-if="icon" :class="`bi bi-${icon}`"></i>
      <b class="ms-4">{{ title }}</b>
    </div>

    <div class="col">
      <span v-if="value">{{ value }}</span>
      <slot name="value" v-else></slot>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "ExpandedContainerProperty",
  props: {
    icon: { type: String, required: false },
    title: { type: String, required: true },
    value: { type: String },
  },
});
</script>

<style scoped></style>
