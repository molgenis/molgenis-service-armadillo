<template>
  <div
    v-if="visible && current?.globalStatus === 'Installing profile'"
    class="alert alert-info mt-2"
    style="display: flex; align-items: center; gap: 0.5rem"
  >
    {{ current?.globalStatus }} '{{ props.profileName }}'

    <div
      class="progress flex-grow-1"
      role="progressbar"
      aria-label="Progress"
      aria-valuemin="0"
      aria-valuemax="100"
      style="height: 1rem"
    >
      <div
        class="progress-bar"
        role="progressbar"
        :aria-valuenow="current?.totalPercent ?? 0"
        :style="{ width: (current?.totalPercent ?? 0) + '%' }"
      >
        {{ current?.totalPercent ?? 0 }}%
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, unref, computed, type Ref } from "vue";
import type { ProfileStartStatus } from "@/types/api";

const props = withDefaults(
  defineProps<{
    // Accept either a plain object or a Ref to one
    status: ProfileStartStatus | null | Ref<ProfileStartStatus | null>;
    timeout?: number;
    profileName: string;
  }>(),
  { timeout: 2000 }
);

// Always work with a plain value
const current = computed<ProfileStartStatus | null>(() => unref(props.status));

const visible = ref(false);
const shownComplete = ref(false);
const hideTimer = ref<number | null>(null);

watch(
  () => current.value,
  (status) => {
    console.log("Child status update:", status);

    if (status && status.totalPercent === 100 && shownComplete.value) return;

    if (hideTimer.value !== null) {
      clearTimeout(hideTimer.value);
      hideTimer.value = null;
    }

    if (status) {
      if ((status.totalPercent ?? 0) < 100) {
        visible.value = true;
        shownComplete.value = false;
      } else {
        visible.value = true;
        hideTimer.value = window.setTimeout(() => {
          visible.value = false;
          hideTimer.value = null;
        }, props.timeout);
        shownComplete.value = true;
      }
    } else {
      visible.value = false;
      shownComplete.value = false;
    }
  },
  { deep: true, immediate: true }
);
</script>
