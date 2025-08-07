<template>
  <div class="row">
    <div class="col mt-3" v-if="isLoading">
      <LoadingSpinner />
      <FeedbackMessage
        v-if="errorMessage != ''"
        :errorMessage="errorMessage"
      ></FeedbackMessage>
    </div>
    <div class="col" v-else>
      <div class="row">
        <div class="col-sm-3 align-self-end">
          <SearchBar id="searchbox" v-model="searchString" />
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
      <div>
        <div class="m-2">
          <h5><i class="bi bi-hdd"></i> Disk</h5>
          <div class="progress">
            <div
              class="progress-bar bg-success"
              role="progressbar"
              aria-label="Success striped example"
              :style="'width:' + diskspace.percentage + '%'"
              :aria-valuenow="diskspace.percentage.toString()"
              aria-valuemin="0"
              aria-valuemax="100"
            ></div>
          </div>
          Used: {{ diskspace.used }} / {{ diskspace.total }}
        </div>
        <div class="row">
          <div class="col">
            <MetricsCard :metrics="metrics" item="application" icon="window" />
            <MetricsCard :metrics="metrics" item="process" icon="cpu" />
            <MetricsCard :metrics="metrics" item="system" icon="gear" />
          </div>
          <div class="col">
            <MetricsCard :metrics="metrics" item="user" icon="person-fill" />
            <MetricsCard
              :metrics="metrics"
              item="rserve"
              icon="r-circle-fill"
            />
            <MetricsCard :metrics="metrics" item="http" icon="wifi" />
            <MetricsCard :metrics="metrics" item="tomcat" icon="hdd-stack" />
          </div>
        </div>
        <MetricsCard :metrics="metrics" item="jvm" icon="filetype-java" />
        <MetricsCard :metrics="metrics" item="spring" icon="leaf" />
      </div>
      <hr />
      <div>
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
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, onMounted } from "vue";
import { getActuator, getMetricsAll } from "@/api/api";
import { ref, Ref } from "vue";
import { Metrics, ActuatorLink, HalLinks } from "@/types/api";
import { ObjectWithStringKey } from "@/types/types";
import { objectDeepCopy, convertBytes } from "@/helpers/utils";
import { useRouter } from "vue-router";
import { processErrorMessages } from "@/helpers/errorProcessing";
import SearchBar from "@/components/SearchBar.vue";
import LoadingSpinner from "./LoadingSpinner.vue";
import FeedbackMessage from "./FeedbackMessage.vue";
import MetricsCard from "./MetricsCard.vue";

export default defineComponent({
  name: "Actuator",
  components: {
    LoadingSpinner,
    SearchBar,
    FeedbackMessage,
    MetricsCard,
  },
  setup() {
    const actuator = ref<ActuatorLink[]>();
    const metrics = ref<Metrics>({});
    const isLoading = ref<boolean>(true);
    const errorMessage: Ref<string> = ref("");
    const router = useRouter();

    onMounted(() => {
      loadActuator();
      loadMetrics();
    });
    const loadActuator = async () => {
      let response = await getActuator().catch((error: string) => {
        errorMessage.value = processErrorMessages(error, "actuator", router);
        return { _links: {} };
      });

      const result: HalLinks = response["_links"];
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
      metrics.value = await getMetricsAll().catch((error: string) => {
        errorMessage.value = processErrorMessages(error, "metrics", router);
        return {};
      });
      isLoading.value = false;
    };
    return {
      actuator,
      metrics,
      isLoading,
      errorMessage,
    };
  },
  data(): {
    searchString: String;
  } {
    return {
      searchString: "",
    };
  },
  watch: {
    searchString() {
      this.filterMetrics();
    },
    isLoading() {
      this.filterMetrics();
    },
  },
  computed: {
    diskspace(): {
      total: String;
      free: String;
      used: String;
      percentage: Number;
    } {
      const totalSpace = this.metrics["disk.total"]["measurements"][0].value;
      const freeSpace = this.metrics["disk.free"]["measurements"][0].value;
      const usedSpace = totalSpace - freeSpace;
      return {
        total: convertBytes(totalSpace),
        free: convertBytes(freeSpace),
        used: convertBytes(usedSpace),
        percentage: (usedSpace * 100) / totalSpace,
      };
    },
  },
  methods: {
    convertBytes,
    filterMetrics() {
      const SEARCH_TEXT_FIELDS = "searchWords";
      const filterOn: string = this.searchString.toLowerCase();
      for (let [_key, value] of Object.entries(this.metrics)) {
        if (!this.metrics[SEARCH_TEXT_FIELDS]) {
          value[SEARCH_TEXT_FIELDS] =
            this.getConcatenatedValues(value).toLowerCase();
        }
        const searchWords: string | undefined = value[SEARCH_TEXT_FIELDS];
        value["_display"] = filterOn === "" || searchWords?.includes(filterOn);
      }
    },
    getConcatenatedValues(obj: any): String {
      let result = "";
      for (const key in obj) {
        if (typeof obj[key] === "object" && obj[key] !== null) {
          result += this.getConcatenatedValues(obj[key]);
        } else {
          result += obj[key];
        }
      }
      return result;
    },
    downloadJSON(filename: String) {
      const cleanedUp = this.removeFields(this.metrics);
      const dataStr =
        "data:text/json;charset=utf-8," +
        encodeURIComponent(JSON.stringify(cleanedUp));
      const downloadAnchorNode = document.createElement("a");
      downloadAnchorNode.setAttribute("href", dataStr);
      downloadAnchorNode.setAttribute("download", filename + ".json");
      document.body.appendChild(downloadAnchorNode); // required for firefox
      downloadAnchorNode.click();
      setTimeout(() => downloadAnchorNode.remove(), 10);
    },
    removeFields(json: Metrics) {
      const result: Metrics = objectDeepCopy<Metrics>(json);
      for (let [_key, value] of Object.entries(result)) {
        const wrapper: ObjectWithStringKey = value;

        delete wrapper["searchWords"];
        delete wrapper["_display"];
      }
      return result;
    },
    downloadMetrics() {
      this.downloadJSON("armadillo-metrics-" + new Date().toISOString());
    },
  },
});
</script>
