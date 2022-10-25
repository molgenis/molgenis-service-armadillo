<template>
  <div class="btn-group" role="group">
    <button
      type="button"
      v-for="(icon, index) in buttonIcons"
      class="btn btn-sm"
      :class="`btn-${getButtonColor(index)} bg-${buttonColors[index]}`"
      :disabled="disabledButtons ? disabledButtons[index] : false"
      @click="clickCallbacks[index](getCallbackArgument(index))"
    >
      <i :class="`bi bi-${icon}`"></i>
    </button>
  </div>
</template>

<script lang="ts">
import { BootstrapType, StringArray } from "@/types/types";
import { defineComponent, PropType } from "vue";

export default defineComponent({
  name: "ButtonGroup",
  props: {
    buttonIcons: {
      type: Array as PropType<StringArray>,
      required: true,
    },
    buttonColors: {
      type: Array as PropType<BootstrapType[]>,
      required: true,
    },
    disabledButtons: {
      type: Array as PropType<boolean[]>
    },
    clickCallbacks: {
      type: Array as PropType<Function[]>, 
      required: true
    },
    callbackArguments: {
      type: Array, 
      required: false
    },
  },
  methods: {
    getButtonColor(index: number): BootstrapType {
      // info button has dark icon, which is ugly with the light primary/success/danger
      return this.buttonColors[index] === "info"
        ? "primary"
        : this.buttonColors[index];
    },
    getCallbackArgument(index: number) {
      // if callback functions don't need arguments, we don't want to specify them
      // if they have arguments, the list of arguments should be as long as all other lists
      if (this.callbackArguments && this.callbackArguments.length > 0) {
        return this.callbackArguments[index];
      } else {
        return undefined;
      }
    },
  },
});
</script>

<style scoped>
.btn.btn-sm.btn-primary.bg-info {
  border-color: rgba(var(--bs-info-rgb), var(--bs-bg-opacity)) !important;
}
</style>
