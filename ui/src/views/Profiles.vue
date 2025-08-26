<template>
  <div>
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

    <LoadingSpinner v-if="profilesLoading" class="mt-5" />
    <!-- Actual table -->
    <Table
      v-else
      :dataToShow="profiles"
      :allData="profiles"
      :indexToEdit="profileToEditIndex"
      :dataStructure="profilesDataStructure"
      :isSmall="true"
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
          class="row p-0"
        >
          <div class="col-6 p-0">
            <span
              class="badge mt-3"
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
          <div class="col-6 p-0">
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
        <div
          v-else-if="
            objectProps.row.autoUpdate &&
            objectProps.data &&
            objectProps.data.frequency
          "
        >
          <span>
            {{
              objectProps.data.frequency === "daily"
                ? `Daily at ${objectProps.data.time}`
                : `Weekly, ${objectProps.data.day} at ${objectProps.data.time}`
            }}
          </span>
        </div>
        <div
          v-else-if="
            !objectProps.row.autoUpdate &&
            objectProps.data &&
            'frequency' in objectProps.data &&
            'day' in objectProps.data &&
            'time' in objectProps.data
          "
        ></div>
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
          :hideColumns="[
            'versionId',
            'autoUpdateSchedule',
            'container',
            'autoUpdateSchedule',
          ]"
          :dataStructure="profilesDataStructure"
        />
        <tr v-if="rowProps.row.autoUpdate">
          <td colspan="100%">
            <strong>Auto-update schedule:</strong>
            <div
              class="form-check form-check-inline"
              v-for="option in ['daily', 'weekly']"
              :key="option"
            >
              <input
                class="form-check-input"
                type="radio"
                :id="`freq-${option}`"
                :value="option"
                v-model="rowProps.row.autoUpdateSchedule.frequency"
              />
              <label class="form-check-label" :for="`freq-${option}`">
                {{ option }}
              </label>
            </div>
            <div class="mt-2">
              <label class="form-label me-2">Day:</label>
              <select
                v-model="rowProps.row.autoUpdateSchedule.day"
                class="form-select d-inline-block w-auto"
                :disabled="
                  rowProps.row.autoUpdateSchedule.frequency === 'daily'
                "
              >
                <option value="" disabled>Select day</option>
                <option
                  v-for="day in [
                    'Sunday',
                    'Monday',
                    'Tuesday',
                    'Wednesday',
                    'Thursday',
                    'Friday',
                    'Saturday',
                  ]"
                  :key="day"
                  :value="day"
                >
                  {{ day }}
                </option>
              </select>
              <label class="form-label ms-3 me-2">Time:</label>
              <input
                type="time"
                v-model="rowProps.row.autoUpdateSchedule.time"
                class="form-control d-inline-block w-auto"
                :disabled="!rowProps.row.autoUpdate"
              />
            </div>
          </td>
        </tr>
      </template>
      <template #boolType="boolProps">
        <input
          class="form-check-input"
          type="checkbox"
          :checked="boolProps.data"
          @change="updateAutoUpdate(boolProps.row, boolProps.data)"
          :disabled="profileToEditIndex !== profiles.indexOf(boolProps.row)"
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
import { convertBytes } from "@/helpers/utils";

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
    const profilesLoading: Ref<Boolean> = ref(true);
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

          return profiles.map((profile) => {
            // Extract datashieldSeed
            const datashieldSeed = profile.options["datashield.seed"];
            delete profile.options["datashield.seed"];

            return {
              ...profile,
              datashieldSeed,
              autoUpdateSchedule: profile.autoUpdateSchedule || {
                frequency: "weekly",
                day: "Sunday",
                time: "01:00",
              },
              ImageSize: profile.imageSize
                ? convertBytes(profile.imageSize)
                : "",
              CreationDate: profile.creationDate
                ? new Date(profile.creationDate).toLocaleDateString()
                : "",
              InstallDate: profile.installDate
                ? new Date(profile.installDate).toLocaleDateString()
                : "",
            };
          });
        })
        .catch((error: string) => {
          errorMessage.value = processErrorMessages(error, "profiles", router);
          return [];
        });

      profilesLoading.value = false;
    };
    return {
      profilesLoading,
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
        name: "string",
        image: "string",
        versionId: "string",
        ImageSize: "number",
        CreationDate: "string",
        InstallDate: "string",
        autoUpdate: "boolean",
        autoUpdateSchedule: "object",
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
        "autoUpdateSchedule",
        "versionId",
        "ImageSize",
        "CreationDate",
        "InstallDate",
      ];

      if (this.profileToEditIndex !== -1) {
        toHideInEdit.forEach((key) => {
          delete columns[key as keyof TypeObject];
        });
      }

      return columns;
    },
  },
  watch: {
    profileToEdit() {
      this.profileToEditIndex = this.getEditIndex();
    },
    profiles: {
      handler(newProfiles) {
        newProfiles.forEach((profile: Profile) => {
          if (
            profile.autoUpdateSchedule &&
            profile.autoUpdateSchedule.frequency === "daily"
          ) {
            profile.autoUpdateSchedule.day = "";
          }
        });
      },
      deep: true,
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
        name: "",
        image: "datashield/rock-base:latest",
        versionId: "",
        autoUpdate: false,
        autoUpdateSchedule: {
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
    updateAutoUpdate(profile: Profile, currentValue: boolean) {
      profile.autoUpdate = !currentValue;
      putProfile(profile).catch((error) => {
        this.errorMessage = `Could not update auto-update for [${profile.name}]: ${error}.`;
        // Revert checkbox on failure
        profile.autoUpdate = currentValue;
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

<style scoped>
* {
  box-sizing: content-box !important;
}
</style>
