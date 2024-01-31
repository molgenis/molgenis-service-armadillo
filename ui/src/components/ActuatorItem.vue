<script setup lang="ts">
import { getMetric } from "@/api/api";
import { ref } from "vue";

const props = defineProps({
  name: {
    type: String,
    required: true,
  },
});

const actuatorItem = ref(null);

async function fetchData() {
  let res = await getMetric(props.name);
  console.log(res);
  actuatorItem.value = res;
}
fetchData();

function convertBytes(bytes: number) {
  const units = ["bytes", "KB", "MB", "GB", "TB"];
  let unitIndex = 0;

  while (bytes >= 1024 && unitIndex < units.length - 1) {
    bytes /= 1024;
    unitIndex++;
  }

  return `${bytes.toFixed(2)} ${units[unitIndex]}`;
}

console.log(convertBytes(222837712)); // Call the function with your byte value

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
  <div v-if="actuatorItem">
    <h3>
      {{ actuatorItem.name }}
    </h3>
    <summary>
      <details>
        <pre>
          {{ JSON.stringify(actuatorItem, null, 3) }}
        </pre>
      </details>
    </summary>
    <h4 v-if="actuatorItem.description">
      {{ actuatorItem.description }}
    </h4>
    <table class="table">
      <thead>
        <tr>
          <th scope="col">#</th>
          <th>statistic</th>
          <th>value</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(v, k) in actuatorItem.measurements">
          <td scope="col">{{ k }}</td>
          <td>{{ v.statistic }}</td>
          <td v-if="actuatorItem.baseUnit === 'bytes'">
            {{ convertBytes(v.value) }}
          </td>
          <td v-else>{{ v.value }} {{ actuatorItem.baseUnit }}</td>
        </tr>
      </tbody>
    </table>
  </div>
  <div v-else>Waiting for {{ name }}</div>
</template>
