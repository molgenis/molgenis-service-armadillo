<script setup lang="ts">
import { getMetricsAll } from "@/api/api";
import { ref, watch } from "vue";
import ActuatorItem from "./ActuatorItem.vue";
import SearchBar from "@/components/SearchBar.vue";
import { json } from "stream/consumers";

const metrics = ref(null);
const names = ref<Array<string>>([]);

const loadMetrics = async () => {
  // metrics.value = await getMetrics();
  metrics.value = await getMetricsAll();
  console.log("Loaded?", metrics.value);
  const bare = metrics.value ? ["_bare"] : {};
  names.value = Object.keys(bare);
  console.log("Names?", names.value);
  // preload search values
  filteredLines();
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

const filterValue = ref("");
watch(filterValue, (_newVal, _oldVal) => filteredLines());

const FIELD_DISPLAY = "_display";
const SEARCH_TEXT_FIELDS = "searchWords";

function filteredLines() {
  const filterOn: string = filterValue.value.toLowerCase();
  console.log("Filtering", filterOn);
  for (let [key, value] of Object.entries(metrics.value)) {
    if (!value[SEARCH_TEXT_FIELDS]) {
      value[SEARCH_TEXT_FIELDS] = JSON.stringify(value).toLowerCase();
    }
    const searchWords: string = value[SEARCH_TEXT_FIELDS];
    value[FIELD_DISPLAY] = filterOn === "" || searchWords.includes(filterOn);
  }
  console.log(metrics.value);
}

function displayMetric(metric) {
  console.log(metric);
  if (metric[FIELD_DISPLAY]) {
    return metric[FIELD_DISPLAY];
  } else {
    return false;
  }
}
</script>
<template>
  <button class="btn btn-primary" v-if="metrics" @click="downloadMetrics">
    <i class="bi bi-box-arrow-down"></i>
    Download metrics
  </button>
  <div class="row">
    <div class="col-sm-3">
      <SearchBar id="searchbox" v-model="filterValue" />
    </div>
  </div>
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
        v-for="(metric, path, index) in metrics"
        :key="index"
        :data="metric"
        :name="path"
      />
    </tbody>
  </table>
</template>
