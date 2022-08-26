<template>
  <div class="btn-group" role="group">
    <button
      type="button"
      v-for="(icon, index) in buttonIcons"
      class="btn btn-sm"
      :class="`btn-${this.buttonColors[index]} bg-${this.buttonColors[index]}`"
      @click="this.clickCallbacks[index](this.getCallbackArgument(index))"
    >
      <i :class="`bi bi-${icon}`"></i>
    </button>
  </div>
</template>

<script>
export default {
  name: "ButtonGroup",
  props: {
    buttonIcons: Array,
    buttonColors: Array,
    clickCallbacks: Array,
    callbackArguments: Array
  },
  methods: {
    getCallbackArgument(index) {
      // if callback functions don't need arguments, we don't want to specify them
      // if they have arguments, the list of arguments should be as long as all other lists
      if (this.callbackArguments && this.callbackArguments.length > 0 ) {
        return this.callbackArguments[index];
      } else {
        return undefined;
      }
    }
  }
};
</script>
