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
          v-if="recordToDelete !== ''"
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
        <div
          v-if="
            objectProps.data &&
            statusMapping[objectProps.data.status as keyof typeof statusMapping]
          "
          class="row"
        >
          <div class="col-6">
            <span
              class="badge"
              :class="`bg-${
                statusMapping[
                  objectProps.data.status as keyof typeof statusMapping
                ].color
              }`"
            >
              {{
                statusMapping[
                  objectProps.data.status as keyof typeof statusMapping
                ].status
              }}
            </span>
          </div>
          <div class="col-6">
            <ProfileStatus
              :disabled="true"
              v-if="objectProps.row.name === loadingProfile"
              :text="
                statusMapping[
                  objectProps.data.status as keyof typeof statusMapping
                ].status === 'OFFLINE'
                  ? 'Starting'
                  : 'Stopping'
              "
              icon="spinner"
            ></ProfileStatus>
            <ProfileStatus
              v-else
              :disabled="loading"
              :text="
                statusMapping[
                  objectProps.data.status as keyof typeof statusMapping
                ].text
              "
              @click.prevent="
                statusMapping[
                  objectProps.data.status as keyof typeof statusMapping
                ].status === 'ONLINE'
                  ? stopProfile(objectProps.row.name)
                  : startProfile(objectProps.row.name)
              "
              :icon="
                statusMapping[
                  objectProps.data.status as keyof typeof statusMapping
                ].icon
              "
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
          :immutable="addProfile ? [] : ['name']"
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
import { ProfilesData, TypeObject } from "@/types/types";
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
    const dockerManagementEnabled: Ref<boolean> = ref(false);
    const router = useRouter();
    onMounted(async () => {
      await loadProfiles();
    });
    const loadProfiles = async () => {
      profiles.value = await getProfiles()
        .then((profiles) => {
          dockerManagementEnabled.value = "container" in profiles[0];
          for (var profile_index in profiles) {
            // Extract options.datashield.seed into proper column
            profiles[profile_index].datashieldSeed =
              profiles[profile_index].options["datashield.seed"];
            // Delete required or else shows when creating or editing profiles
            delete profiles[profile_index].options["datashield.seed"];
          }
          return profiles;
        })
        .catch((error: string) => {
          errorMessage.value = processErrorMessages(error, "profiles", router);
          return [];
        });
    };
    return {
      profiles,
      errorMessage,
      loadProfiles,
      dockerManagementEnabled,
    };
  },
  data(): ProfilesData {
    return {
      addProfile: false,
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
    firstFreeSeed(): string {
      let seed = 100000000;
      while (
        this.profiles.find(
          (profile) => profile.datashieldSeed == seed.toString()
        )
      ) {
        seed++;
      }
      return String(seed);
    },
    profilesDataStructure(): TypeObject {
      let columns: TypeObject = {
        autoStart: "boolean",
        name: "string",
        image: "string",
        host: "string",
        port: "string",
        packageWhitelist: "array",
        functionBlacklist: "array",
        datashieldSeed: "string",
        options: "object",
      };

      if (this.dockerManagementEnabled) {
        columns["container"] = "object";
      }

      return columns;
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

      const imageParts = profile.image.split(":");
      if (imageParts.length == 1) {
        this.errorMessage = `Save failed: [${profile.image}] needs a version added. Try [${profile.image}:latest]`;
        return;
      }
      if (imageParts.length > 2) {
        this.errorMessage = `Save failed: [${profile.image}] needs a version added. Try [${imageParts[0]}:latest]`;
        return;
      }

      const hostPortCombo = `${profile.host}:${profile.port}`;

      const hasDuplicates = this.profiles.some(
        (prof) =>
          prof !== profile && `${prof.host}:${prof.port}` === hostPortCombo
      );

      if (hasDuplicates) {
        this.errorMessage = `Save failed: [${hostPortCombo}] already used.`;
        return;
      }

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
      } else {
        this.proceedEdit(profile);
      }
    },
    proceedEdit(profile: Profile) {
      this.addProfile = false;
      profile.options["datashield.seed"] = profile.datashieldSeed;
      //add/update
      this.loadingProfile = profile.name;
      putProfile(profile)
        .then(() => {
          this.successMessage = `[${profile.name}] was successfully saved.`;
          this.clearProfileToEdit();
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
      this.addProfile = false;
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
      this.addProfile = true;
      this.clearUserMessages();

      this.profiles.unshift({
        autoStart: true,
        name: "",
        image: "datashield/rock-base:latest",
        host: "localhost",
        port: this.firstFreePort,
        packageWhitelist: ["dsBase"],
        functionBlacklist: [],
        datashieldSeed: this.firstFreeSeed,
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
