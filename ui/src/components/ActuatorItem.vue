<template>
  <tr v-if="data._display" v-for="(v, key) in data.measurements" :key="key">
    <td scope="col">{{ key }}</td>
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
  <tr v-if="data._display">
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

<script setup lang="ts">
import { convertBytes } from "@/helpers/utils";
const props = defineProps({
  name: {
    type: String,
    required: true,
  },
  methods: {
    convertBytes,
  },
  data: {
    type: Object,
    required: true,
  },
});
</script>
