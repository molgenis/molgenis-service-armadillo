<template>
  <div class="card m-1">
    <div class="card-header fw-bold">
      {{ endpoint.key }}
    </div>
    <div class="card-body">
      <div class="sm fst-italic mb-2">{{ endpoint.href }}</div>
      <form class="mb-2" v-if="endpoint.templated">
        <label class="form-label">{{ argument }}</label>
        <input type="text" class="form-control" v-model="argumentInput" />
      </form>
      <button
        class="btn btn-success btn-sm"
        :disabled="(endpoint.templated && argumentInput === '') || result != ''"
        @click="getResult"
      >
        Execute <i class="bi bi-play-fill"></i>
      </button>
      <a
        class="btn btn-primary btn-sm"
        v-if="!endpoint.templated"
        :href="endpoint.href"
        target="_blank"
        ><i class="bi bi-box-arrow-up-right"></i
      ></a>
      <LoadingSpinner v-if="isLoading" />
      <FeedbackMessage
        v-if="errorMsg !== ''"
        :errorMessage="errorMsg"
        :successMessage="''"
      />
      <div v-if="result != ''" class="m-2 bg-light border rounded p-2">
        <div class="float-end">
          <button class="btn btn-sm btn-danger" @click="clearResult">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <pre>
          <code> 
            {{ JSON.stringify(result, null, 2) }}
          </code>
        </pre>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { EndpointObject } from "@/types/types";
import { get, handleResponse } from "@/api/api";
import { PropType } from "vue";
import LoadingSpinner from "./LoadingSpinner.vue";
import FeedbackMessage from "./FeedbackMessage.vue";
import { APISettings } from "@/api/config";

export default {
  name: "ExtraMetricsEndpoint",
  components: {
    FeedbackMessage,
    LoadingSpinner,
  },
  props: {
    endpoint: {
      type: Object as PropType<EndpointObject>,
      required: true,
    },
  },
  data() {
    return {
      result: "",
      argumentInput: "",
      isLoading: false,
      errorMsg: "",
    };
  },
  computed: {
    splittedEndpoint() {
      const firstSplit = this.endpoint.href.split("{");
      const secondSplit = firstSplit[1].split("}");
      return [firstSplit[0], secondSplit[0], secondSplit[1]];
    },
    argument() {
      return this.endpoint.templated ? this.splittedEndpoint[1] : "";
    },
  },
  methods: {
    clearResult() {
      this.result = "";
    },
    async getResult() {
      let endpoint = this.endpoint.href;
      if (this.endpoint.templated) {
        endpoint =
          this.splittedEndpoint[0] +
          this.argumentInput +
          this.splittedEndpoint[2];
      }
      this.isLoading = true;
      get(endpoint)
        .then((endpointResult: string) => {
          this.isLoading = false;
          this.errorMsg = "";
          this.result = endpointResult;
        })
        .catch((error) => {
          this.isLoading = false;
          this.errorMsg = error.message;
        });
    },
  },
};
</script>
