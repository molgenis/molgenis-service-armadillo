<script setup lang="ts">
const props = defineProps({
  name: {
    type: String,
    required: true,
  },
  data: {
    type: Object,
    required: true,
  },
});

/**
 * Convert given bytes to 2 digits precision round exponent version string.
 * @param bytes number
 */
function convertBytes(bytes: number): string {
  const units = ["bytes", "KB", "MB", "GB", "TB", "EB"];
  let unitIndex = 0;

  while (bytes >= 1024 && unitIndex < units.length - 1) {
    bytes /= 1024;
    unitIndex++;
  }

  return `${bytes.toFixed(2)} ${units[unitIndex]}`;
}

/*
{
   "name": "application.ready.time",
   "description": "Time taken for the application to be ready to service requests",
   "baseUnit": "seconds",
   "measurements": [
      {
         "statistic": "VALUE",
         "value": 2.649
      }
   ],
   "availableTags": [
      {
         "tag": "main.application.class",
         "values": [
            "org.molgenis.armadillo.ArmadilloServiceApplication"
         ]
      }
   ]
}
*/
</script>
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
