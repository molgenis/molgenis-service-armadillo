<script setup lang="ts">
import { getMetricsAll } from "@/api/api";
import { ref } from "vue";
import ActuatorItem from "./ActuatorItem.vue";

const metrics = ref(null);
const names = ref<Array<string>>([]);

const loadMetrics = async () => {
  // metrics.value = await getMetrics();
  metrics.value = await getMetricsAll();
  console.log("Loaded?", metrics.value);
  const bare = metrics.value ? ["_bare"] : {};
  names.value = Object.keys(bare);
  console.log("Names?", names.value);
};

loadMetrics();

function downloadJSON(json, filename: string) {
  var dataStr =
    "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(json));
  var downloadAnchorNode = document.createElement("a");
  downloadAnchorNode.setAttribute("href", dataStr);
  downloadAnchorNode.setAttribute("download", filename + ".json");
  document.body.appendChild(downloadAnchorNode); // required for firefox
  downloadAnchorNode.click();
  setTimeout(() => downloadAnchorNode.remove(), 10);
}

function downloadMetrics() {
  downloadJSON(metrics.value, "armadillo-metrics-" + new Date().toISOString());
}
</script>
<template>
  <button class="btn btn-primary" v-if="metrics" @click="downloadMetrics">
    <i class="bi bi-box-arrow-down"></i>
    Download metrics
  </button>
  <table class="table">
    <thead>
      <tr>
        <th scope="col">#</th>
        <th>key</th>
        <th>statistic</th>
        <th>value</th>
      </tr>
    </thead>
    <tbody>
      <ActuatorItem
        v-for="(items, path, index) in metrics"
        :key="index"
        :data="items"
        :name="path"
      />
    </tbody>
  </table>
</template>
