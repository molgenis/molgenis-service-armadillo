<template>
  <ul class="list-group">
    <li
      class="list-group-item"
      v-for="key in listContent"
      :class="
        key == selectedItem ? `text-bg-${selectionColor}` : 'link-primary'
      "
      @click="toggleSelectedItem(key)"
    >
      <a>
        <i
          v-if="altIconCondition(key) && rowIconAlt"
          :class="`bi bi-${rowIconAlt}`"
        ></i>
        <i v-else :class="`bi bi-${rowIcon}`"></i> {{ key }}
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
import { PropType, Ref, ref } from "vue";

export default {
  name: "ListGroup",
  setup() {
    const selectedItem: Ref<string> = ref("");
    return {
      selectedItem,
    };
  },
  props: {
    listContent: { type: Array as PropType<StringArray>, required: true },
    rowIcon: { type: String, required: true },
    rowIconAlt: { type: String },
    altIconCondition: {
      type: Function,
      default: () => {
        return false;
      },
    },
    selectionColor: { type: String as PropType<BootstrapType> },
  },
  methods: {
    toggleSelectedItem(newItem: string) {
      if(this.selectedItem !==  newItem) {
        this.selectedItem = newItem;
      } else {
        this.selectedItem = '';
      }
    },
  },
};
</script>
