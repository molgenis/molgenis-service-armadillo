<template>
  <div
    :class="isValidated && validationCondition ? 'invalid-field' : ''"
    class="p-1"
  >
    <slot></slot>
    <div v-if="isValidated && validationCondition" class="feedback">
      {{ invalidMessage }}
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "FormValidation",
  props: {
    isValidated: {
      type: Boolean,
      default: false,
    },
    validationCondition: {
      type: Boolean,
      required: true,
    },
    invalidMessage: {
      type: String,
      required: true,
    },
  },
});
</script>

<style scoped>
.invalid-field {
  border: 1px;
  border-color: rgb(220, 53, 69);
  border-style: solid;
  border-radius: 10px;
}

.feedback {
  color: rgb(220, 53, 69);
  font-style: italic;
  font-size: 0.9rem;
}
</style>
