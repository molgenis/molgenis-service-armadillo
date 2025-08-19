<template>
  <div>
    <div class="row text-center">
      <div v-if="numberOfLines > -1" class="text-secondary fst-italic">
        <span>{{ currentFocus + 1 }} / {{ numberOfLines }}</span>
      </div>
      <div v-else-if="filterValue != ''" class="text-secondary fst-italic">
        <span>No search results</span>
      </div>
      <div v-else>
        <div class="text-white">-</div>
      </div>
    </div>
    <div class="row">
      <div class="col ps-1">
        <div
          class="btn-group"
          role="group"
          aria-label="navigation"
          v-if="true || (filterValue && matchedLines.length > 0)"
        >
          <button
            type="button"
            :disabled="numberOfLines < 1 || currentFocus === 0"
            class="btn btn-primary"
            @click="navigationFunction('first')"
          >
            <i class="bi bi-skip-backward-fill"></i>
          </button>
          <button
            type="button"
            :disabled="numberOfLines < 1 || currentFocus === 0"
            class="btn btn-primary"
            @click="navigationFunction('prev')"
          >
            <i class="bi bi-skip-start-fill"></i>
          </button>
          <button
            type="button"
            :disabled="numberOfLines < 1 || currentFocus === numberOfLines - 1"
            class="btn btn-primary"
            @click="navigationFunction('next')"
          >
            <i class="bi bi-skip-end-fill"></i>
          </button>
          <button
            type="button"
            :disabled="numberOfLines < 1 || currentFocus === numberOfLines - 1"
            class="btn btn-primary"
            @click="navigationFunction('last')"
          >
            <i class="bi bi-skip-forward-fill"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "SearchNavigation",
  emits: ["switched"],
  props: {
    numberOfLines: {
      type: Number,
      required: true,
    },
    currentFocus: {
      type: Number,
      required: true,
    },
    filterValue: {
      type: String,
      required: true,
    },
    matchedLines: {
      type: Array,
      required: true,
    },
    navigationFunction: {
      type: Function,
      required: true,
    },
  },
  data() {
    return {};
  },
});
</script>
