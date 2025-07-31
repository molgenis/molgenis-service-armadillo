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
      <div>
        <details>
          <pre>
            {{ JSON.stringify(data, null, 3) }}
          </pre>
        </details>
      </div>
    </td>
  </tr>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { convertBytes } from "@/helpers/utils";

export default defineComponent({
  name: "ActuatorItem",
  props: {
    name: {
      type: String,
      required: true,
    },
    data: {
      type: Object,
      required: true,
    },
  },
  methods: { convertBytes },
});
</script>
