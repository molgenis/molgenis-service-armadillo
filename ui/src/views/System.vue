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
          v-if="softRestartTriggered || hardRestartTriggered"
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
          v-if="updateAppTriggered"
          record="application"
          action="update"
          recordType="armadillo"
          @proceed="updateApplication"
          @cancel="cancelAppUpdate"
          :extraInfo="`The application will be updated to version [${versionToUpdateTo}]. The application will be restarted. In the very unlikely case that the application doesn't come back up, please contact your administrator. If you decide to proceed, try refreshing until the application is back up.`"
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
        <ApplicationControl
          :currentReleaseVersion="currentVersion"
          :latestReleaseVersion="latestReleaseVersion"
          :latestVersionDownloaded="latestVersionDownloaded"
          :appList="appList"
          ref="appControl"
          @error="putErrorMessage"
          @download-done="loadAppList"
          @update-app="triggerUpdate"
          @hard-restart-pushed="triggerHardRestart"
          @soft-restart-pushed="triggerSoftRestart"
        />
        <Storage
          :appList="appList"
          :freeDiskSpace="freeDiskSpace ? freeDiskSpace : undefined"
          :totalDiskSpace="totalDiskSpace ? totalDiskSpace : undefined"
          :currentVersion="currentVersion"
          @triggerDelete="askIfSureDeleteJar"
        />
        <OidcConfig
          v-if="
            !isLoading &&
            authConfig.issuerUri &&
            authConfig.clientId &&
            authConfig.clientSecret &&
            authConfig.deviceClientId &&
            authConfig.deviceIssuerUri
          "
          ref="updatedOidcConfig"
          @saveOidcConfig="askIfSureUpdateOidc"
          :presetClientId="authConfig.clientId"
          :presetClientSecret="authConfig.clientSecret"
          :presetServerUri="authConfig.issuerUri"
          :presetDeviceServerUri="authConfig.deviceIssuerUri"
          :presetDeviceClientId="authConfig.deviceClientId"
          :key="reloadOidc"
        />
        <OidcConfig
          v-else-if="!isLoading"
          ref="updatedOidcConfig"
          @saveOidcConfig="askIfSureUpdateOidc"
          presetClientId=""
          presetClientSecret=""
          presetServerUri=""
          presetDeviceServerUri=""
          presetDeviceClientId=""
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
  getAppList,
  deleteApplicationJar,
  getVersion,
  getLatestReleaseInfo,
  downloadUpdater,
  startUpdate,
  softRestartServer,
  hardRestartServer,
} from "@/api/api";
import { AuthServerConfig } from "@/types/api";
import OidcConfig from "@/components/OidcConfig.vue";
import ApplicationControl from "@/components/ApplicationControl.vue";
import { getJarFromVersion } from "@/helpers/utils";

export default defineComponent({
  name: "System",
  components: {
    ConfirmationDialog,
    FeedbackMessage,
    LoadingSpinner,
    OidcConfig,
    DiskSpace,
    Storage,
    ApplicationControl,
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
    const latestReleaseInfo: Ref<string> = ref("");

    onBeforeMount(() => {
      loadAuthConfig();
      loadDiskSpace();
      loadAppList();
      getAppList();
      loadVersion();
      loadLatestReleaseInfo();
    });
    const loadVersion = async () => {
      currentVersion.value = await getVersion().catch((error: string) => {
        errorMessage.value = processErrorMessages(error, "version", router);
        return "";
      });
    };
    const loadLatestReleaseInfo = async () => {
      latestReleaseInfo.value = await getLatestReleaseInfo().catch(
        (error: string) => {
          console.log(error);
          errorMessage.value = processErrorMessages(error, "version", router);
          return "";
        }
      );
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
      latestReleaseInfo,
      loadAppList,
    };
  },
  data() {
    return {
      successMessage: "",
      warningMessage: "",
      updateOidcTriggered: false,
      configToSave: {},
      reloadOidc: 0,
      appToDelete: "",
      deleteJarTriggered: false,
      versionToUpdateTo: "",
      updateAppTriggered: false,
      softRestartTriggered: false,
      hardRestartTriggered: false,
    };
  },
  computed: {
    latestReleaseVersion() {
      return (this.latestReleaseInfo as any).tag_name;
    },
    latestVersionDownloaded() {
      if (this.latestReleaseVersion !== undefined) {
        return this.appList.includes(
          getJarFromVersion(this.latestReleaseVersion)
        );
      } else {
        return false;
      }
    },
  },
  methods: {
    downloadUpdater,
    startUpdate,
    triggerHardRestart() {
      this.hardRestartTriggered = true;
    },
    triggerSoftRestart() {
      this.softRestartTriggered = true;
    },
    updateApplication() {
      if (this.versionToUpdateTo !== this.currentVersion) {
        console.log("update app", this.currentVersion, this.versionToUpdateTo);
        this.downloadUpdater(this.versionToUpdateTo);
        this.warningMessage =
          "Update in progress. The website will be down for a short period of time. Try refreshing until it is back up. In the very unlikely case your server doesn't come back up, contact your administrator.";
        this.startUpdate(this.versionToUpdateTo);
      } else {
        this.warningMessage = `Cannot update: Version [${this.versionToUpdateTo}] already running.`;
      }
    },
    triggerUpdate(version: Event) {
      this.updateAppTriggered = true;
      this.versionToUpdateTo = version.toString();
    },
    cancelAppUpdate() {
      this.updateAppTriggered = false;
      this.versionToUpdateTo = "";
    },
    putErrorMessage(e: Event) {
      this.errorMessage = String(e);
    },
    proceedRestartServer() {
      this.warningMessage =
        "Server will restart now. Please refresh and log back in. If the application is not reloaded, please contact your administrator.";
      if (this.softRestartTriggered) {
        softRestartServer();
      }
      if (this.hardRestartTriggered) {
        hardRestartServer();
      }
    },
    cancelRestartServer() {
      this.hardRestartTriggered = false;
      this.softRestartTriggered = false;
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
      deleteApplicationJar(this.appToDelete).then(() => {
        this.successMessage = "Succesfully deleted: " + this.appToDelete;
        this.appToDelete = "";
        this.loadAppList();
      });
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
