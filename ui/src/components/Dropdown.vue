<template>
  <select class="form-select" v-model="selectedOption">
    <option :value="option" v-for="option in options" :key="option">
      {{ option }}
    </option>
  </select>
</template>

<script lang="ts">
import { StringArray } from "@/types/types";
import { defineComponent, PropType } from "vue";

export default defineComponent({
  name: "Dropdown",
  props: {
    options: {
      default: [],
      type: Array as PropType<StringArray>,
    },
  },
  emits: ["update"],
  watch: {
    selectedOption() {
      this.$emit("update", this.selectedOption);
    },
  },
  data() {
    return {
      selectedOption: "",
    };
  },
});
</script>
