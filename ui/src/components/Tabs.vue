<template>
  <div>
    <ul class="nav nav-tabs">
      <li v-for="(item, index) in menu" class="nav-item">
        <button
          :class="index == activeTab ? 'nav-link active' : 'nav-link'"
          :id="`${item}-tab`"
          data-bs-toggle="tab"
          :data-bs-target="`#${item}-tab-pane`"
          type="button"
          role="tab"
          :aria-controls="`${item}-tab-pane`"
          :aria-selected="index == activeTab ? 'true' : 'false'"
          v-on:click="$emit('activeTabChange', index)"
        >
          <i v-if="index < icons.length" :class="`bi bi-${icons[index]}`"></i>
          {{ item }}
        </button>
      </li>
    </ul>
    <div class="tab-content">
      <slot></slot>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  name: 'Tabs',
  props: {
    menu: Array,
    icons: Array,
    activeTab: Number,
  },
};
</script>
