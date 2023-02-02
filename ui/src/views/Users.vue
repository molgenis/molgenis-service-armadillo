<template>
  <div>
    <h2 class="mt-3">Users</h2>
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
          recordType="user"
          @proceed="proceedDelete"
          @cancel="clearRecordToDelete"
        ></ConfirmationDialog>
        <ConfirmationDialog
          v-if="projectToAdd != ''"
          :record="projectToAdd"
          action="add new"
          recordType="project"
          @proceed="proceedProjectUpdate"
          @cancel="clearProject"
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
      :dataToShow="getFilteredAndSortedUsers()"
      :allData="users"
      :indexToEdit="editMode.userToEditIndex"
      :dataStructure="userDataStructure"
      :highlightedRowIndex="updatedUserIndex"
    >
      <template v-slot:extraHeader>
        <!-- Add extra header for buttons (add user button) -->
        <th>
          <button
            type="button"
            class="btn btn-sm me-1 btn-primary bg-primary"
            :disabled="addRow || editMode.userToEdit !== ''"
            @click="toggleAddRow"
          >
            <i class="bi bi-plus-lg"></i>
          </button>
        </th>
      </template>
      <template v-slot:extraRow v-if="addRow">
        <!-- Extra row for adding a new user  -->
        <InlineRowEdit
          :row="addMode.newUser"
          :save="saveNewUser"
          :cancel="clearNewUser"
          :hideColumns="[]"
          :dataStructure="userDataStructure"
          :dropDowns="{'projects': availableProjects}"
          @update-array-element="updateProjects"
        />
      </template>
      <template #extraColumn="columnProps">
        <!-- Add buttons for editing/deleting users -->
        <th scope="row">
          <ButtonGroup
            :buttonIcons="['pencil-fill', 'trash-fill']"
            :buttonColors="['primary', 'danger']"
            :clickCallbacks="[editUser, removeUser]"
            :disabled="
              addRow ||
              (editMode.userToEdit != '' &&
                editMode.userToEdit !== columnProps.email)
            "
            :callbackArguments="[columnProps.item, columnProps.item]"
          ></ButtonGroup>
        </th>
      </template>
      <template #arrayType="arrayProps">
        <!-- Show Projects as badges -->
        <BadgeList :itemArray="arrayProps.data" :canEdit="false"></BadgeList>
      </template>
      <template #boolType="boolProps">
        <!-- Show booleans as checkboxes -->
        <input
          class="form-check-input"
          type="checkbox"
          :checked="boolProps.data"
          @change="updateAdmin(boolProps.row, boolProps.data)"
        />
      </template>
      <template #editRow="rowProps">
        <InlineRowEdit
          :row="rowProps.row"
          :save="saveEditedUser"
          :cancel="clearUserToEdit"
          :hideColumns="[]"
          :dataStructure="userDataStructure"
          :dropDowns="{'projects': availableProjects}"
          @update-array-element="updateProjects"
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
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import SearchBar from "@/components/SearchBar.vue";
import Table from "@/components/Table.vue";
import InlineRowEdit from "@/components/InlineRowEdit.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import { deleteUser, getUsers, putUser, getProjects } from "@/api/api";
import { sortAlphabetically, stringIncludesOtherString } from "@/helpers/utils";
import { defineComponent, onMounted, Ref, ref } from "vue";
import { User, UserStringKey } from "@/types/api";
import { StringArray, UsersData } from "@/types/types";
import { useRouter } from "vue-router";
import { processErrorMessages } from "@/helpers/errorProcessing";

export default defineComponent({
  name: "Users",
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
    const users: Ref<User[]> = ref([]);
    const errorMessage: Ref<string> = ref("");
    const router = useRouter();
    onMounted(() => {
      loadUsers();
    });
    const loadUsers = async () => {
      users.value = await getUsers().catch((error: string) => {
        errorMessage.value = processErrorMessages(error, router);
        return [];
      });
    };
    return {
      users,
      errorMessage,
      loadUsers,
    };
  },
  data(): UsersData {
    return {
      availableProjects: [],
      recordToDelete: "",
      projectToAdd: "",
      confirmedProject: "",
      updatedUserIndex: -1,
      userDataStructure: {
        email: "string",
        firstName: "string",
        lastName: "string",
        institution: "string",
        admin: "boolean",
        projects: "array",
      },
      editMode: {
        addProjectToRow: false,
        userToEdit: "",
        projects: [],
        userToEditIndex: -1,
      },
      addMode: {
        addProjectToRow: false,
        newUser: {
          email: "",
          firstName: "",
          lastName: "",
          institution: "",
          admin: false,
          projects: [],
        },
      },
      addRow: false,
      loading: false,
      successMessage: "",
      searchString: "",
    };
  },
  computed: {
    projectsOfUserToEdit: {
      get(): string[] {
        return this.editMode.projects;
      },
      set(projects) {
        this.editMode.projects = projects;
      },
    },
    disabledButtons(): boolean[] {
      return [this.addRow, this.addRow];
    },
    userToEdit: {
      get(): string {
        return this.editMode.userToEdit;
      },
      set(newValue: string) {
        this.editMode.userToEdit = newValue;
      },
    },
  },
  watch: {
    userToEdit() {
      this.editMode.userToEditIndex = this.getEditIndex();
      if (this.editMode.userToEditIndex !== -1) {
        this.projectsOfUserToEdit =
          this.users[this.editMode.userToEditIndex].projects;
        this.updateAvailableProjects();
      }
    },
  },
  methods: {
    updateAvailableProjects() {
      let availableProjects: StringArray = [];
      getProjects()
        .catch((error: string) => {
          this.errorMessage = processErrorMessages(error, this.$router);
          return [];
        })
        .then((projects) => {
          projects.forEach((project) => {
            if (this.projectsOfUserToEdit.indexOf(project.name) === -1) {
              availableProjects.push(project.name);
            }
          });
        });
      this.availableProjects = availableProjects;
    },
    updateProjects(event: Event) {
      const project = event.toString();
      if (
        this.userToEdit !== "" &&
        this.projectsOfUserToEdit.indexOf(project) !== -1
      ) {
        this.errorMessage = `Project: [${project}] already added to user: [${this.userToEdit}]`;
      } else if (this.addMode.newUser.projects.indexOf(project) !== -1) {
        this.errorMessage = `Project: [${project}] already added to new user`;
      } else if (this.availableProjects.indexOf(project) === -1) {
        this.projectToAdd = project;
      } else {
        this.confirmedProject = project;
        this.proceedProjectUpdate();
      }
    },
    removeFromAvailableProjects(projectName: string) {
      const indexOfProject = this.availableProjects.indexOf(projectName);
      this.availableProjects.splice(indexOfProject, 1);
    },
    proceedProjectUpdate() {
      if (this.confirmedProject === "") {
        this.confirmedProject = this.projectToAdd;
      }
      if (this.editMode.userToEditIndex !== -1) {
        this.users[this.editMode.userToEditIndex].projects.push(
          this.confirmedProject
        );
        this.projectsOfUserToEdit =
          this.users[this.editMode.userToEditIndex].projects;
        this.removeFromAvailableProjects(this.confirmedProject);
      } else {
        this.addMode.newUser.projects.push(this.confirmedProject);
        this.removeFromAvailableProjects(this.confirmedProject);
      }
    },
    clearProject() {
      this.projectToAdd = "";
      this.confirmedProject = "";
    },
    clearUpdatedUserIndex() {
      this.updatedUserIndex = -1;
    },
    clearUserMessages() {
      this.successMessage = "";
      this.errorMessage = "";
    },
    clearUserToEdit() {
      this.userToEdit = "";
      this.reloadUsers();
    },
    clearNewUser() {
      Object.keys(this.addMode.newUser).forEach((key) => {
        if (key != "projects" && key != "admin") {
          this.addMode.newUser[key as UserStringKey] = "";
        }
      });
      this.addMode.newUser.admin = false;
      this.addMode.newUser.projects = [];
      this.toggleAddRow();
      this.reloadUsers();
    },
    editUser(user: User) {
      this.userToEdit = user.email;
      this.addRow = false;
    },
    getEditIndex() {
      const index = this.users.findIndex((user: User) => {
        return user.email === this.userToEdit;
      });
      // only change when user is cleared, otherwise it will return -1 when email is altered
      if (this.userToEdit === "" || index !== -1) {
        return index;
      } else return this.editMode.userToEditIndex;
    },
    async reloadUsers() {
      this.loading = true;
      try {
        await this.loadUsers();
        this.loading = false;
      } catch (error) {
        this.errorMessage = `Could not load users: ${error}.`;
        this.loading = false;
      }
    },
    getFilteredAndSortedUsers(): User[] {
      let users = this.users;
      if (this.searchString) {
        users = this.users.filter((user: User) => {
          const firstName = user.firstName ? user.firstName : "";
          const lastName = user.lastName ? user.firstName : "";
          return (
            stringIncludesOtherString(user.email, this.searchString) ||
            stringIncludesOtherString(firstName, this.searchString) ||
            stringIncludesOtherString(lastName, this.searchString)
          );
        });
      }
      if (this.userToEdit) {
        return users;
      } else {
        return sortAlphabetically(users, "email") as User[];
      }
    },
    clearRecordToDelete() {
      this.recordToDelete = "";
    },
    proceedDelete(userEmail: string) {
      this.clearRecordToDelete();
      deleteUser(userEmail)
        .then(() => {
          this.successMessage = `[${userEmail}] was successfully deleted.`;
          this.reloadUsers();
        })
        .catch((error) => {
          this.errorMessage = `Could not delete [${userEmail}]: ${error}.`;
        });
    },
    removeUser(user: User) {
      this.clearUserMessages();
      this.recordToDelete = user.email;
    },
    saveEditedUser() {
      const user: User = this.users[this.editMode.userToEditIndex];
      this.saveUser(user, () => {
        // Check if email was altered, then delete the old row
        if (user.email != this.userToEdit) {
          deleteUser(this.userToEdit).then(() => {
            this.reloadUsers();
          });
        }
        this.clearUserToEdit();
      });
    },
    saveNewUser() {
      this.saveUser(this.addMode.newUser, () => {
        if (this.successMessage) {
          this.clearNewUser();
        }
      });
    },
    saveUser(user: User, callback: Function | undefined) {
      this.clearUserMessages();
      const emailList = this.users.map((existingUser) => {
        return existingUser.email;
      });
      if (user.email === "") {
        this.errorMessage = "Cannot create user with empty email address.";
      } else if (
        user.email === this.addMode.newUser.email &&
        emailList.includes(user.email)
      ) {
        this.errorMessage = `User with email address [${user.email}] already exists.`;
      } else {
        const userEmail = user.email;
        putUser(user)
          .then(async () => {
            this.successMessage = `[${user.email}] was successfully saved.`;
            await this.reloadUsers();
            if (callback) {
              callback();
            }
            this.updatedUserIndex = this.getFilteredAndSortedUsers().findIndex(
              (u) => {
                return u.email === userEmail;
              }
            );
            setTimeout(this.clearUpdatedUserIndex, 1000);
          })
          .catch((error) => {
            this.errorMessage = `Could not save [${user.email}]: ${error}.`;
          });
      }
    },
    toggleAddRow() {
      this.addRow = !this.addRow;
      this.updateAvailableProjects();
      this.clearUserToEdit();
    },
    updateAdmin(user: User, isAdmin: boolean) {
      user.admin = !isAdmin;
      this.saveUser(user, undefined);
    },
  },
});
</script>
