<script setup lang="ts">
import { computed, ref, unref, watch, type Ref } from "vue";
import type { ContainerStartStatus } from "@/types/api";

const props = defineProps<{
  status: ContainerStartStatus | null | Ref<ContainerStartStatus | null>;
  containerName: string;
}>();

const current = computed<ContainerStartStatus | null>(() =>
  unref(props.status)
);

const serverPerc = computed(() => {
  const s = current.value;
  if (!s) return 0;
  const total = s.totalLayers ?? 0;
  const completed = s.completedLayers ?? 0;
  return total > 0 ? Math.round((completed / total) * 100) : 0;
});

const smoothedPerc = ref(0);
const displayedPerc = computed(() => Math.round(smoothedPerc.value));

function smoothServerPercentage() {
  const cap = serverPerc.value === 100 ? 100 : 99;

  smoothedPerc.value = Math.max(smoothedPerc.value, serverPerc.value);

  if (smoothedPerc.value < cap) {
    const remaining = cap - smoothedPerc.value;
    const base = 0.3;
    const taper = 0.25 + 0.75 * (remaining / cap);
    const step = Math.max(0.1, base * taper);
    smoothedPerc.value = Math.min(cap, smoothedPerc.value + step);
  }
}

watch(() => current.value, smoothServerPercentage, { immediate: true });
</script>

<template>
  <div
    v-if="current?.status === 'Installing container'"
    class="alert alert-info mt-2"
    style="display: flex; align-items: center; gap: 0.5rem"
  >
    {{ current?.status }} '{{ props.containerName }}'

    <div
      class="progress flex-grow-1"
      role="progressbar"
      aria-valuemin="0"
      aria-valuemax="100"
      style="height: 1rem"
    >
      <div
        class="progress-bar"
        :aria-valuenow="displayedPerc"
        :style="{ width: displayedPerc + '%' }"
      >
        {{ displayedPerc }}%
      </div>
    </div>
  </div>
</template>
