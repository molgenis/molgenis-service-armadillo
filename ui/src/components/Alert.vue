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
import { BootstrapType } from "@/types/types";
import { defineComponent, PropType } from "vue";
export default defineComponent({
  name: "Alert",
  props: {
    type: String as PropType<BootstrapType>,
  },
  emits: ["clear"],
});
</script>
