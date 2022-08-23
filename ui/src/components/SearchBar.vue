<template>
  <div class="form-group has-search">
    <i class="bi bi-search form-control-feedback"></i>
    <input
      type="text"
      class="form-control"
      placeholder="Search"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  </div>
</template>

<script>
export default {
  name: "SearchBar",
  props: { modelValue: String },
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
