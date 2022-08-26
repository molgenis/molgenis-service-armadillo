<template>
  <div>
    <div class="row">
      <div class="col">
        <!-- Error messages will appear here -->
        <UserFeedback
          :successMessage="this.successMessage"
          :errorMessage="this.errorMessage"
        ></UserFeedback>
        <!-- Loading spinner -->
        <LoadingSpinner v-if="this.loading"></LoadingSpinner>
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
      :indexToEdit="this.projectToEditIndex"
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
              this.inspectProject,
              this.editProject,
              this.removeProject,
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
          :saveCallback="this.deleteUser"
        ></BadgeList>
      </template>
      <template #editRow="rowProps">
        <TableRowEditor
          :rowToEdit="rowProps.row"
          arrayColumn="users"
          :saveCallback="this.saveEditedProject"
          :cancelCallback="this.clearProjectToEdit"
          :addArrayElementCallback="this.addUserToEditProject"
          :deleteArrayElementCallback="this.deleteUser"
          :saveArrayElementCallback="this.saveUser"
          :addArrayElementToRow="this.addUserToRow"
          v-model="this.userToAdd"
        ></TableRowEditor>
      </template>
    </Table>
  </div>
</template>

<script>
import Badge from "../components/Badge.vue";
import BadgeList from "../components/BadgeList.vue";
import ButtonGroup from "../components/ButtonGroup.vue";
import InlineRowEdit from "../components/InlineRowEdit.vue";
import LoadingSpinner from "../components/LoadingSpinner.vue";
import SearchBar from "../components/SearchBar.vue";
import Table from "../components/Table.vue";
import TableRowEditor from "../components/TableRowEditor.vue";
import UserFeedback from "../components/UserFeedback.vue";
import { getProjects, putProject, deleteProject } from "../api/api.js";
import { stringIncludesOtherString, sortAlphabetically } from "../helpers/utils.js";
import { onMounted, ref } from "vue";

export default {
  name: "Projects",
  components: {
    Badge,
    BadgeList,
    ButtonGroup,
    InlineRowEdit,
    LoadingSpinner,
    SearchBar,
    Table,
    TableRowEditor,
    UserFeedback,
  },
  setup() {
    const projects = ref([]);
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
    filteredAndSortedProjects() {
      const projects = this.projects;
      if (this.searchString) {
        projects = this.projects.filter((project) => {
          return stringIncludesOtherString(project.name, this.searchString);
        });
      }
      return sortAlphabetically(projects, "name");
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
    deleteUser(users, project) {
      const updatedProject = project;
      project.users = users;
      // Don't save immediately while editing
      if (project.name !== this.projectToEdit) {
        this.saveProject(updatedProject);
      }
    },
    editProject(project) {
      this.projectToEdit = project.name;
    },
    inspectProject() {
      console.log("inspecting");
    },
    getEditIndex() {
      const index = this.projects.findIndex((project) => {
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
    removeProject(project) {
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
      const project = this.projects[this.projectToEditIndex];
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
    saveProject(project, callback) {
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
};
</script>
