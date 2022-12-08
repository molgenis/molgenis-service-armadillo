<template>
  <div>
    <h2>Profiles</h2>
    <div class="row">
      <div class="col">
        <!-- Error messages will appear here -->
        <FeedbackMessage
          :successMessage="successMessage"
          :errorMessage="errorMessage"
        ></FeedbackMessage>
      </div>
    </div>
    <!-- Actual table -->
    <!-- Loading spinner -->
    <LoadingSpinner v-if="loading"></LoadingSpinner>
    <Table
      v-else
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
          >
            <i class="bi bi-plus-lg"></i>
          </button>
        </th>
      </template>
      <template #objectType="objectProps">
        <div v-if="objectProps.data.status">
          <span
            class="badge"
            :class="
              statusMapping[objectProps.data.status] === 'ONLINE'
                ? 'bg-success'
                : statusMapping[objectProps.data.status] === 'OFFLINE'
                ? 'bg-secondary'
                : 'bg-danger'
            "
          >
            {{ statusMapping[objectProps.data.status] }}
          </span>
          <button
            v-if="statusMapping[objectProps.data.status] === 'ONLINE'"
            href=""
            @click.prevent="stopProfile(objectProps.row.name)"
            class="btn btn-link pt-0 pb-0"
          >
            <i class="bi bi-stop-circle-fill"></i>
            <br/>
            Stop
          </button>
          <button
            v-else-if="statusMapping[objectProps.data.status] === 'OFFLINE'"
            @click.prevent="startProfile(objectProps.row.name)"
            class="btn btn-link pt-0 pb-0"
          >
            <i class="bi bi-play-circle-fill"></i>
            <br/>
            Start
          </button>
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
import Badge from "@/components/Badge.vue";
import { ProfilesData } from "@/types/types";
import { useRouter } from "vue-router";
import { isDuplicate } from "@/helpers/utils";

export default defineComponent({
  name: "Profiles",
  components: {
    Badge,
    FeedbackMessage,
    InlineRowEdit,
    LoadingSpinner,
    Table,
    ButtonGroup,
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
        if (error === "Unauthorized") {
          router.push("/login");
        } else {
          errorMessage.value = error;
        }
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
        whitelist: "array",
        options: "object",
        container: "object",
      },
      loading: false,
      successMessage: "",
      profileToEditIndex: -1,
      profileToEdit: "",
      statusMapping: {
        NOT_FOUND: "OFFLINE",
        NOT_RUNNING: "OFFLINE",
        RUNNING: "ONLINE",
        DOCKER_OFFLINE: "ERROR",
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
      putProfile(profile)
        .then(() => {
          this.successMessage = `[${profile.name}] was successfully saved.`;
          this.loadProfiles();
          this.profileToEditIndex = -1;
        })
        .catch(
          (error) =>
            (this.errorMessage = `Save failed: Could not save [${profile.name}]: ${error}.`)
        );
      //check if new name
      if (profile.name !== this.profileToEdit) {
        deleteProfile(this.profileToEdit)
          .then(() => this.loadProfiles())
          .catch(
            (error) =>
              (this.errorMessage = `Could not rename: delete previous profile [${profile.name}]: ${error}.`)
          );
      }
    },
    removeProfile(profile: Profile) {
      this.clearUserMessages();
      deleteProfile(profile.name)
        .then(() => {
          this.successMessage = `[${profile.name}] was successfully deleted.`;
          this.loadProfiles();
        })
        .catch((error) => {
          this.errorMessage = `Could not delete [${profile.name}]: ${error}.`;
        });
    },
    clearProfileToEdit() {
      this.loadProfiles();
      this.profileToEditIndex = -1;
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
        whitelist: ["dsBase"],
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
      startProfile(name)
        .then(() => {
          this.successMessage = `[${name}] was successfully started.`;
          this.loadProfiles();
          this.loading = false;
        })
        .catch((error) => {
          this.errorMessage = `Could not start [${name}]: ${error}.`;
          this.loading = false;
        });
    },
    stopProfile(name: string) {
      this.clearUserMessages();
      this.loading = true;
      stopProfile(name)
        .then(() => {
          this.successMessage = `[${name}] was successfully stopped.`;
          this.loadProfiles();
          this.loading = false;
        })
        .catch((error) => {
          this.errorMessage = `Could not stop [${name}]: ${error}.`;
          this.loading = false;
        });
    },
  },
});
</script>
