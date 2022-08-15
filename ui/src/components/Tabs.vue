<template>
  <div>
    <ul class="nav nav-tabs">
      <li v-for="(item, index) in menu" class="nav-item">
        <button
          :class="index == this.activeTab ? 'nav-link active' : 'nav-link'"
          :id="`${item}-tab`"
          data-bs-toggle="tab"
          :data-bs-target="`#${item}-tab-pane`"
          type="button"
          role="tab"
          :aria-controls="`${item}-tab-pane`"
          :aria-selected="index == this.activeTab ? 'true' : 'false'"
          @click="this.triggerActiveTabChange(index)"
        >
          <i v-if="index < this.icons.length" :class="`bi bi-${this.icons[index]}`"></i>
          {{ item }}
        </button>
      </li>
    </ul>
    <div class="tab-content">
      <slot></slot>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    menu: Array,
    icons: Array,
    activeTab: Number,
  },
  methods: {
    triggerActiveTabChange(index) {
      this.$emit("activeTabChange", index);
    },
  },
};
</script>
