<template>
  <div>
    <ul class="nav nav-tabs">
      <li v-for="(item, index) in menu" class="nav-item" :key="index">
        <router-link :to="`/${item.toLowerCase()}`">
          <button
            :class="isSelectedPage(item) ? 'nav-link active' : 'nav-link'"
            :id="`${item.toLowerCase()}-tab`"
            data-bs-toggle="tab"
            :data-bs-target="`#${item.toLowerCase()}-tab-pane`"
            type="button"
            role="tab"
            :aria-controls="`${item.toLowerCase()}-tab-pane`"
            :aria-selected="isSelectedPage(item) ? 'true' : 'false'"
          >
            <i v-if="index < icons.length" :class="`bi bi-${icons[index]}`"></i>
            {{ item }}
          </button>
        </router-link>
      </li>
    </ul>
    <div class="tab-content">
      <router-view />
    </div>
  </div>
</template>

<script lang="ts">
import { StringArray } from "@/types/types";
import { defineComponent, PropType } from "vue";
import RouterLink from "vue-router";
import RouterView from "vue-router";

export default defineComponent({
  name: "Tabs",
  props: {
    menu: { type: Array as PropType<StringArray>, required: true },
    icons: { type: Array as PropType<StringArray>, required: true },
  },
  methods: {
    isSelectedPage(page: string) {
      return page.toLowerCase() === this.selectedPage;
    },
  },
  computed: {
    selectedPage() {
      return this.$route.fullPath.split('/')[1];
    },
  },
});
</script>
