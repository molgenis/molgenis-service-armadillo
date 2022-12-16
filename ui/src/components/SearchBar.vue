<template>
  <div class="form-group has-search">
    <i class="bi bi-search form-control-feedback"></i>
    <input
      type="text"
      class="form-control"
      placeholder="Search"
      :value="modelValue"
      @input="$emit('update:modelValue', getValue($event))"
    />
  </div>
</template>

<script lang="ts">
import { getEventValue } from "@/helpers/utils";

export default {
  name: "SearchBar",
  props: { modelValue: String },
  methods: {
    // not excactly sure why, but calling this method directly won't work
    getValue(event: Event) {
      return getEventValue(event);
    },
  },
};
</script>

<style scoped>
.has-search .form-control {
  padding-left: 2.375rem;
}

.has-search .form-control-feedback {
  position: absolute;
  z-index: 2;
  display: block;
  width: 2.375rem;
  height: 2.375rem;
  line-height: 2.375rem;
  text-align: center;
  pointer-events: none;
  color: #aaa;
}
</style>
