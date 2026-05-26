<template>
  <div class="card mt-2 mb-2">
    <h5 class="card-header">
      <i class="bi bi-window-fullscreen"></i> Application
    </h5>
    <div class="card-body">
      <h5>Update</h5>
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
            >Downloading molgenis-armadillo-{{ versionToDownload }}.jar
          </span>
          <i
            class="bi bi-check-circle-fill text-success"
            v-if="downloadPercentage === 100"
          ></i>
          <span v-else>{{ downloadPercentage }} %</span>
        </div>
      </div>
      <div class="row">
        <div class="col">
          <h5>
            Restart
            <button
              class="btn btn-sm btn-link"
              :class="showRestartInfo ? 'text-secondary' : 'text-info'"
              @click="showRestartInfo = !showRestartInfo"
            >
              <i class="bi bi-info-circle-fill"></i>
            </button>
          </h5>
          <div class="alert alert-info" role="alert" v-if="showRestartInfo">
            <h6 class="alert-heading">
              <i class="bi bi-info-circle-fill"></i> Restarting armadillo
              <button
                class="btn btn-link text-danger p-0 pe-1 float-end"
                @click="showRestartInfo = false"
              >
                <i class="bi bi-x-circle-fill"></i>
              </button>
            </h6>
            <hr />
            <p class="mb-0">
              If your application isn't behaving as it should, a restart might
              help. With the buttons below you can do a "soft" or "hard"
              restart. We advice to first try a soft restart, if that doesn't
              fix your problem, try the hard restart. Keep in mind that in both
              options, currently running analyses will probably be terminated
              and that there is a slight risk that the application doesn't start
              after shutting down, meaning you will have to contact your
              administrator.
            </p>
          </div>
          <div
            class="btn-group"
            role="group"
            aria-label="Basic outlined example"
          >
            <button
              class="btn btn-outline-dark btn-warning"
              @click="$emit('soft-restart-pushed')"
            >
              <i class="bi bi-arrow-repeat"></i> Soft restart
            </button>
            <button
              class="btn btn-outline-dark btn-warning"
              @click="$emit('hard-restart-pushed')"
            >
              <i class="bi bi-power"></i>/<i class="bi bi-play-fill"></i> Hard
              restart
            </button>
          </div>
        </div>
      </div>
      <div class="row mt-3">
        <h5>
          Advanced update &nbsp;
          <button
            class="btn btn-outline-primary btn-sm text-start"
            @click="advancedUpdateCollapsed = !advancedUpdateCollapsed"
          >
            <i class="bi bi-chevron-down" v-if="advancedUpdateCollapsed"></i>
            <i class="bi bi-chevron-up" v-else></i>
          </button>
        </h5>
      </div>
      <div class="card" v-if="!advancedUpdateCollapsed">
        <div class="card-body">
          <div class="row mb-2">
            <h6>Download version</h6>
            <div class="col-sm-8 ms-2">
              <FormInput
                label="Version number"
                :value="versionToDownload"
                :isEditMode="true"
                ref="versionInput"
                mb-0
              />
              <span class="text-secondary offset-sm-3 fst-italic mt-0">
                <span class="ms-sm-2"> e.g. 5.12.2 </span>
              </span>
            </div>
            <div class="col">
              <button class="btn btn-primary" @click="downloadVersion">
                <i class="bi bi-box-arrow-down"></i> Download
              </button>
            </div>
          </div>
          <div class="row mb-3">
            <h6>Update version</h6>
            <div class="col-sm-8 ms-2">
              <div class="row">
                <div class="col-sm-3 mt-2">Version number</div>
                <div class="col-sm-9">
                  <Dropdown :options="appList" @update="selectUpdateVersion" />
                  <span class="text-secondary fst-italic"
                    >If the version you want to run is not in this list,
                    download it first</span
                  >
                </div>
              </div>
            </div>
            <div class="col">
              <button
                class="btn btn-primary"
                @click="$emit('update-app', updateVersion)"
              >
                <i class="bi bi-arrow-up-circle"></i> &nbsp;&nbsp;Update
                &nbsp;&nbsp;
              </button>
            </div>
          </div>
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
  emits: [
    "update-app",
    "error",
    "download-done",
    "soft-restart-pushed",
    "hard-restart-pushed",
  ],
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
      versionToDownload: "",
      downloadPercentage: 0,
      updateVersion: "",
      advancedUpdateCollapsed: true,
      showRestartInfo: false,
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
