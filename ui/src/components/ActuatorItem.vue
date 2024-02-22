<template>
  <tr v-for="(v, k) in data.measurements">
    <td scope="col">{{ k }}</td>
    <td :title="data.description">
      <span>
        {{ data.name }}
        <i v-if="data.description" class="bi bi-info-circle-fill"></i>
      </span>
    </td>
    <td>{{ v.statistic }}</td>
    <td v-if="data.baseUnit === 'bytes'">
      {{ convertBytes(v.value) }}
    </td>
    <td v-else>{{ v.value }} {{ data.baseUnit }}</td>
  </tr>
  <tr>
    <td colspan="5">
      <summary>
        <details>
          <pre>
            {{ JSON.stringify(data, null, 3) }}
          </pre>
        </details>
      </summary>
    </td>
  </tr>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "ActuatorItem",
  props: {
    name: {
      type: Number,
      required: true,
    },
    data: {
      type: Object,
      required: true,
    },
  },
  methods: {
    /**
     * Convert given bytes to 2 digits precision round exponent version string.
     * @param bytes number
     */
    convertBytes(bytes: number): string {
      const units = ["bytes", "KB", "MB", "GB", "TB", "EB"];
      let unitIndex = 0;

      while (bytes >= 1024 && unitIndex < units.length - 1) {
        bytes /= 1024;
        unitIndex++;
      }

      return `${bytes.toFixed(2)} ${units[unitIndex]}`;
    },
  },
});
</script>
