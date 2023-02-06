<template>
  <div>
    <h2 class="mt-3">Projects</h2>
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
        <ConfirmationDialog
          v-if="userToAdd != ''"
          :record="userToAdd"
          action="add new"
          recordType="user"
          extraInfo="User will be added when project is saved"
          @proceed="proceedUserUpdate(userToAdd)"
          @cancel="clearUser"
        ></ConfirmationDialog>
      </div>
    </div>
    <div class="row">
      <div class="col-0 col-sm-9"></div>
      <div class="col-12 col-sm-3">
        <SearchBar class="mt-1" v-model="searchString" />
      </div>
    </div>
    <!-- Loading spinner -->
    <LoadingSpinner v-if="loading"></LoadingSpinner>
    <!-- Actual table -->
    <Table
      v-else
      :dataToShow="getFilteredAndSortedProjects()"
      :allData="projects"
      :indexToEdit="projectToEditIndex"
      :customColumns="['name']"
      :dataStructure="projectsDataStructure"
      :highlightedRowIndex="projectToHighlightIndex"
    >
      <template #customType="customProps">
        {{ customProps.data }}
        <router-link :to="`/projects-explorer/${customProps.data}`">
          <button class="btn btn-link">
            <i class="bi bi-table"></i><i class="bi bi-pencil"></i>
          </button>
        </router-link>
      </template>
      <template v-slot:extraHeader>
        <!-- Add extra header for buttons (add user button) -->
        <th>
          <button
            type="button"
            class="btn btn-sm me-1 btn-primary bg-primary"
            @click="toggleAddRow"
            :disabled="addRow || projectToEdit.name != ''"
          >
            <i class="bi bi-plus-lg"></i>
          </button>
        </th>
      </template>
      <template v-slot:extraRow v-if="addRow">
        <!-- Extra row for adding a new user  -->
        <InlineRowEdit
          :row="newProject"
          :save="saveNewProject"
          :cancel="clearNewProject"
          :hideColumns="[]"
          :dataStructure="projectsDataStructure"
          :dropDowns="{ users: availableUsers }"
          @update-array-element="updateUsers"
        />
      </template>
      <template #extraColumn="columnProps">
        <!-- Add buttons for editing/deleting users -->
        <th scope="row">
          <ButtonGroup
            :buttonIcons="['pencil-fill', 'trash-fill']"
            :buttonColors="['primary', 'danger']"
            :clickCallbacks="[editProject, removeProject]"
            :callbackArguments="[columnProps.item, columnProps.item]"
            :disabled="
              addRow ||
              (projectToEdit.name != '' &&
                projectToEdit.name !== columnProps.item.name)
            "
          ></ButtonGroup>
        </th>
      </template>
      <template #arrayType="arrayProps">
        <!-- Show Projects as badges -->
        <BadgeList :itemArray="arrayProps.data" :canEdit="false"></BadgeList>
      </template>
      <template #editRow="rowProps">
        <InlineRowEdit
          :immutable="['name']"
          :row="rowProps.row"
          :save="saveEditedProject"
          :cancel="clearProjectToEdit"
          :hideColumns="[]"
          :dataStructure="projectsDataStructure"
          :dropDowns="{ users: availableUsers }"
          @update-array-element="updateUsers"
        />
      </template>
    </Table>
  </div>
</template>

<script lang="ts">
import Badge from "@/components/Badge.vue";
import BadgeList from "@/components/BadgeList.vue";
import ButtonGroup from "@/components/ButtonGroup.vue";
import ConfirmationDialog from "@/components/ConfirmationDialog.vue";
import InlineRowEdit from "@/components/InlineRowEdit.vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import SearchBar from "@/components/SearchBar.vue";
import Table from "@/components/Table.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import { deleteProject, getProjects, getUsers, putProject } from "@/api/api";
import { sortAlphabetically, stringIncludesOtherString } from "@/helpers/utils";
import { defineComponent, onMounted, Ref, ref } from "vue";
import { Project } from "@/types/api";
import { ProjectsData, StringArray } from "@/types/types";
import { useRouter } from "vue-router";
import { processErrorMessages } from "@/helpers/errorProcessing";

export default defineComponent({
  name: "Projects",
  components: {
    Badge,
    BadgeList,
    ButtonGroup,
    ConfirmationDialog,
    FeedbackMessage,
    InlineRowEdit,
    LoadingSpinner,
    SearchBar,
    Table,
  },
  setup() {
    const projects: Ref<Project[]> = ref([]);
    const errorMessage: Ref<string> = ref("");
    const router = useRouter();
    onMounted(() => {
      loadProjects();
    });
    const loadProjects = async () => {
      projects.value = await getProjects().catch((error: string) => {
        errorMessage.value = processErrorMessages(error, router);
        return [];
      });
    };
    return {
      projects,
      errorMessage,
      loadProjects,
    };
  },
  data(): ProjectsData {
    return {
      availableUsers: [],
      userToAdd: "",
      recordToDelete: "",
      addRow: false,
      newProject: {
        name: "",
        users: [],
      },
      projectsDataStructure: {
        name: "string",
        users: "array",
      },
      projectToEdit: {
        name: "",
        users: [],
      },
      projectToHighlightIndex: -1,
      projectToEditIndex: -1,
      loading: false,
      successMessage: "",
      searchString: "",
    };
  },
  computed: {
    isEditingProject(): boolean {
      return this.projectToEditIndex !== -1;
    },
    usersOfProjectToEdit: {
      get(): string[] {
        return this.projectToEdit.users;
      },
      set(users) {
        this.projectToEdit.users = users;
      },
    },
  },
  watch: {
    projectToEdit() {
      if (this.isEditingProject) {
        this.usersOfProjectToEdit =
          this.projects[this.projectToEditIndex].users;
        this.updateAvailableUsers();
      }
    },
  },
  methods: {
    addingDuplicateUserToExistingProject(user: string) {
      return (
        this.isEditingProject && this.usersOfProjectToEdit.indexOf(user) !== -1
      );
    },
    addingDuplicateUserToNewProject(user: string) {
      return this.newProject.users.indexOf(user) !== -1;
    },
    addingNonExistingUser(user: string) {
      return this.availableUsers.indexOf(user) === -1;
    },
    updateAvailableUsers() {
      let availableUsers: StringArray = [];
      getUsers()
        .catch((error: string) => {
          this.errorMessage = processErrorMessages(error, this.$router);
          return [];
        })
        .then((users) => {
          users.forEach((user) => {
            if (this.usersOfProjectToEdit.indexOf(user.email) === -1) {
              availableUsers.push(user.email);
            }
          });
        });
      this.availableUsers = availableUsers;
    },
    updateUsers(event: Event) {
      const user = event.toString();
      if (this.addingDuplicateUserToExistingProject(user)) {
        this.errorMessage = `User: [${user}] already added to project: [${this.projectToEdit.name}]`;
      } else if (this.addingDuplicateUserToNewProject(user)) {
        this.errorMessage = `User: [${user}] already added to new project`;
      } else if (this.addingNonExistingUser(user)) {
        // this will trigger confirmation dialog
        this.userToAdd = user;
      } else {
        this.proceedUserUpdate(user);
      }
    },
    proceedUserUpdate(confirmedUser: string) {
      if (this.isEditingProject) {
        this.projects[this.projectToEditIndex].users.push(confirmedUser);
        this.usersOfProjectToEdit =
          this.projects[this.projectToEditIndex].users;
        this.removeFromAvailableUsers(confirmedUser);
      } else {
        this.newProject.users.push(confirmedUser);
        this.removeFromAvailableUsers(confirmedUser);
      }
    },
    clearUser() {
      this.userToAdd = "";
    },
    removeFromAvailableUsers(userName: string) {
      const indexOfUser = this.availableUsers.indexOf(userName);
      this.availableUsers.splice(indexOfUser, 1);
    },
    clearUserMessages() {
      this.successMessage = "";
      this.errorMessage = "";
    },
    clearProjectToEdit() {
      this.projectToEditIndex = -1;
      setTimeout(() => {
        this.projectToHighlightIndex = -1;
      }, 1000);

      this.projectToEdit = { name: "", users: [] };
      this.reloadProjects();
    },
    editProject(project: Project) {
      this.clearNewProject();
      this.projectToEditIndex = this.getProjectIndex(project.name);
      this.projectToHighlightIndex = this.getProjectIndex(project.name);
      this.projectToEdit = project;
    },
    getProjectIndex(projectName: string) {
      return this.projects.findIndex((someProject) => {
        return someProject.name === projectName;
      });
    },
    async reloadProjects() {
      this.loading = true;
      try {
        await this.loadProjects();
        this.loading = false;
      } catch (error) {
        this.loading = false;
        this.errorMessage = `Could not load projects: ${error}.`;
      }
    },
    getFilteredAndSortedProjects(): Project[] {
      let projects = this.projects;
      if (this.searchString) {
        projects = this.projects.filter((project: Project) => {
          return stringIncludesOtherString(project.name, this.searchString);
        });
      }
      return sortAlphabetically(projects, "name") as Project[];
    },
    proceedDelete(projectName: string) {
      this.clearRecordToDelete();
      deleteProject(projectName)
        .then(() => {
          this.successMessage = `[${projectName}] was successfully deleted.`;
          this.reloadProjects();
        })
        .catch((error) => {
          this.errorMessage = `Could not delete [${projectName}]: ${error}.`;
        });
    },
    clearRecordToDelete() {
      this.recordToDelete = "";
    },
    removeProject(project: Project) {
      this.clearUserMessages();
      this.recordToDelete = project.name;
    },
    saveEditedProject() {
      const project: Project = this.projects[this.projectToEditIndex];
      this.saveProject(project, () => {
        this.clearProjectToEdit();
      });
    },
    saveProject(project: Project, callback: Function | undefined) {
      this.clearUserMessages();
      const projectName = project.name;
      const projectNames = this.projects.map((existingProject) => {
        return existingProject.name;
      });
      if (projectName === "") {
        this.errorMessage = "Cannot create project with empty name.";
      } else if (
        projectName === this.newProject.name &&
        projectNames.includes(projectName)
      ) {
        this.errorMessage = `Project with name [${projectName}] already exists.`;
      } else {
        putProject(project)
          .then(async () => {
            this.successMessage = `[${projectName}] was successfully saved.`;
            await this.reloadProjects();
            if (callback) {
              callback();
            }
          })
          .catch((error) => {
            this.errorMessage = `Could not save [${projectName}]: ${error}.`;
          });
      }
    },
    toggleAddRow() {
      this.addRow = !this.addRow;
      this.updateAvailableUsers();
      this.clearProjectToEdit();
    },
    saveNewProject() {
      this.saveProject(this.newProject, () => {
        if (this.successMessage) {
          this.clearNewProject();
        }
      });
    },
    clearNewProject() {
      this.newProject.name = "";
      this.newProject.users = [];
      this.addRow = false;
      this.reloadProjects();
    },
  },
});
</script>
