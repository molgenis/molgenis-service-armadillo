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
    <Table v-else
           :dataToShow="(profiles as ListOfObjectsWithStringKey)"
           :allData="profiles"
           :indexToEdit="profileToEditIndex"
    >
      <template v-slot:extraHeader>
        <!-- Add extra header for buttons (add profile button) -->
        <th>
          <button
              type="button"
              class="btn btn-sm me-1 btn-primary bg-primary"
              @click="addNewProfile"
          >
            <i class="bi bi-plus"></i>
          </button>
        </th>
      </template>
      <template #objectType="objectProps">
        <div v-if="objectProps.data.status">
          <div v-if="objectProps.data.status === 'RUNNING'">
          <span class="badge bg-success">
            {{ objectProps.data.status }}
          </span>
            <a href="" @click.prevent="stopProfile(objectProps.row.name)" class="p-2">stop</a>
          </div>
          <div v-else-if="objectProps.data.status === 'NOT_RUNNING'">
             <span class="badge bg-warning text-dark">
            {{ objectProps.data.status }}
          </span>
            <a href="" @click.prevent="startProfile(objectProps.row.name)" class="p-2">start</a>
          </div>
          <div v-else-if="objectProps.data.status === 'NOT_FOUND'">
             <span class="badge bg-danger">
            {{ objectProps.data.status }}
          </span>
            <a href="" @click.prevent="startProfile(objectProps.row.name)" class="p-2">start</a>
          </div>
          <div v-else>
             <span class="badge bg-dark">
            {{ objectProps.data.status }}
          </span>
          </div>
        </div>
        <div v-else>
          <div v-for="(value,key) in objectProps.data ">
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
              :callbackArguments="[
              columnProps.item,
              columnProps.item,
            ]"
          ></ButtonGroup>
        </th>
      </template>
      <template #editRow="rowProps">
        <TableRowEditor
            :rowToEdit="rowProps.row"
            arrayColumn="whitelist"
            :saveCallback="saveEditedProfile"
            :cancelCallback="clearProfileToEdit"
            :hideColumns="['container']"
        ></TableRowEditor>
      </template>
    </Table>
  </div>
</template>

<script lang="ts">
import LoadingSpinner from "../components/LoadingSpinner.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import {defineComponent, onMounted, Ref, ref} from "vue";
import {Profile} from "@/types/api";
import {deleteProfile, getProfiles, putProfile, startProfile, stopProfile} from "@/api/api";
import TableRowEditor from "@/components/TableRowEditor.vue";
import Table from "@/components/Table.vue";
import ButtonGroup from "@/components/ButtonGroup.vue";
import Badge from "@/components/Badge.vue";
import { ListOfObjectsWithStringKey } from "@/types/types";

export default defineComponent({
  name: "Profiles",
  components: {
    Badge,
    FeedbackMessage,
    LoadingSpinner,
    TableRowEditor,
    Table, ButtonGroup
  },
  setup() {
    const profiles: Ref<Profile[]> = ref([]);
    onMounted(() => {
      loadProfiles();
    });
    const loadProfiles = async () => {
      profiles.value = await getProfiles();
    };
    return {
      profiles,
      loadProfiles,
    };
  },
  data() {
    return {
      errorMessage: "",
      loading: false,
      successMessage: "",
      profileToEditIndex: -1,
      profileToEdit: "",
    };
  },
  computed: {
    firstFreePort(): number {
      var port = 6311;
      while (this.profiles.find(profile => profile.port === port)) {
        port++;
      }
      return port;
    }
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
      if (this.profileToEdit === 'default' && profile.name != this.profileToEdit) {
        this.errorMessage = "Save failed: cannot rename 'default' package";
        return;
      }
      //add/update
      putProfile(profile)
          .then(() => {
            this.successMessage = `[${profile.name}] was successfully saved.`;
            this.loadProfiles();
            this.profileToEditIndex = -1;
          })
          .catch(error => this.errorMessage = `Save failed: Could not save [${profile.name}]: ${error}.`);
      //check if new name
      if (profile.name !== this.profileToEdit) {
        deleteProfile(this.profileToEdit).then(() => this.loadProfiles()).catch(error => this.errorMessage = `Could not rename: delete previous profile [${profile.name}]: ${error}.`);
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
        whitelist: ['dsBase'],
        options: {},
        container: {tags: [], status: "unknown"}
      })
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
            this.loading = false
          })
          .catch(error => {
            this.errorMessage = `Could not start [${name}]: ${error}.`;
            this.loading = false
            ;
          });
    },
    stopProfile(name: string) {
      this.clearUserMessages();
      this.loading = true;
      stopProfile(name)
          .then(() => {
            this.successMessage = `[${name}] was successfully stopped.`;
            this.loadProfiles();
            this.loading = false
          })
          .catch(error => {
            this.errorMessage = `Could not stop [${name}]: ${error}.`;
            this.loading = false
          });
    },
  },
});
</script>
