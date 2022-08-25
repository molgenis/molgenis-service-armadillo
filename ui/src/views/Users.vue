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
      :data="filteredUsers"
      idCol="email"
      :indexToEdit="this.userToEditIndex"
    >
      <template v-slot:extraHeader>
        <!-- Add extra header for buttons (add user button) -->
        <th>
          <button
            type="button"
            class="btn btn-sm me-1"
            :class="
              this.addRow ? 'btn-danger bg-danger' : 'btn-primary bg-primary'
            "
            @click="toggleAddRow"
          >
            <i class="bi bi-person-x-fill" v-if="this.addRow"></i>
            <i class="bi bi-person-plus-fill" v-else></i>
          </button>
        </th>
      </template>
      <template v-slot:extraRow v-if="addRow">
        <!-- Extra row for adding a new user  -->
        <UserEditor
          :userToEdit="this.newUser"
          :saveCallback="this.saveNewUser"
          :cancelCallback="this.clearNewUser"
          :addProjectCallback="this.addProjectToNewUser"
          :deleteProjectCallback="this.deleteProject"
          :saveProjectCallback="this.saveProjectInAddMode"
          :addProjectToRow="this.addProjectToNewRow"
          v-model="this.projectToAdd"
        ></UserEditor>
      </template>
      <template #extraColumn="columnProps">
        <!-- Add buttons for editing/deleting users -->
        <th scope="row">
          <div class="btn-group" role="group">
            <button
              type="button"
              class="btn btn-primary btn-sm bg-primary"
              @click="this.editUser(columnProps.item)"
            >
              <i class="bi bi-pencil-fill"></i>
            </button>
            <button
              type="button"
              class="btn btn-danger btn-sm bg-danger"
              @click="this.removeUser(columnProps.item)"
            >
              <i class="bi bi-trash-fill"></i>
            </button>
          </div>
        </th>
      </template>
      <template #arrayType="arrayProps">
        <!-- Show Projects as badges -->
        <TableColumnBadges
          :itemArray="arrayProps.data"
          :row="arrayProps.row"
          :saveCallback="this.deleteProject"
        ></TableColumnBadges>
      </template>
      <template #boolType="boolProps">
        <!-- Show booleans as checkboxes -->
        <input
          class="form-check-input"
          type="checkbox"
          :checked="boolProps.data"
          @change="this.updateAdmin(boolProps.row, boolProps.data)"
        />
      </template>
      <template #editRow="rowProps">
        <UserEditor
          :userToEdit="rowProps.row"
          :saveCallback="this.saveEditedUser"
          :cancelCallback="this.clearUserToEdit"
          :addProjectCallback="this.addProjectToEditUser"
          :deleteProjectCallback="this.deleteProject"
          :saveProjectCallback="this.saveProjectInEditMode"
          :addProjectToRow="this.addProjectToEditRow"
          v-model="this.projectToEdit"
        ></UserEditor>
      </template>
    </Table>
  </div>
</template>

<script>
import Badge from "../components/Badge.vue";
import InlineRowEdit from "../components/InlineRowEdit.vue";
import LoadingSpinner from "../components/LoadingSpinner.vue";
import SearchBar from "../components/SearchBar.vue";
import Table from "../components/Table.vue";
import TableColumnBadges from "../components/TableColumnBadges.vue";
import UserEditor from "../components/UserEditor.vue";
import UserFeedback from "../components/UserFeedback.vue";
import { getUsers, putUser, deleteUser } from "../api/api";
import { onMounted, ref } from "vue";

export default {
  name: "Users",
  components: {
    Badge,
    InlineRowEdit,
    LoadingSpinner,
    SearchBar,
    Table,
    TableColumnBadges,
    UserEditor,
    UserFeedback,
  },
  setup() {
    const users = ref([]);
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
  data() {
    return {
      addProjectToEditRow: false,
      addProjectToNewRow: false,
      addRow: false,
      errorMessage: "",
      loading: false,
      newUser: {
        email: "",
        firstName: "",
        lastName: "",
        institution: "",
        admin: false,
        projects: [],
      },
      projectToAdd: "",
      projectToEdit: "",
      searchString: "",
      successMessage: "",
      userToEdit: "",
      userToEditIndex: -1,
    };
  },
  computed: {
    filteredUsers() {
      if (this.searchString) {
        return this.users.filter((user) => {
          return (
            this.stringIncludesOtherString(user.email, this.searchString) ||
            this.stringIncludesOtherString(user.firstName, this.searchString) ||
            this.stringIncludesOtherString(user.lastName, this.searchString)
          );
        });
      } else {
        return this.users;
      }
    },
  },
  watch: {
    userToEdit() {
      this.userToEditIndex = this.getEditIndex();
    },
  },
  methods: {
    addProjectToNewUser() {
      this.addProjectToNewRow = true;
    },
    addProjectToEditUser() {
      this.addProjectToEditRow = true;
    },
    clearSuccess() {
      this.successMessage = "";
    },
    clearErrorMessage() {
      this.errorMessage = "";
    },
    clearUserToEdit() {
      this.userToEdit = "";
    },
    clearNewUser() {
      Object.keys(this.newUser).forEach((key) => {
        this.newUser[key] = "";
      });
      this.newUser.admin = false;
      this.newUser.projects = [];
    },
    deleteProject(projects, user) {
      const updatedUser = user;
      user.projects = projects;
      // Don't save immediately while editing
      if (user.email !== this.userToEdit && user.email !== this.newUser.email) {
        this.saveUser(updatedUser);
      }
    },
    editUser(user) {
      this.userToEdit = user.email;
    },
    getEditIndex() {
      const index = this.users.findIndex((user) => {
        return user.email === this.userToEdit;
      });
      // only change when user is cleared, otherwise it will return -1 when email is altered
      if (this.userToEdit === "" || index !== -1) {
        return index;
      } else return this.userToEditIndex;
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
    removeUser(user) {
      this.clearErrorMessage();
      this.clearSuccess();
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
      const user = this.users[this.userToEditIndex];
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
      this.saveUser(this.newUser, () => {
        if (this.successMessage) {
          this.clearNewUser();
          this.toggleAddRow();
        }
      });
    },
    saveProject() {
      projects.push(newProject);
      newProject = "";
      projectBool = false;
    },
    saveProjectInAddMode() {
      this.newUser.projects.push(this.projectToAdd);
      this.projectToAdd = "";
      this.addProjectToNewRow = false;
    },
    saveProjectInEditMode() {
      this.users[this.userToEditIndex].projects.push(this.projectToEdit);
      this.projectToEdit = "";
      this.addProjectToEditRow = false;
    },
    saveUser(user, callback) {
      this.clearErrorMessage();
      this.clearSuccess();
      if (user.email === "") {
        this.errorMessage = "Cannot create user with empty email adress.";
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
    stringIncludesOtherString(string, substring) {
      return string.toLowerCase().includes(substring.toLowerCase());
    },
    toggleAddRow() {
      this.addRow = !this.addRow;
    },
    updateAdmin(user, admin) {
      user.admin = !admin;
      this.saveUser(user);
    },
  },
};
</script>

<style>
button.check-badge {
  border: none;
  padding: 0;
  margin-left: 0.2em;
  margin-right: -0.2em;
}
</style>
