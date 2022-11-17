<template>
  <div :class="`alert alert-${type} alert-dismissible`" role="alert">
    <i class="bi bi-info-circle-fill" v-if="type==='primary'"></i>&nbsp;
    <i class="bi bi-check-circle-fill" v-else-if="type==='success'"></i>&nbsp;
    <i class="bi bi-exclamation-triangle-fill" v-else-if="type==='danger' || type==='warning'"></i>&nbsp;
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
