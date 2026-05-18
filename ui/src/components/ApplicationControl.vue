<template>
  <div class="card mt-2 mb-2">
    <h5 class="card-header">
      <i class="bi bi-window-fullscreen"></i> Application
    </h5>
    <div class="card-body">
      <Alert v-if="isUpdateAvailable" type="info" :dismissible="false">
        Update available: {{ latestReleaseVersion }}
        <div class="pb-0" v-if="!latestVersionDownloaded">
          <button class="btn btn-sm btn-primary mt-2" @click="downloadLatest">
            <i class="bi bi-download"></i> Download update
          </button>
        </div>
        <div class="pb-0" v-else>
          <button
            class="btn btn-sm btn-success mt-2"
            @click="$emit('update-app', latestReleaseVersion)"
          >
            <i class="bi bi-play-fill"></i> Update now
          </button>
        </div>
      </Alert>
      <Alert v-else type="success" :dismissible="false">
        <i class="bi bi-check-circle-fill"></i>Running latest version:
        {{ latestReleaseVersion }}
      </Alert>
      <div class="row" v-if="downloadPercentage > 0">
        <div class="col-8 mt-1">
          <ProgressBar :percentage="downloadPercentage" />
        </div>
        <div class="col-4 mb-2">
          <span class="fst-italic"
            >Downloading molgenis-armadillo-{{ versionToDownload }}.jar </span
          ><i
            class="bi bi-check-circle-fill text-success"
            v-if="downloadPercentage === 100"
          ></i
          ><span v-else>{{ downloadPercentage }} %</span>
        </div>
      </div>
      <div class="row">
        <h5>Download version</h5>
        <div class="col-md-6 col-sm-8">
          <FormInput
            label="Version"
            :value="versionToDownload"
            :isEditMode="true"
            ref="versionInput"
          />
        </div>
        <div class="col">
          <button class="btn btn-primary" @click="downloadVersion">
            <i class="bi bi-box-arrow-down"></i> Download
          </button>
        </div>
      </div>
      <div class="row mb-3">
        <h5>Update version</h5>
        <div class="col-md-6 col-sm-8">
          <Dropdown :options="appList" @update="selectUpdateVersion" />
        </div>
        <div class="col">
          <button
            class="btn btn-primary"
            @click="$emit('update-app', updateVersion)"
          >
            <i class="bi bi-arrow-up-circle"></i> Update
          </button>
        </div>
      </div>
      <div class="row">
        <div class="col">
          <h5>Restart</h5>
          <button class="btn btn-warning" @click="isRestartServerPushed = true">
            <i class="bi bi-arrow-repeat"></i> Soft restart
          </button>
          <button
            class="btn btn-warning"
            @click="makeIsRestartServerPushedTrue"
          >
            <i class="bi bi-arrow-repeat"></i> Hard restart
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import Alert from "./Alert.vue";
import { downloadJar } from "@/api/api";
import ProgressBar from "./ProgressBar.vue";
import FormInput from "./FormInput.vue";
import Dropdown from "./Dropdown.vue";

export default defineComponent({
  name: "ApplicationControl",
  emits: ["update-app", "error", "download-done"],
  components: {
    Alert,
    ProgressBar,
    FormInput,
    Dropdown,
  },
  props: {
    latestReleaseVersion: { type: String, required: false },
    currentReleaseVersion: { type: String, required: true },
    latestVersionDownloaded: { type: Boolean, default: false },
    appList: { type: Array as PropType<string[]>, default: [] },
  },
  data() {
    return {
      isRestartServerPushed: false,
      versionToDownload: "",
      downloadPercentage: 0,
      updateVersion: "",
    };
  },
  methods: {
    selectUpdateVersion(event: Event) {
      this.updateVersion = event
        .toString()
        .replace(".jar", "")
        .replace("molgenis-armadillo-", "");
    },
    isValidVersion(version: string) {
      const regex = /v?[0-9]+\.[0-9]+\.[0-9]+/;
      const found = version.match(regex);
      return found == null ? false : found.length == 1;
    },
    makeIsRestartServerPushedTrue() {
      this.isRestartServerPushed = true;
    },
    makeIsRestartServerPushedFalse() {
      this.isRestartServerPushed = false;
    },
    downloadNewRelease() {
      this.downloadPercentage = 0;
      const source = downloadJar(this.versionToDownload);
      source.addEventListener("progress", (e) => {
        this.downloadPercentage = parseInt(e.data);
      });
      source.addEventListener("done", () => {
        this.$emit("download-done");
        source.close();
      });
      //TODO: error processing?
    },
    downloadVersion() {
      const version = (this.$refs.versionInput as any).mappedValue;
      if (this.isValidVersion(version)) {
        this.versionToDownload = version;
        this.downloadNewRelease();
      } else {
        this.$emit(
          "error",
          `Cannot download jar: Version [${version}] not a valid version`
        );
      }
    },
    downloadLatest() {
      if (this.latestReleaseVersion !== undefined) {
        this.versionToDownload = this.latestReleaseVersion;
        this.downloadNewRelease();
      }
    },
  },
  computed: {
    isUpdateAvailable() {
      return this.latestReleaseVersion != this.currentReleaseVersion;
    },
  },
});
</script>
