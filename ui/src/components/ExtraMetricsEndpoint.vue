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
            {{ result }}
          </code>
        </pre>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { defineComponent, PropType } from "vue";
import LoadingSpinner from "./LoadingSpinner.vue";
import FeedbackMessage from "./FeedbackMessage.vue";
import { ActuatorLink } from "@/types/api";
import { APISettings } from "@/api/config";

export default defineComponent({
  name: "ExtraMetricsEndpoint",
  components: {
    FeedbackMessage,
    LoadingSpinner,
  },
  props: {
    endpoint: {
      type: Object as PropType<ActuatorLink>,
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
      await fetch(endpoint, {
        method: "GET",
        headers: APISettings.headers,
      })
        .then((response) => {
          this.isLoading = false;
          this.errorMsg = "";
          const contentType = response.headers.get("content-type");
          console.log(contentType);
          if (contentType && contentType.indexOf("json") !== -1) {
            return response.json().then((data) => {
              this.result = data;
            });
          } else {
            return response.text().then((text) => {
              this.result = text;
            });
          }
        })
        .catch((error) => {
          this.isLoading = false;
          this.errorMsg = error.message;
        });
    },
  },
});
</script>
