<template>
  <div>
    <div class="row text-center">
      <div class="fst-italic" :class="textValueColour">
        <span v-if="currentValue === ''" class="text-white">-</span>
        <span v-else>{{ currentValue }}</span>
      </div>
    </div>
    <div class="row">
      <div class="col ps-1">
        <div
          class="btn-group"
          :class="isSmall ? 'btn-group-sm' : ''"
          role="group"
          aria-label="navigation"
        >
          <button
            v-for="buttonId in buttons"
            type="button"
            class="btn btn-primary"
            :disabled="disabled[buttonId]"
            @click="functions[buttonId]()"
          >
            <i :class="`bi bi-${icons[buttonId]}`"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";

export default defineComponent({
  name: "NavigationButtons",
  props: {
    currentValue: {
      type: String,
      required: true,
    },
    icons: {
      type: Object as PropType<{
        prev: String;
        next: String;
        first: String;
        last: String;
      }>,
      required: true,
    },
    functions: {
      type: Object as PropType<{
        prev: Function;
        next: Function;
        first: Function;
        last: Function;
      }>,
      required: true,
    },
    disabled: {
      type: Object as PropType<{
        prev: boolean;
        next: boolean;
        first: boolean;
        last: boolean;
      }>,
      required: true,
    },
    isSmall: {
      type: Boolean,
      default: false,
    },
    textValueColour: {
      type: String,
      default: "",
    },
  },
  data(): {
    buttons: ["first", "prev", "next", "last"];
  } {
    return {
      buttons: ["first", "prev", "next", "last"],
    };
  },
});
</script>
