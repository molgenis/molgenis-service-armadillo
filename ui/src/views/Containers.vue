<template>
  <div>
    <div class="row">
      <div class="col">
        <!-- Error messages will appear here -->
        <ContainerStatusMessage
          :status="containerStatus"
          :containerName="loadingContainer"
        />
        <FeedbackMessage
          :successMessage="successMessage"
          :errorMessage="errorMessage"
        ></FeedbackMessage>
        <ConfirmationDialog
          v-if="recordToDelete !== ''"
          :record="recordToDelete"
          action="delete"
          recordType="container"
          @proceed="proceedDelete"
          @cancel="clearRecordToDelete"
        ></ConfirmationDialog>
      </div>
    </div>
    <LoadingSpinner v-if="containersLoading" class="mt-5" />
    <EditContainerCard
      v-else-if="containerToEdit != ''"
      :container="getContainerByName(containerToEdit)"
      :template="
        getContainerByName(containerToEdit).image.startsWith('datashield/')
          ? 'ds'
          : 'default'
      "
      :status="
        statusMapping[
          getContainerByName(containerToEdit).container
            .status as keyof typeof statusMapping
        ]
      "
      @cancel-edit="this.containerToEdit = ''"
    />
    <div v-else>
      <span v-for="container in containers" :ref="container.name">
        <ContainerCard
          @showEditContainer="setEditContainer"
          :name="container.name"
          :image="container.image"
          :version="'versionId' in container ? container.versionId : 'Unknown'"
          :size="convertBytes(container.imageSize)"
          :port="container.port"
          :isLoading="loading && loadingContainer === container.name"
          :loadingEnabled="loading"
          :status="
            statusMapping[
              container.container.status as keyof typeof statusMapping
            ]
          "
          :template="
            container.image.startsWith('datashield/') ? 'ds' : 'default'
          "
          :deleteFunction="removeContainer"
          :startFunction="startDockerContainer"
          :stopFunction="stopDockerContainer"
          :creationDate="container.creationDate"
          :installationDate="container.installDate"
          :autoUpdate="container.autoUpdate"
          :updateSchedule="container.updateSchedule"
          :options="getOtherOptions(container.options)"
        >
          <DataShieldContainerInfo
            v-if="container.image.startsWith('datashield/')"
            :packageWhitelist="container.packageWhitelist"
            :functionBlacklist="container.functionBlacklist"
            :options="container.options"
          />
          <!-- Add component for different types here -->
        </ContainerCard>
      </span>
    </div>
  </div>
</template>

<script lang="ts">
import ConfirmationDialog from "@/components/ConfirmationDialog.vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import { defineComponent, onMounted, Ref, ref } from "vue";
import { Container } from "@/types/api";
import {
  deleteContainer,
  getContainers,
  putContainer,
  startContainer,
  stopContainer,
} from "@/api/api";
import InlineRowEdit from "@/components/InlineRowEdit.vue";
import Table from "@/components/Table.vue";
import ButtonGroup from "@/components/ButtonGroup.vue";
import ContainerStatus from "@/components/ContainerStatus.vue";
import Badge from "@/components/Badge.vue";
import { ContainersData, TypeObject } from "@/types/types";
import { useRouter } from "vue-router";
import { isDuplicate } from "@/helpers/utils";
import { processErrorMessages } from "@/helpers/errorProcessing";
import { convertBytes, useContainerStatus } from "@/helpers/utils";
import ContainerStatusMessage from "@/components/ContainerStatusMessage.vue";
import containerStatus from "@/components/ContainerStatus.vue";
import ContainerCard from "@/components/ContainerCard.vue";
import DataShieldContainerInfo from "@/components/DataShieldContainerInfo.vue";
import EditContainerCard from "@/components/EditContainerCard.vue";
import containerCard from "@/components/ContainerCard.vue";

export default defineComponent({
  name: "Containers",
  components: {
    EditContainerCard,
    DataShieldContainerInfo,
    Badge,
    ConfirmationDialog,
    FeedbackMessage,
    InlineRowEdit,
    LoadingSpinner,
    Table,
    ButtonGroup,
    ContainerStatus,
    ContainerStatusMessage,
    ContainerCard,
  },
  setup() {
    const containers: Ref<Container[]> = ref([]);
    const containersLoading: Ref<Boolean> = ref(true);
    const errorMessage: Ref<string> = ref("");
    const dockerManagementEnabled: Ref<boolean> = ref(false);
    const router = useRouter();
    const loadingContainer = ref(""); // reactive container name
    const {
      status: containerStatus,
      startPolling,
      stopPolling,
    } = useContainerStatus();
    onMounted(async () => {
      await loadContainers();
    });
    const loadContainers = async () => {
      containers.value = await getContainers()
        .then((containers) => {
          dockerManagementEnabled.value = "container" in containers[0];

          return containers.map((container) => {
            // Extract datashieldSeed
            const datashieldSeed = container.options["datashield.seed"];
            delete container.options["datashield.seed"];

            return {
              ...container,
              datashieldSeed,
            };
          });
        })
        .catch((error: string) => {
          errorMessage.value = processErrorMessages(
            error,
            "containers",
            router
          );
          return [];
        });

      containersLoading.value = false;
    };
    return {
      containersLoading,
      containers,
      errorMessage,
      loadContainers,
      dockerManagementEnabled,
      convertBytes,
      containerStatus,
      startPolling,
      stopPolling,
    };
  },
  data(): ContainersData {
    return {
      addContainer: false,
      recordToDelete: "",
      loading: false,
      loadingContainer: "",
      successMessage: "",
      containerToEditIndex: -1,
      containerToEdit: "",
      dsOptions: {
        "nfilter.tab": "",
        "nfiler.subset": "",
        "nfilter.glm": "",
        "nfilter.string": "",
        "nfilter.stringShort": "",
        "nfilter.kNN": "",
        "nfilter.levels.density": "",
        "nfilter.levels.max": "",
        "nfilter.noise": "",
        "datashield.privacyControlLevel": "",
      },
      statusMapping: {
        NOT_FOUND: {
          status: "OFFLINE",
          text: "Start",
          color: "secondary",
          icon: "play-circle-fill",
        },
        NOT_RUNNING: {
          status: "OFFLINE",
          text: "Start",
          color: "secondary",
          icon: "play-circle-fill",
        },
        RUNNING: {
          status: "ONLINE",
          text: "Stop",
          color: "success",
          icon: "stop-circle-fill",
        },
        DOCKER_OFFLINE: {
          status: "ERROR",
          text: "Error",
          color: "danger",
          icon: "exclamation-circle-fill",
        },
      },
    };
  },
  computed: {
    containerCard() {
      return containerCard;
    },
    containerStatus() {
      return containerStatus;
    },
    firstFreePort(): number {
      let port = 6311;
      while (this.containers.find((container) => container.port === port)) {
        port++;
      }
      return port;
    },
    firstFreeSeed(): string {
      let seed = 100000000;
      while (
        this.containers.find(
          (container) => container.datashieldSeed == seed.toString()
        )
      ) {
        seed++;
      }
      return String(seed);
    },
    containersDataStructure(): TypeObject {
      let columns: TypeObject = {
        name: "string",
        image: "string",
        versionId: "string",
        imageSize: "number",
        creationDate: "string",
        installDate: "string",
        autoUpdate: "boolean",
        updateSchedule: "object",
        port: "string",
        packageWhitelist: "array",
        functionBlacklist: "array",
        datashieldSeed: "string",
        options: "object",
      };

      if (this.dockerManagementEnabled) {
        columns["container"] = "object";
      }

      const toHideInEdit = [
        "container",
        "updateSchedule",
        "versionId",
        "imageSize",
        "creationDate",
        "installDate",
      ];

      if (this.containerToEditIndex !== -1) {
        toHideInEdit.forEach((key) => {
          delete columns[key as keyof TypeObject];
        });
      }

      return columns;
    },
  },
  watch: {
    containerToEdit() {
      this.containerToEditIndex = this.getEditIndex();
    },
    containers: {
      handler(newContainers) {
        newContainers.forEach((container: Container) => {
          if (
            container.updateSchedule &&
            container.updateSchedule.frequency === "daily"
          ) {
            container.updateSchedule.day = "";
          }
        });
      },
      deep: true,
    },
  },
  methods: {
    getContainerByName(name) {
      return this.containers.filter((container) => container.name === name)[0];
    },
    setEditContainer(container) {
      console.log(container);
      this.containerToEdit = container;
    },
    proceedDelete(containerName: string) {
      this.clearRecordToDelete();
      deleteContainer(containerName)
        .then(() => {
          this.successMessage = `[${containerName}] was successfully deleted.`;
          this.reloadContainers();
        })
        .catch((error) => {
          this.errorMessage = `Could not delete [${containerName}]: ${error}.`;
        });
    },
    clearRecordToDelete() {
      this.recordToDelete = "";
    },
    editContainer(container: Container) {
      this.containerToEdit = container.name;
    },
    saveEditedContainer() {
      this.clearUserMessages();
      const container: Container = this.containers[this.containerToEditIndex];
      const containerNames = this.containers.map((container) => {
        return container.name;
      });

      const imageParts = container.image.split(":");
      if (imageParts.length == 1) {
        this.errorMessage = `Save failed: [${container.image}] needs a version added. Try [${container.image}:latest]`;
        return;
      }
      if (imageParts.length > 2) {
        this.errorMessage = `Save failed: [${container.image}] needs a version added. Try [${imageParts[0]}:latest]`;
        return;
      }

      const hostPortCombo = `${container.host}:${container.port}`;

      const hasDuplicates = this.containers.some(
        (prof) =>
          prof !== container && `${prof.host}:${prof.port}` === hostPortCombo
      );

      if (hasDuplicates) {
        this.errorMessage = `Save failed: [${hostPortCombo}] already used.`;
        return;
      }

      if (
        this.containerToEdit === "default" &&
        container.name != this.containerToEdit
      ) {
        this.errorMessage = "Save failed: cannot rename 'default' package.";
        return;
      } else if (container.name === "") {
        this.errorMessage = "Cannot create container with empty name.";
        return;
      } else if (isDuplicate(container.name, containerNames)) {
        this.errorMessage = `Container with name [${container.name}] already exists.`;
        return;
      } else {
        this.proceedEdit(container);
      }
    },
    proceedEdit(container: Container) {
      this.addContainer = false;
      container.options["datashield.seed"] = container.datashieldSeed;
      //add/update
      this.loadingContainer = container.name;
      putContainer(container)
        .then(() => {
          this.successMessage = `[${container.name}] was successfully saved.`;
          this.clearContainerToEdit();
          this.containerToEditIndex = -1;
        })
        .catch((error) => {
          this.errorMessage = `Save failed: Could not save [${container.name}]: ${error}.`;
          this.clearLoading();
        });
      //check if new name
      if (this.containerToEdit && container.name !== this.containerToEdit) {
        deleteContainer(this.containerToEdit)
          .then(() => this.reloadContainers())
          .catch((error) => {
            this.errorMessage = `Could not rename: delete previous container [${container.name}]: ${error}.`;
            this.clearLoading();
          });
      }
    },
    removeContainer(containerName: String) {
      this.clearUserMessages();
      this.recordToDelete = containerName;
    },
    clearLoading() {
      this.loading = false;
      this.loadingContainer = "";
    },
    clearContainerToEdit() {
      this.reloadContainers();
      this.containerToEditIndex = -1;
      this.containerToEdit = "";
      this.addContainer = false;
    },
    getEditIndex() {
      const index = this.containers.findIndex((container: Container) => {
        return container.name === this.containerToEdit;
      });
      // only change when user is cleared, otherwise it will return -1 when name is altered
      if (this.containerToEdit === "" || index !== -1) {
        return index;
      } else return this.containerToEditIndex;
    },
    addNewContainer() {
      this.addContainer = true;
      this.clearUserMessages();

      this.containers.unshift({
        name: "",
        image: "datashield/rock-base:latest",
        versionId: "",
        autoUpdate: false,
        updateSchedule: {
          frequency: "daily",
          day: "",
          time: "03:00",
        },
        host: "localhost",
        port: this.firstFreePort,
        packageWhitelist: ["dsBase"],
        functionBlacklist: [],
        datashieldSeed: this.firstFreeSeed,
        options: {},
        container: { tags: [], status: "unknown" },
      });
      this.containerToEditIndex = 0;
    },
    clearUserMessages() {
      this.successMessage = "";
      this.errorMessage = "";
    },
    startDockerContainer(name: string) {
      this.clearUserMessages();
      this.loading = true;
      this.loadingContainer = name;
      this.startPolling(name);
      startContainer(name)
        .then(() => {
          this.successMessage = `[${name}] was successfully started.`;
          this.reloadContainers();
        })
        .catch((error) => {
          this.errorMessage = `Could not start [${name}]: ${error}.`;
          this.clearLoading();
        });
    },
    stopDockerContainer(name: string) {
      this.clearUserMessages();
      this.loading = true;
      this.loadingContainer = name;
      stopContainer(name)
        .then(() => {
          this.successMessage = `[${name}] was successfully stopped.`;
          this.reloadContainers();
        })
        .catch((error) => {
          this.errorMessage = `Could not stop [${name}]: ${error}.`;
          this.clearLoading();
          this.stopPolling();
        });
    },
    updateAutoUpdate(container: Container, currentValue: boolean) {
      container.autoUpdate = !currentValue;
      putContainer(container).catch((error) => {
        this.errorMessage = `Could not update auto-update for [${container.name}]: ${error}.`;
        // Revert checkbox on failure
        container.autoUpdate = currentValue;
      });
    },
    getOtherOptions(options) {
      return Object.keys(options)
        .filter((key) => !Object.keys(this.dsOptions).includes(key))
        .reduce((obj, key) => {
          obj[key] = options[key];
          return obj;
        }, {});
    },
    async reloadContainers() {
      this.loading = true;
      try {
        await this.loadContainers();
        this.clearLoading();
      } catch (error) {
        this.clearLoading();
        this.errorMessage = `Could not load containers: ${error}.`;
      }
    },
  },
});
</script>

<style scoped>
* {
  box-sizing: content-box !important;
}
</style>
