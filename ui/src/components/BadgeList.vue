<template>
  <span>
    <Badge v-for="(badgeItem, index) in badgeItems" :key="badgeItem">
      {{ badgeItem }}
      <button v-if="canEdit"
              class="cancel-badge text-light bg-secondary"
              @click="remove(index)"
      >
        <i class="bi bi-x"></i>
      </button>
    </Badge>
  </span>
</template>

<script lang="ts">
import {StringArray} from "@/types/types";
import {defineComponent, PropType} from "vue";
import Badge from "@/components/Badge.vue";

export default defineComponent({
  name: "BadgeList",
  components: {Badge},
  props: {
    itemArray: {
      type: Array as PropType<StringArray>,
      required: true
    },
    canEdit: {
      type: Boolean,
      default: false
    },
  },
  data() {
    return {
      badgeItems: this.itemArray,
    };
  },
  methods: {
    remove(index: number) {
      this.badgeItems.splice(index, 1);
      this.$emit('update', this.badgeItems);
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
