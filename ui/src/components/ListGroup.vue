<template>
  <ul class="list-group">
    <li
      class="list-group-item"
      v-for="key in listContent"
      :class="
        key == selectedItem ? `text-bg-${selectionColor}` : 'link-primary'
      "
      :key="key"
      @click="toggleSelectedItem(key)"
    >
      <a>
        <i
          v-if="altIconCondition(key) && rowIconAlt"
          :class="`bi bi-${rowIconAlt}`"
        ></i>
        <i v-else :class="`bi bi-${rowIcon}`"></i>
        {{ handleListGroupItemDisplayKey(key) }}
        <i class="bi bi-chevron-right float-end" v-if="key == selectedItem"></i>
      </a>
    </li>
  </ul>
</template>

<style scoped>
.list-group {
  border-radius: 0;
}
</style>

<script lang="ts">
import { BootstrapType, StringArray } from "@/types/types";
import { shortenFileName } from "@/helpers/utils";
import { PropType } from "vue";

export default {
  name: "ListGroup",
  props: {
    listContent: { type: Array as PropType<StringArray>, required: true },
    rowIcon: { type: String, required: true },
    rowIconAlt: { type: String },
    preselectedItem: { type: String, default: "" },
    altIconCondition: {
      type: Function,
      default: () => {
        return false;
      },
    },
    selectionColor: { type: String as PropType<BootstrapType> },
  },
  data() {
    return { selectedItem: this.preselectedItem };
  },
  watch: {
    listContent: function () {
      this.selectedItem = "";
    },
    preselectedItem: function () {
      this.selectedItem = this.preselectedItem;
    },
  },
  methods: {
    toggleSelectedItem(newItem: string) {
      if (this.selectedItem !== newItem) {
        this.selectedItem = newItem;
      } else {
        this.selectedItem = "";
      }
    },
    handleListGroupItemDisplayKey(key: string): string {
      // Check if file
      if (this.rowIcon == "table") {
        return shortenFileName(key);
      } else {
        return key;
      }
    },
  },
};
</script>
