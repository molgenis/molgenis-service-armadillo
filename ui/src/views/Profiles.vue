<template>
  <div>
    <h2 class="mt-3">Profiles</h2>
    <div class="row">
      <div class="col">
        <!-- Error messages will appear here -->
        <FeedbackMessage
          :successMessage="successMessage"
          :errorMessage="errorMessage"
        ></FeedbackMessage>
        <ConfirmationDialog
          v-if="recordToDelete != ''"
          :record="recordToDelete"
          action="delete"
          recordType="project"
          @proceed="proceedDelete"
          @cancel="clearRecordToDelete"
        ></ConfirmationDialog>
      </div>
    </div>
    <!-- Actual table -->
    <Table
      :dataToShow="profiles"
      :allData="profiles"
      :indexToEdit="profileToEditIndex"
      :dataStructure="profilesDataStructure"
    >
      <template v-slot:extraHeader>
        <!-- Add extra header for buttons (add profile button) -->
        <th>
          <button
            type="button"
            class="btn btn-sm me-1 btn-primary bg-primary"
            @click="addNewProfile"
            :disabled="profileToEdit !== '' || profileToEditIndex === 0"
          >
            <i class="bi bi-plus-lg"></i>
          </button>
        </th>
      </template>
      <template #objectType="objectProps">
        <div v-if="objectProps.data.status" class="row">
          <div class="col-6">
            <span
              class="badge"
              :class="`bg-${statusMapping[objectProps.data.status].color}`"
            >
              {{ statusMapping[objectProps.data.status].status }}
            </span>
          </div>
          <div class="col-6">
            <ProfileStatus
              :disabled="true"
              v-if="objectProps.row.name === loadingProfile"
              :text="
                statusMapping[objectProps.data.status].status === 'OFFLINE'
                  ? 'Starting'
                  : 'Stopping'
              "
              icon="spinner"
            ></ProfileStatus>
            <ProfileStatus
              v-else
              :disabled="loading"
              :text="statusMapping[objectProps.data.status].text"
              @click.prevent="
                statusMapping[objectProps.data.status].status === 'ONLINE'
                  ? stopProfile(objectProps.row.name)
                  : startProfile(objectProps.row.name)
              "
              :icon="statusMapping[objectProps.data.status].icon"
            ></ProfileStatus>
          </div>
        </div>
        <div v-else>
          <div v-for="(value, key) in objectProps.data" :key="key">
            {{ key }} = {{ value }}
          </div>
        </div>
      </template>
      <template #extraColumn="columnProps">
        <!-- Add buttons for editing/deleting profiles -->
        <th scope="row">
          <ButtonGroup
            :buttonIcons="['pencil-fill', 'trash-fill']"
            :buttonColors="['primary', 'danger']"
            :clickCallbacks="[editProfile, removeProfile]"
            :callbackArguments="[columnProps.item, columnProps.item]"
            :disabled="profileToEdit !== '' || profileToEditIndex === 0"
          ></ButtonGroup>
        </th>
      </template>
      <template #editRow="rowProps">
        <InlineRowEdit
          :row="rowProps.row"
          :save="saveEditedProfile"
          :cancel="clearProfileToEdit"
          :hideColumns="['container']"
          :dataStructure="profilesDataStructure"
        />
      </template>
    </Table>
  </div>
</template>

<script lang="ts">
import ConfirmationDialog from "@/components/ConfirmationDialog.vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import { defineComponent, onMounted, Ref, ref } from "vue";
import { Profile } from "@/types/api";
import {
  deleteProfile,
  getProfiles,
  putProfile,
  startProfile,
  stopProfile,
} from "@/api/api";
import InlineRowEdit from "@/components/InlineRowEdit.vue";
import Table from "@/components/Table.vue";
import ButtonGroup from "@/components/ButtonGroup.vue";
import ProfileStatus from "@/components/ProfileStatus.vue";
import Badge from "@/components/Badge.vue";
import { ProfilesData } from "@/types/types";
import { useRouter } from "vue-router";
import { isDuplicate } from "@/helpers/utils";
import { processErrorMessages } from "@/helpers/errorProcessing";

export default defineComponent({
  name: "Profiles",
  components: {
    Badge,
    ConfirmationDialog,
    FeedbackMessage,
    InlineRowEdit,
    LoadingSpinner,
    Table,
    ButtonGroup,
    ProfileStatus,
  },
  setup() {
    const profiles: Ref<Profile[]> = ref([]);
    const errorMessage: Ref<string> = ref("");
    const router = useRouter();
    onMounted(() => {
      loadProfiles();
    });
    const loadProfiles = async () => {
      profiles.value = await getProfiles().catch((error: string) => {
        errorMessage.value = processErrorMessages(error, "profiles", router);
        return [];
      });
    };
    return {
      profiles,
      errorMessage,
      loadProfiles,
    };
  },
  data(): ProfilesData {
    return {
      profilesDataStructure: {
        name: "string",
        image: "string",
        host: "string",
        port: "string",
        packageWhitelist: "array",
        functionBlacklist: "array",
        options: "object",
        container: "object",
      },
      recordToDelete: "",
      loading: false,
      loadingProfile: "",
      successMessage: "",
      profileToEditIndex: -1,
      profileToEdit: "",
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
    firstFreePort(): number {
      let port = 6311;
      while (this.profiles.find((profile) => profile.port === port)) {
        port++;
      }
      return port;
    },
  },
  watch: {
    profileToEdit() {
      this.profileToEditIndex = this.getEditIndex();
    },
  },
  methods: {
    proceedDelete(profileName: string) {
      this.clearRecordToDelete();
      deleteProfile(profileName)
        .then(() => {
          this.successMessage = `[${profileName}] was successfully deleted.`;
          this.reloadProfiles();
        })
        .catch((error) => {
          this.errorMessage = `Could not delete [${profileName}]: ${error}.`;
        });
    },
    clearRecordToDelete() {
      this.recordToDelete = "";
    },
    editProfile(profile: Profile) {
      this.profileToEdit = profile.name;
    },
    saveEditedProfile() {
      this.clearUserMessages();
      const profile: Profile = this.profiles[this.profileToEditIndex];
      const profileNames = this.profiles.map((profile) => {
        return profile.name;
      });
      if (
        this.profileToEdit === "default" &&
        profile.name != this.profileToEdit
      ) {
        this.errorMessage = "Save failed: cannot rename 'default' package.";
        return;
      } else if (profile.name === "") {
        this.errorMessage = "Cannot create profile with empty name.";
        return;
      } else if (isDuplicate(profile.name, profileNames)) {
        this.errorMessage = `Profile with name [${profile.name}] already exists.`;
        return;
      }
      //add/update
      this.loadingProfile = profile.name;
      putProfile(profile)
        .then(() => {
          this.successMessage = `[${profile.name}] was successfully saved.`;
          this.reloadProfiles();
          this.profileToEditIndex = -1;
        })
        .catch((error) => {
          this.errorMessage = `Save failed: Could not save [${profile.name}]: ${error}.`;
          this.clearLoading();
        });
      //check if new name
      if (this.profileToEdit && profile.name !== this.profileToEdit) {
        deleteProfile(this.profileToEdit)
          .then(() => this.reloadProfiles())
          .catch((error) => {
            this.errorMessage = `Could not rename: delete previous profile [${profile.name}]: ${error}.`;
            this.clearLoading();
          });
      }
    },
    removeProfile(profile: Profile) {
      this.clearUserMessages();
      this.recordToDelete = profile.name;
    },
    clearLoading() {
      this.loading = false;
      this.loadingProfile = "";
    },
    clearProfileToEdit() {
      this.reloadProfiles();
      this.profileToEditIndex = -1;
      this.profileToEdit = "";
    },
    getEditIndex() {
      const index = this.profiles.findIndex((profile: Profile) => {
        return profile.name === this.profileToEdit;
      });
      // only change when user is cleared, otherwise it will return -1 when name is altered
      if (this.profileToEdit === "" || index !== -1) {
        return index;
      } else return this.profileToEditIndex;
    },
    addNewProfile() {
      this.clearUserMessages();

      this.profiles.unshift({
        name: "",
        image: "molgenis/armadillo:latest",
        host: "localhost",
        port: this.firstFreePort,
        packageWhitelist: ["dsBase"],
        functionBlacklist: [],
        options: {},
        container: { tags: [], status: "unknown" },
      });
      this.profileToEditIndex = 0;
    },
    clearUserMessages() {
      this.successMessage = "";
      this.errorMessage = "";
    },
    startProfile(name: string) {
      this.clearUserMessages();
      this.loading = true;
      this.loadingProfile = name;
      startProfile(name)
        .then(() => {
          this.successMessage = `[${name}] was successfully started.`;
          this.reloadProfiles();
        })
        .catch((error) => {
          this.errorMessage = `Could not start [${name}]: ${error}.`;
          this.clearLoading();
        });
    },
    stopProfile(name: string) {
      this.clearUserMessages();
      this.loading = true;
      this.loadingProfile = name;
      stopProfile(name)
        .then(() => {
          this.successMessage = `[${name}] was successfully stopped.`;
          this.reloadProfiles();
        })
        .catch((error) => {
          this.errorMessage = `Could not stop [${name}]: ${error}.`;
          this.clearLoading();
        });
    },
    async reloadProfiles() {
      this.loading = true;
      try {
        await this.loadProfiles();
        this.clearLoading();
      } catch (error) {
        this.clearLoading();
        this.errorMessage = `Could not load profiles: ${error}.`;
      }
    },
  },
});
</script>
