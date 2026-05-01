<template>
  <div>
    <div class="row">
      <div class="col">
        <!-- Error messages will appear here -->
        <FeedbackMessage
          :successMessage="successMessage"
          :errorMessage="errorMessage"
          :warningMessage="warningMessage"
        ></FeedbackMessage>
        <ConfirmationDialog
          v-if="isRestartServerPushed"
          record="armadillo"
          action="restart"
          recordType="application"
          @proceed="proceedRestartServer"
          @cancel="cancelRestartServer"
          extraInfo="The website will be down for a short period of time. Try refreshing until it is back up. In the very unlikely case your server doesn't come back up, contact your administrator."
        ></ConfirmationDialog>
        <ConfirmationDialog
          v-if="updateOidcTriggered"
          record="oidc config"
          action="update"
          recordType="the authentication server"
          @proceed="saveOidcConfig"
          @cancel="cancelOidcUpdate"
          extraInfo="Changing the authentication settings can affect whether users can login. If you change the authentication server, ensure that all of your current users are registered in the new server so that they continue to have access. After configuration is updated, the application will be restarted. In the very unlikely case that the application doesn't come back up, please contact your administrator."
        ></ConfirmationDialog>
        <ConfirmationDialog
          v-if="deleteJarTriggered"
          :record="appToDelete"
          action="delete"
          recordType="application jar"
          @proceed="deleteJar"
          @cancel="cancelDeleteJar"
        >
        </ConfirmationDialog>
      </div>
    </div>
    <div class="row">
      <div class="col-12">
        <div class="card mt-2 mb-2">
          <h5 class="card-header">
            <i class="bi bi-window-fullscreen"></i> Application
          </h5>
          <div class="card-body">
            <Alert type="info" :dismissible="false">
              Update available: v5.12.2
              <div class="pb-0">
                <button class="btn btn-sm btn-primary mt-2">
                  <i class="bi bi-download"></i> Download update
                </button>
              </div>
            </Alert>
            <div class="row">
              <div class="col-8 mt-1">
                <div class="progress">
                  <div
                    class="progress-bar progress-bar-striped progress-bar-animated"
                    role="progressbar"
                    aria-label="Animated striped example"
                    aria-valuenow="75"
                    aria-valuemin="0"
                    aria-valuemax="100"
                    style="width: 75%"
                  ></div>
                </div>
              </div>
              <div class="col-4 fst-italic mb-2">
                Downloading molgenis-armadillo-5.12.2.jar
              </div>
            </div>
            <button
              class="btn btn-warning"
              @click="isRestartServerPushed = true"
            >
              <i class="bi bi-arrow-repeat"></i> Soft restart
            </button>
            <button
              class="btn btn-warning"
              @click="isRestartServerPushed = true"
            >
              <i class="bi bi-arrow-repeat"></i> Hard restart
            </button>
          </div>
        </div>
        <OidcConfig
          v-if="
            !isLoading &&
            authConfig.issuerUri &&
            authConfig.clientId &&
            authConfig.clientSecret
          "
          ref="updatedOidcConfig"
          @saveOidcConfig="askIfSureUpdateOidc"
          :presetClientId="authConfig.clientId"
          :presetClientSecret="authConfig.clientSecret"
          :presetServerUri="authConfig.issuerUri"
          :key="reloadOidc"
        />
        <OidcConfig
          v-else-if="!isLoading"
          ref="updatedOidcConfig"
          @saveOidcConfig="askIfSureUpdateOidc"
          presetClientId=""
          presetClientSecret=""
          presetServerUri=""
        />
        <Storage
          :appList="appList"
          :freeDiskSpace="freeDiskSpace ? freeDiskSpace : undefined"
          :totalDiskSpace="totalDiskSpace ? totalDiskSpace : undefined"
          :currentVersion="currentVersion"
          @triggerDelete="askIfSureDeleteJar"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import ConfirmationDialog from "@/components/ConfirmationDialog.vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import DiskSpace from "@/components/DiskSpace.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import Storage from "@/components/Storage.vue";
import { useRouter } from "vue-router";
import { defineComponent, onBeforeMount, Ref, ref } from "vue";
import { processErrorMessages } from "@/helpers/errorProcessing";
import {
  getAuthServerConfig,
  putAuthServerConfig,
  getFreeDiskSpace,
  getTotalDiskSpace,
  restartServer,
  getAppList,
  deleteApplicationJar,
  getVersion,
} from "@/api/api";
import { AuthServerConfig } from "@/types/api";
import OidcConfig from "@/components/OidcConfig.vue";
import Alert from "@/components/Alert.vue";

export default defineComponent({
  name: "System",
  components: {
    ConfirmationDialog,
    FeedbackMessage,
    LoadingSpinner,
    OidcConfig,
    Alert,
    DiskSpace,
    Storage,
  },
  setup() {
    const router = useRouter();
    const authConfig: Ref<AuthServerConfig> = ref({});
    const isLoading: Ref<Boolean> = ref(true);
    const errorMessage: Ref<string> = ref("");
    const freeDiskSpace: Ref<number | undefined> = ref();
    const totalDiskSpace: Ref<number | undefined> = ref();
    const appList: Ref<Array<string>> = ref([]);
    const currentVersion: Ref<string> = ref("");

    onBeforeMount(() => {
      loadAuthConfig();
      loadDiskSpace();
      loadAppList();
      getAppList();
      loadVersion();
    });
    const loadVersion = async () => {
      currentVersion.value = await getVersion().catch((error: string) => {
        errorMessage.value = processErrorMessages(error, "version", router);
        return "";
      });
    };
    const loadAuthConfig = async () => {
      authConfig.value = await getAuthServerConfig()
        .catch((error: string) => {
          errorMessage.value = processErrorMessages(error, "config", router);
          return {};
        })
        .then((value: AuthServerConfig) => {
          isLoading.value = false;
          return value;
        });
    };
    const loadAppList = async () => {
      appList.value = await getAppList().catch((error: string) => {
        errorMessage.value = processErrorMessages(error, "app list", router);
        return {};
      });
    };
    const loadDiskSpace = async () => {
      freeDiskSpace.value = await getFreeDiskSpace().catch((error: string) => {
        errorMessage.value = processErrorMessages(
          error,
          "free diskspace",
          router
        );
        return undefined;
      });
      totalDiskSpace.value = await getTotalDiskSpace().catch(
        (error: string) => {
          errorMessage.value = processErrorMessages(
            error,
            "total diskspace",
            router
          );
          return undefined;
        }
      );
    };
    return {
      authConfig,
      errorMessage,
      freeDiskSpace,
      totalDiskSpace,
      appList,
      isLoading,
      currentVersion,
    };
  },
  data() {
    return {
      successMessage: "",
      warningMessage: "",
      updateOidcTriggered: false,
      isRestartServerPushed: false,
      configToSave: {},
      reloadOidc: 0,
      appToDelete: "",
      deleteJarTriggered: false,
    };
  },
  methods: {
    proceedRestartServer() {
      this.warningMessage =
        "Server will restart now. Please refresh and log back in. If the application is not reloaded, please contact your administrator.";
      restartServer();
    },
    cancelRestartServer() {
      this.isRestartServerPushed = false;
    },
    cancelOidcUpdate() {
      this.updateOidcTriggered = false;
      this.reloadOidc++;
    },
    askIfSureUpdateOidc(event: Event) {
      this.updateOidcTriggered = true;
      this.configToSave = event;
    },
    askIfSureDeleteJar(event: Event) {
      this.deleteJarTriggered = true;
      this.appToDelete = String(event);
    },
    deleteJar() {
      deleteApplicationJar(this.appToDelete);
    },
    cancelDeleteJar() {
      this.deleteJarTriggered = false;
    },
    saveOidcConfig() {
      putAuthServerConfig(this.configToSave)
        .then(() => {
          this.successMessage = "Successfully updated OIDC config";
          this.authConfig = this.configToSave;
          this.cancelOidcUpdate();
          this.proceedRestartServer();
        })
        .catch((errrorMsg) => {
          this.errorMessage = `Cannot update OIDC config, because: ${errrorMsg}.`;
          this.cancelOidcUpdate();
        });
    },
  },
});
</script>
