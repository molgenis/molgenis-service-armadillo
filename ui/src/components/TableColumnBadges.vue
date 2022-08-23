<template>
  <span>
    <Badge v-for="(badgeItem, index) in badgeItems">
      {{ badgeItem }}
      <button
        class="cancel-badge text-light bg-secondary"
        @click="this.remove(index)"
      >
        <i class="bi bi-x"></i>
      </button>
    </Badge>
  </span>
</template>

<script>
import Badge from "./Badge.vue";

export default {
  name: "TableColumnBadges",
  components: { Badge },
  props: {
    data: Array,
    saveCallback: Function,
    // Some kind of row indicator
    row: Object,
  },
  data() {
    return {
      badgeItems: this.data,
    };
  },
  methods: {
    remove(index) {
      this.badgeItems.splice(index);
      this.saveCallback(this.badgeItems, this.row);
    },
  },
};
</script>

<style scoped>
button.cancel-badge {
  border: none;
  padding: 0;
  margin-right: -0.2em;
}
</style>
