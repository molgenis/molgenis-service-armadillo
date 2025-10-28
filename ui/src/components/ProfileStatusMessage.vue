<template>
  <div
    v-if="visible && props.status?.globalStatus === 'Installing'"
    class="alert alert-info mt-2"
    style="display: flex; align-items: center"
  >
    {{ props.status?.globalStatus }}&nbsp;
    <span v-if="props.status && props.status.completedLayers !== null">
      layer {{ props.status.completedLayers }}/{{
        props.status.totalLayers
      }}
      ({{ props.status?.totalPercent }}%)
    </span>
    <span v-if="props.status?.layerStatus && props.status?.layerPercent">
      &nbsp;| {{ props.status?.layerStatus }}
    </span>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import type { ProfileStartStatus } from "@/types/api";
import LoadingSpinner from "@/components/LoadingSpinner.vue";

const props = withDefaults(
  defineProps<{
    status: ProfileStartStatus | null;
    timeout?: number;
  }>(),
  { timeout: 2000 }
);

const visible = ref(false);
const shownComplete = ref(false);
const hideTimer = ref<number | null>(null);

watch(
  () => props.status,
  (newVal) => {
    // suppress repeated 100% without touching the hide timer
    if (newVal && newVal.totalPercent === 100 && shownComplete.value) return;

    // clear pending timer for other updates
    if (hideTimer.value !== null) {
      clearTimeout(hideTimer.value);
      hideTimer.value = null;
    }

    if (newVal) {
      if (newVal.totalPercent < 100) {
        // in progress: keep visible continuously
        visible.value = true;
        shownComplete.value = false; // reset for next cycle
        return;
      }
      // first time at 100%: show then hide after timeout
      visible.value = true;
      hideTimer.value = window.setTimeout(() => {
        visible.value = false;
        hideTimer.value = null;
      }, props.timeout);
      shownComplete.value = true;
    } else {
      visible.value = false;
      shownComplete.value = false;
    }
  }
);
</script>
