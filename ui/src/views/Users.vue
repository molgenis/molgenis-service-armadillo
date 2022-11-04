<template>
  <div>
    <div class="row">
      <div class="col">
        <!-- Error messages will appear here -->
        <FeedbackMessage
          :successMessage="successMessage"
          :errorMessage="errorMessage"
        ></FeedbackMessage>
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
      :dataToShow="filteredAndSortedUsers"
      :allData="users"
      :indexToEdit="editMode.userToEditIndex"
      :dataStructure="userDataStructure"
    >
      <template v-slot:extraHeader>
        <!-- Add extra header for buttons (add user button) -->
        <th>
          <button
            type="button"
            class="btn btn-sm me-1 btn-primary bg-primary"
            :disabled="addRow"
            @click="toggleAddRow"
          >
            <i class="bi bi-person-plus-fill"></i>
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
        />
      </template>
      <template #extraColumn="columnProps">
        <!-- Add buttons for editing/deleting users -->
        <th scope="row">
          <ButtonGroup
            :buttonIcons="['pencil-fill', 'trash-fill']"
            :buttonColors="['primary', 'danger']"
            :clickCallbacks="[editUser, removeUser]"
            :callbackArguments="[columnProps.item, columnProps.item]"
          ></ButtonGroup>
        </th>
      </template>
      <template #arrayType="arrayProps">
        <!-- Show Projects as badges -->
        <BadgeList
          :itemArray="arrayProps.data"
          :row="arrayProps.row"
          :saveCallback="deleteProject"
        ></BadgeList>
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
        />
      </template>
    </Table>
  </div>
</template>

<script lang="ts">
import Badge from "@/components/Badge.vue";
import BadgeList from "@/components/BadgeList.vue";
import ButtonGroup from "@/components/ButtonGroup.vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import SearchBar from "@/components/SearchBar.vue";
import Table from "@/components/Table.vue";
import InlineRowEdit from "@/components/InlineRowEdit.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import { deleteUser, getUsers, putUser } from "@/api/api";
import { sortAlphabetically, stringIncludesOtherString } from "@/helpers/utils";
import { defineComponent, onMounted, Ref, ref } from "vue";
import { User, UserStringKey } from "@/types/api";
import { StringArray, TypeObject } from "@/types/types";

export default defineComponent({
  name: "Users",
  components: {
    Badge,
    BadgeList,
    ButtonGroup,
    FeedbackMessage,
    InlineRowEdit,
    LoadingSpinner,
    SearchBar,
    Table,
  },
  setup() {
    const users: Ref<User[]> = ref([]);
    onMounted(() => {
      loadUsers();
    });
    const loadUsers = async () => {
      users.value = await getUsers();
    };
    return {
      users,
      loadUsers,
    };
  },
  data(): {
    userDataStructure: TypeObject;
    editMode: {
      addProjectToRow: boolean;
      project: string;
      userToEdit: string;
      userToEditIndex: number;
    };
    addMode: {
      addProjectToRow: boolean;
      newUser: User;
      project: string;
    };
    addRow: boolean;
    errorMessage: string;
    loading: boolean;
    successMessage: string;
    searchString: string;
  } {
    return {
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
        project: "",
        userToEdit: "",
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
        project: "",
      },
      addRow: false,
      errorMessage: "",
      loading: false,
      successMessage: "",
      searchString: "",
    };
  },
  computed: {
    userToEdit() {
      return this.editMode.userToEdit;
    },
    filteredAndSortedUsers(): User[] {
      let users = this.users;
      if (this.searchString) {
        users = this.users.filter((user: User) => {
          return (
            stringIncludesOtherString(user.email, this.searchString) ||
            stringIncludesOtherString(user.firstName, this.searchString) ||
            stringIncludesOtherString(user.lastName, this.searchString)
          );
        });
      }
      if (this.userToEdit) {
        return users;
      } else {
        return sortAlphabetically(users, "email") as User[];
      }
    },
  },
  watch: {
    userToEdit() {
      this.editMode.userToEditIndex = this.getEditIndex();
    },
  },
  methods: {
    clearUserMessages() {
      this.successMessage = "";
      this.errorMessage = "";
    },
    clearUserToEdit() {
      this.editMode.userToEdit = "";
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
    },
    deleteProject(projects: StringArray, user: User) {
      const updatedUser: User = user;
      user.projects = projects;
      // Don't save immediately while editing
      if (
        user.email !== this.editMode.userToEdit &&
        user.email !== this.addMode.newUser.email
      ) {
        this.saveUser(updatedUser, undefined);
      }
    },
    editUser(user: User) {
      this.editMode.userToEdit = user.email;
      this.addRow = false;
    },
    getEditIndex() {
      const index = this.users.findIndex((user: User) => {
        return user.email === this.editMode.userToEdit;
      });
      console.log(index);
      // only change when user is cleared, otherwise it will return -1 when email is altered
      if (this.editMode.userToEdit === "" || index !== -1) {
        return index;
      } else return this.editMode.userToEditIndex;
    },
    reloadUsers() {
      this.loading = true;
      this.loadUsers()
        .then(() => {
          this.loading = false;
        })
        .catch((error) => {
          this.errorMessage = `Could not load users: ${error}.`;
        });
    },
    removeUser(user: User) {
      this.clearUserMessages();
      deleteUser(user.email)
        .then(() => {
          this.successMessage = `[${user.email}] was successfully deleted.`;
          this.reloadUsers();
        })
        .catch((error) => {
          this.errorMessage = `Could not delete [${user.email}]: ${error}.`;
        });
    },
    saveEditedUser() {
      const user: User = this.users[this.editMode.userToEditIndex];
      this.saveUser(user, () => {
        // Check if email was altered, then delete the old row
        if (user.email != this.editMode.userToEdit) {
          deleteUser(this.editMode.userToEdit).then(() => {
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
    saveProject(projects: StringArray, mode: "editMode" | "addMode") {
      projects.push(this[mode].project);
      this[mode].project = "";
      this[mode].addProjectToRow = false;
    },
    saveUser(user: User, callback: Function | undefined) {
      this.clearUserMessages();
      if (user.email === "") {
        this.errorMessage = "Cannot create user with empty email address.";
      } else {
        putUser(user)
          .then(() => {
            this.successMessage = `[${user.email}] was successfully saved.`;
            this.reloadUsers();
            if (callback) {
              callback();
            }
          })
          .catch((error) => {
            this.errorMessage = `Could not save [${user.email}]: ${error}.`;
          });
      }
    },
    toggleAddRow() {
      this.addRow = !this.addRow;
      this.clearUserToEdit();
    },
    updateAdmin(user: User, isAdmin: boolean) {
      user.admin = !isAdmin;
      this.saveUser(user, undefined);
    },
  },
});
</script>
