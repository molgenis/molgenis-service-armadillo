<template>
  <span>
    <Badge v-for="(badgeItem, index) in badgeItems">
      {{ badgeItem }}
      <button
        class="cancel-badge text-light bg-secondary"
        @click="remove(index)"
      >
        <i class="bi bi-x"></i>
      </button>
    </Badge>
  </span>
</template>

<script lang="ts">
import { StringArray } from "@/types/types";
import { remove } from "@vue/shared";
import { defineComponent, PropType } from "vue";
import Badge from "./Badge.vue";

export default defineComponent({
  name: "BadgeList",
  components: { Badge },
  props: {
    itemArray: {
      type: Array as PropType<StringArray>, 
        required: true 
      },
    saveCallback: {
      type: Function, 
      required: true
    },
    // Some kind of row indicator
    row: {
      type: Object, 
      required: true
    },
  },
  data() {
    return {
      badgeItems: this.itemArray,
    };
  },
  methods: {
    remove(index: number) {
      this.badgeItems.splice(index);
      this.saveCallback(this.badgeItems, this.row);
    },
  },
  watch: {
    itemArray: function () {
      this.badgeItems = this.itemArray;
    },
  },
});
</script>

<style scoped>
button.cancel-badge {
  border: none;
  padding: 0;
  margin-right: -0.2em;
}
</style>
