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
          extraInfo="The website will be down for a short period of time. Refresh until it's back up."
        ></ConfirmationDialog>
        <ConfirmationDialog
          v-if="updateOidcTriggered"
          record="oidc config"
          action="update"
          recordType="the authentication server"
          @proceed="saveOidcConfig"
          @cancel="cancelOidcUpdate"
          extraInfo="Another authentication server might be used when proceeding, meaning that current users possibly cannot login if they are not registered to the new oidc config."
        ></ConfirmationDialog>
      </div>
    </div>
    <div class="row">
      <div class="col-12">
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
        />
        <OidcConfig
          v-else-if="!isLoading"
          ref="updatedOidcConfig"
          @saveOidcConfig="askIfSureUpdateOidc"
          presetClientId=""
          presetClientSecret=""
          presetServerUri=""
        />
        <div class="card mt-2 mb-2">
          <h5 class="card-header">Armadillo server</h5>
          <div class="card-body">
            <button
              class="btn btn-warning"
              @click="isRestartServerPushed = true"
            >
              <i class="bi bi-arrow-repeat"></i> Restart Server
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import ConfirmationDialog from "@/components/ConfirmationDialog.vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import { useRouter } from "vue-router";
import { defineComponent, onBeforeMount, Ref, ref } from "vue";
import { processErrorMessages } from "@/helpers/errorProcessing";
import {
  getAuthServerConfig,
  putAuthServerConfig,
  restartServer,
} from "@/api/api";
import { AuthServerConfig } from "@/types/api";
import OidcConfig from "@/components/OidcConfig.vue";

export default defineComponent({
  name: "System",
  components: {
    ConfirmationDialog,
    FeedbackMessage,
    LoadingSpinner,
    OidcConfig,
  },
  setup() {
    const router = useRouter();
    const authConfig: Ref<AuthServerConfig> = ref({});
    const isLoading: Ref<Boolean> = ref(true);
    const errorMessage: Ref<string> = ref("");

    onBeforeMount(() => {
      loadAuthConfig();
    });
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
    return {
      authConfig,
      errorMessage,
      isLoading,
    };
  },
  data() {
    return {
      successMessage: "",
      warningMessage: "",
      updateOidcTriggered: false,
      isRestartServerPushed: false,
      configToSave: {},
    };
  },
  methods: {
    proceedRestartServer() {
      this.warningMessage =
        "Server will restart now. Please refresh and log back in.";
      restartServer();
    },
    cancelRestartServer() {
      this.isRestartServerPushed = false;
    },
    cancelOidcUpdate() {
      this.updateOidcTriggered = false;
    },
    askIfSureUpdateOidc(event: Event) {
      this.updateOidcTriggered = true;
      this.configToSave = event;
    },
    saveOidcConfig() {
      putAuthServerConfig(this.configToSave)
        .then(() => {
          this.successMessage = "Successfully updated OIDC config";
          this.authConfig = this.configToSave;
          this.cancelOidcUpdate();
        })
        .catch((errrorMsg) => {
          this.errorMessage = `Cannot update OIDC config, because: ${errrorMsg}.`;
          this.cancelOidcUpdate();
        });
    },
  },
});
</script>
