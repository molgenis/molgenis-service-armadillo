<template>
  <div class="row">
    <div class="col mt-3" v-if="isLoading">
      <LoadingSpinner />
    </div>
    <div class="col" v-else>
      <div class="row">
        <div class="col-sm-3">
          <SearchBar id="searchbox" v-model="filterValue" />
        </div>
        <div class="col">
          <button
            class="btn btn-primary float-end"
            v-if="metrics"
            @click="downloadMetrics"
          >
            <i class="bi bi-box-arrow-down"></i>
            Download metrics
          </button>
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
              <tr v-for="(item, key) in actuator" :key="key">
                <td>{{ key }}</td>
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { getActuator, getMetricsAll } from "@/api/api";
import { ref, watch } from "vue";
import { Metrics, HalLinks } from "@/types/api";
import { ObjectWithStringKey } from "@/types/types";
import { objectDeepCopy } from "@/helpers/utils";

import ActuatorItem from "./ActuatorItem.vue";
import SearchBar from "@/components/SearchBar.vue";
import LoadingSpinner from "./LoadingSpinner.vue";

const actuator = ref<HalLinks>();
const metrics = ref<Metrics>([]);
const isLoading = ref<boolean>(true);

const loadActuator = async () => {
  let result = (await getActuator())["_links"];
  let list = [];
  for (let key in result) {
    // Add key to each item for further usage
    const item = result[key];
    item["key"] = key;
    list.push(result[key]);
  }
  actuator.value = list;
};

const loadMetrics = async () => {
  metrics.value = await getMetricsAll();

  // preload search values
  filteredLines();
  isLoading.value = false;
};

loadMetrics();
loadActuator();

function downloadJSON(filename: string) {
  const cleanedUp = removeFields(metrics.value);
  const dataStr =
    "data:text/json;charset=utf-8," +
    encodeURIComponent(JSON.stringify(cleanedUp));
  const downloadAnchorNode = document.createElement("a");
  downloadAnchorNode.setAttribute("href", dataStr);
  downloadAnchorNode.setAttribute("download", filename + ".json");
  document.body.appendChild(downloadAnchorNode); // required for firefox
  downloadAnchorNode.click();
  setTimeout(() => downloadAnchorNode.remove(), 10);
}

function downloadMetrics() {
  downloadJSON("armadillo-metrics-" + new Date().toISOString());
}

const filterValue = ref("");
watch(filterValue, (_newVal, _oldVal) => filteredLines());

const FIELD_DISPLAY = "_display";
const SEARCH_TEXT_FIELDS = "searchWords";

function concatValues(obj: any): string {
  let result = "";
  for (const key in obj) {
    if (typeof obj[key] === "object" && obj[key] !== null) {
      result += concatValues(obj[key]);
    } else {
      result += obj[key];
    }
  }
  return result;
}

/**
 * Filter metrics on search value
 *
 * We add:
 * - string search field for matching
 * - booelan display field for storing matched
 */
function filteredLines() {
  const filterOn: string = filterValue.value.toLowerCase();
  for (let [_key, value] of Object.entries(metrics.value)) {
    if (!value[SEARCH_TEXT_FIELDS]) {
      value[SEARCH_TEXT_FIELDS] = concatValues(value).toLowerCase();
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
function removeFields(json: Metrics) {
  const result: Metrics = objectDeepCopy<Metrics>(json);
  for (let [_key, value] of Object.entries(result)) {
    const wrapper: ObjectWithStringKey = value;

    delete wrapper[SEARCH_TEXT_FIELDS];
    delete wrapper[FIELD_DISPLAY];
  }
  return result;
}
</script>
