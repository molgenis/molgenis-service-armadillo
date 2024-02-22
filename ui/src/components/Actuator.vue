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
            v-for="(metric, path, index) in filteredLines"
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
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, onMounted, Ref, ref } from "vue";
import { getActuator, getMetricsAll } from "@/api/api";
import ActuatorItem from "./ActuatorItem.vue";
import SearchBar from "@/components/SearchBar.vue";
import LoadingSpinner from "./LoadingSpinner.vue";

export default defineComponent({
  name: "Actuator",
  components: {
    ActuatorItem,
    SearchBar,
    LoadingSpinner,
  },
  emits: ["loading-done"],
  setup(_, { emit }) {
    const actuator = ref({});
    const metrics = ref({});
    const isLoading = ref(true);
    let actuatorLoaded = false;
    let metricsLoaded = false;

    onMounted(() => {
      loadActuator();
      loadMetrics();
    });
    function emitIfLoadingDone() {
      if (actuatorLoaded && metricsLoaded) {
        isLoading.value = false;
        emit("loading-done");
      }
    }
    async function loadActuator() {
      await getActuator().then((data) => {
        let list = [];
        const result = data["_links"];
        for (let key in result) {
          const item = result[key];
          item["key"] = key;
          list.push(result[key]);
        }
        actuator.value = list;
        actuatorLoaded = true;
        emitIfLoadingDone();
      });
    }
    async function loadMetrics() {
      await getMetricsAll().then((data) => {
        metrics.value = data;
        metricsLoaded = true;
        emitIfLoadingDone();
      });
    }
    return {
      actuator,
      metrics,
      isLoading,
      loadMetrics,
      loadActuator,
    };
  },
  data() {
    return {
      filterValue: "",
      SEARCH_TEXT_FIELDS: "searchWords",
    };
  },
  computed: {
    /**
     * Filter metrics on search value
     *
     * We add:
     * - string search field for matching
     * - boolean display field for storing matched
     */
    filteredLines() {
      if (this.filterValue.length) {
        const filterOn: string = this.filterValue.toLowerCase();
        return Object.values(this.metrics).filter((metric) => {
          if (!metric[this.SEARCH_TEXT_FIELDS]) {
            metric[this.SEARCH_TEXT_FIELDS] =
              JSON.stringify(metric).toLowerCase();
          }
          const searchWords: string = metric[this.SEARCH_TEXT_FIELDS];
          return (metric[this.FIELD_DISPLAY] =
            filterOn === "" || searchWords.includes(filterOn));
        });
      } else {
        return Object.values(this.metrics);
      }
    },
  },
  watch: {},
  methods: {
    downloadJSON(json, filename: string) {
      const cleanedUp = this.removeFields(json);
      var dataStr =
        "data:text/json;charset=utf-8," +
        encodeURIComponent(JSON.stringify(cleanedUp));
      var downloadAnchorNode = document.createElement("a");
      downloadAnchorNode.setAttribute("href", dataStr);
      downloadAnchorNode.setAttribute("download", filename + ".json");
      document.body.appendChild(downloadAnchorNode); // required for firefox
      downloadAnchorNode.click();
      setTimeout(() => downloadAnchorNode.remove(), 10);
    },
    downloadMetrics() {
      this.downloadJSON(
        this.metrics,
        "armadillo-metrics-" + new Date().toISOString()
      );
    },
    /**
     * Remove added fields for searching.
     *
     * @param json
     */
    removeFields(json) {
      const result = JSON.parse(JSON.stringify(json));
      for (let [key, value] of Object.entries(result)) {
        delete value[this.SEARCH_TEXT_FIELDS];
      }
      return result;
    },
  },
});
</script>
