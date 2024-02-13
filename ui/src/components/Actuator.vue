<script setup lang="ts">
import { getActuator, getMetricsAll } from "@/api/api";
import { ref, watch } from "vue";
import ActuatorItem from "./ActuatorItem.vue";
import SearchBar from "@/components/SearchBar.vue";

const actuator = ref({});
const metrics = ref({});

const loadActuator = async () => {
  let result = (await getActuator())["_links"];
  let list = [];
  for (let key in result) {
    const item = result[key];
    item["key"] = key;
    list.push(result[key]);
  }
  actuator.value = list;
  console.log(JSON.stringify(actuator.value, null, 2));
};
const loadMetrics = async () => {
  metrics.value = await getMetricsAll();

  // preload search values
  filteredLines();
};

loadMetrics();
loadActuator();
function downloadJSON(json, filename: string) {
  const cleanedUp = removeFields(json);
  var dataStr =
    "data:text/json;charset=utf-8," +
    encodeURIComponent(JSON.stringify(cleanedUp));
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

/**
 * Filter metrics on search value
 *
 * We add:
 * - string search field for matching
 * - booelan display field for storing matched
 */
function filteredLines() {
  const filterOn: string = filterValue.value.toLowerCase();
  for (let [key, value] of Object.entries(metrics.value)) {
    if (!value[SEARCH_TEXT_FIELDS]) {
      // TODO: drop keys before stringify?
      value[SEARCH_TEXT_FIELDS] = JSON.stringify(value).toLowerCase();
    }
    const searchWords: string = value[SEARCH_TEXT_FIELDS];
    value[FIELD_DISPLAY] = filterOn === "" || searchWords.includes(filterOn);
  }
}

/**
 * Remove added fields for searching.
 *
 * @param json
 */
function removeFields(json) {
  const result = JSON.parse(JSON.stringify(json));
  for (let [key, value] of Object.entries(result)) {
    delete value[SEARCH_TEXT_FIELDS];
    delete value[FIELD_DISPLAY];
  }
  return result;
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
  <hr />
  <summary>
    <h3>Other Actuator links</h3>
    <details>
      <table>
        <thead>
          <tr>
            <td>key</td>
            <td>href</td>
            <td>templated</td>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in actuator">
            <td>{{ item.key }}</td>
            <td v-if="item.templated">{{ item.href }}</td>
            <td v-if="!item.templated">
              <a :href="item.href" target="_new">{{ item.href }}</a>
            </td>
            <td>{{ item.templated }}</td>
          </tr>
        </tbody>
      </table>
    </details>
  </summary>
</template>
