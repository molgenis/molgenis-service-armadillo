<template>
  <div>
    <button
      type="button"
      class="btn btn-secondary"
      @click="toggleColumnNames()"
    >
      {{ buttonName }} <i class="bi bi-chevron-down" v-if="!isCollapsed"></i>
      <i class="bi bi-chevron-up" v-else></i>
    </button>
    <div class="fst-italic text-secondary" v-show="isCollapsed">
      {{ columnNamesString }}
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "ColumnNamesPreview",
  props: {
    columnNames: { type: Array, required: true },
    buttonName: String,
  },
  data() {
    return {
      isCollapsed: false,
    };
  },
  computed: {
    columnNamesString() {
      return this.columnNames.join(", ");
    },
  },
  methods: {
    toggleColumnNames() {
      this.isCollapsed = !this.isCollapsed;
    },
  },
});
</script>
