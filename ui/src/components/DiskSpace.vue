<template>
  <div>
    <h5><i class="bi bi-hdd"></i> Disk</h5>
    <div class="progress">
      <div
        class="progress-bar bg-success"
        role="progressbar"
        aria-label="Success striped example"
        :style="'width:' + percentage + '%'"
        :aria-valuenow="percentage.toString()"
        aria-valuemin="0"
        aria-valuemax="100"
      ></div>
    </div>
    Used: {{ used }} / {{ total }}
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "DiskSpace",
  props: {
    used: { type: String, required: true },
    total: { type: String, required: true },
    percentage: { type: Number, required: true },
  },
});
</script>
