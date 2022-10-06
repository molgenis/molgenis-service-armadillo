<template>
  <div>
    <div class="row">
      <div class="col">
        <!-- Error messages will appear here -->
        <FeedbackMessage
          :successMessage="successMessage"
          :errorMessage="errorMessage"
        ></FeedbackMessage>
        <!-- Loading spinner -->
        <LoadingSpinner v-if="loading"></LoadingSpinner>
      </div>
    </div>
    <div class="row">
      <div class="col-0 col-sm-9"></div>
      <div class="col-12 col-sm-3">
        <SearchBar class="mt-1" v-model="searchString" />
      </div>
    </div>
    <!-- Actual table -->
    <Table
      :dataToShow="filteredAndSortedProjects"
      :allData="projects"
      idCol="name"
      :indexToEdit="projectToEditIndex"
    >
      <template v-slot:extraHeader>
        <!-- Add extra header for buttons (add user button) -->
        <th></th>
      </template>
      <template #extraColumn="columnProps">
        <!-- Add buttons for editing/deleting users -->
        <th scope="row">
          <ButtonGroup
            :buttonIcons="['search', 'pencil-fill', 'trash-fill']"
            :buttonColors="['info', 'primary', 'danger']"
            :clickCallbacks="[
              inspectProject,
              editProject,
              removeProject,
            ]"
            :callbackArguments="[
              columnProps.item,
              columnProps.item,
              columnProps.item,
            ]"
          ></ButtonGroup>
        </th>
      </template>
      <template #arrayType="arrayProps">
        <!-- Show Projects as badges -->
        <BadgeList
          :itemArray="arrayProps.data"
          :row="arrayProps.row"
          :saveCallback="deleteUser"
        ></BadgeList>
      </template>
      <template #editRow="rowProps">
        <TableRowEditor
          :rowToEdit="rowProps.row"
          arrayColumn="users"
          :saveCallback="saveEditedProject"
          :cancelCallback="clearProjectToEdit"
          :addArrayElementCallback="addUserToEditProject"
          :deleteArrayElementCallback="deleteUser"
          :saveArrayElementCallback="saveUser"
          :addArrayElementToRow="addUserToRow"
          v-model="userToAdd"
        ></TableRowEditor>
      </template>
    </Table>
  </div>
</template>

<script lang="ts">
import Badge from "../components/Badge.vue";
import BadgeList from "../components/BadgeList.vue";
import ButtonGroup from "../components/ButtonGroup.vue";
import InlineRowEdit from "../components/InlineRowEdit.vue";
import LoadingSpinner from "../components/LoadingSpinner.vue";
import SearchBar from "../components/SearchBar.vue";
import Table from "../components/Table.vue";
import TableRowEditor from "../components/TableRowEditor.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import { getProjects, putProject, deleteProject, deleteUser } from "../api/api";
import { stringIncludesOtherString, sortAlphabetically } from "../helpers/utils";
import { defineComponent, onMounted, Ref, ref } from "vue";
import { Project } from "@/types/api";
import { StringArray } from "@/types/types";

export default defineComponent({
  name: "Projects",
  components: {
    Badge,
    BadgeList,
    ButtonGroup,
    FeedbackMessage,
    InlineRowEdit,
    LoadingSpinner,
    SearchBar,
    Table,
    TableRowEditor,
  },
  setup() {
    const projects: Ref<Project[]> = ref([]);
    onMounted(() => {
      loadProjects();
    });
    const loadProjects = async () => {
      projects.value = await getProjects();
    };
    return {
      projects,
      loadProjects,
    };
  },
  data() {
    return {
      addUserToRow: false,
      userToAdd: "",
      projectToEdit: "",
      projectToEditIndex: -1,
      errorMessage: "",
      loading: false,
      successMessage: "",
      searchString: "",
    };
  },
  computed: {
    filteredAndSortedProjects(): Project[] {
      let projects = this.projects;
      if (this.searchString) {
        projects = this.projects.filter((project: Project) => {
          return stringIncludesOtherString(project.name, this.searchString);
        });
      }
      return sortAlphabetically(projects, "name") as Project[];
    },
  },
  watch: {
    projectToEdit() {
      this.projectToEditIndex = this.getEditIndex();
    },
  },
  methods: {
    addUserToEditProject() {
      this.addUserToRow = true;
    },
    clearUserMessages() {
      this.successMessage = "";
      this.errorMessage = "";
    },
    clearProjectToEdit() {
      this.projectToEdit = "";
    },
    deleteUser(users: StringArray, project: Project) {
      const updatedProject = project;
      project.users = users;
      // Don't save immediately while editing
      if (project.name !== this.projectToEdit) {
        this.saveProject(updatedProject, undefined);
      }
    },
    editProject(project: Project) {
      this.projectToEdit = project.name;
    },
    inspectProject() {
      console.log("inspecting");
    },
    getEditIndex() {
      const index = this.projects.findIndex((project: Project) => {
        return project.name === this.projectToEdit;
      });
      // only change when user is cleared, otherwise it will return -1 when name is altered
      if (this.projectToEdit === "" || index !== -1) {
        return index;
      } else return this.projectToEditIndex;
    },
    reloadProjects() {
      this.loading = true;
      this.loadProjects()
        .then(() => {
          this.loading = false;
        })
        .catch((error) => {
          this.errorMessage = `Could not load projects: ${error}.`;
        });
    },
    removeProject(project: Project) {
      this.clearUserMessages();
      deleteProject(project.name)
        .then(() => {
          this.successMessage = `[${project.name}] was successfully deleted.`;
          this.reloadProjects();
        })
        .catch((error) => {
          this.errorMessage = `Could not delete [${project.name}]: ${error}.`;
        });
    },
    saveEditedProject() {
      const project: Project = this.projects[this.projectToEditIndex];
      this.saveProject(project, () => {
        // Check if name was altered, then delete the old row
        if (project.name != this.projectToEdit) {
          deleteProject(this.projectToEdit).then(() => {
            this.reloadProjects();
          });
        }
        this.clearProjectToEdit();
      });
    },
    saveUser() {
      this.projects[this.projectToEditIndex].users.push(this.userToAdd);
      this.userToAdd = "";
      this.addUserToRow = false;
    },
    saveProject(project: Project, callback: Function | undefined) {
      this.clearUserMessages();
      if (project.name === "") {
        this.errorMessage = "Cannot create project with empty name.";
      } else {
        putProject(project)
          .then(() => {
            this.successMessage = `[${project.name}] was successfully saved.`;
            this.reloadProjects();
            if (callback) {
              callback();
            }
          })
          .catch((error) => {
            this.errorMessage = `Could not save [${project.name}]: ${error}.`;
          });
      }
    },
  },
});
</script>
