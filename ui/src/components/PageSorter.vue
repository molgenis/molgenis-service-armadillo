<template>
  <div class="col-2 offset-1">
    <div class="row">
      <div class="col">Sort page on:</div>
      <div class="col-3">
        <i v-if="sortType == 'timeDesc'" class="bi bi-sort-numeric-up-alt"></i>
        <i
          v-else-if="sortType == 'timeAsc'"
          class="bi bi-sort-numeric-down-alt"
        ></i>
      </div>
    </div>
    <select
      v-model="sortOn"
      @change="$emit('selectChanged')"
      class="form-select form-select-sm mt-1"
    >
      <option value="timeDesc">Time (new -> old)</option>
      <option value="timeAsc">Time (old -> new)</option>
    </select>
  </div>
</template>
<script lang="ts">
export default {
  name: "PageSorter",
  props: {
    sortType: {
      type: String,
      required: true,
    },
  },
  emits: ["selectChanged"],
  data() {
    return {
      sortOn: this.sortType,
    };
  },
};
</script>
