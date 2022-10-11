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
          <router-link :to="`/${item.toLowerCase()}`">
            <i v-if="index < icons.length" :class="`bi bi-${icons[index]}`"></i>
            {{ item }}
          </router-link>
        </button>
      </li>
    </ul>
    <div class="tab-content">
      <router-view />
    </div>
  </div>
</template>

<script lang="ts">
import { StringArray } from "@/types/types";
import { PropType } from "vue";

export default {
  name: "Tabs",
  props: {
    menu: { type: Array as PropType<StringArray>, required: true },
    icons: { type: Array as PropType<StringArray>, required: true },
    activeTab: { type: Number, required: true },
  },
};
</script>
