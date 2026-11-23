<template>
  <div class="progress">
    <div
      class="progress-bar"
      :class="
        percentage == 100
          ? 'bg-success'
          : 'progress-bar-animated progress-bar-striped'
      "
      role="progressbar"
      aria-label="Progress bar"
      :aria-valuenow="String(percentage)"
      aria-valuemin="0"
      aria-valuemax="100"
      :style="'width: ' + percentage + '%'"
    ></div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "ProgressBar",
  props: {
    percentage: { type: Number, required: true },
  },
});
</script>
