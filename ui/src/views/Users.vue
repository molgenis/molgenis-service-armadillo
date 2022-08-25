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
      :indexToEdit="this.editMode.userToEditIndex"
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
          :userToEdit="this.addMode.newUser"
          :saveCallback="this.saveNewUser"
          :cancelCallback="this.clearNewUser"
          :addProjectCallback="this.addProjectToNewUser"
          :deleteProjectCallback="this.deleteProject"
          :saveProjectCallback="this.saveProjectInAddMode"
          :addProjectToRow="this.addMode.addProjectToRow"
          v-model="this.addMode.project"
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
        <BadgeList
          :itemArray="arrayProps.data"
          :row="arrayProps.row"
          :saveCallback="this.deleteProject"
        ></BadgeList>
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
          :addProjectToRow="this.editMode.addProjectToRow"
          v-model="this.editMode.project"
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
import BadgeList from "../components/BadgeList.vue";
import UserEditor from "../components/UserEditor.vue";
import UserFeedback from "../components/UserFeedback.vue";
import { getUsers, putUser, deleteUser } from "../api/api.js";
import { stringIncludesOtherString } from "../helpers/utils.js";
import { onMounted, ref } from "vue";

export default {
  name: "Users",
  components: {
    Badge,
    BadgeList,
    InlineRowEdit,
    LoadingSpinner,
    SearchBar,
    Table,
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
    filteredUsers() {
      if (this.searchString) {
        return this.users.filter((user) => {
          return (
            stringIncludesOtherString(user.email, this.searchString) ||
            stringIncludesOtherString(user.firstName, this.searchString) ||
            stringIncludesOtherString(user.lastName, this.searchString)
          );
        });
      } else {
        return this.users;
      }
    },
  },
  watch: {
    userToEdit() {
      this.editMode.userToEditIndex = this.getEditIndex();
    },
  },
  methods: {
    addProjectToNewUser() {
      this.addMode.addProjectToRow = true;
    },
    addProjectToEditUser() {
      this.editMode.addProjectToRow = true;
    },
    clearUserMessages() {
      this.successMessage = "";
      this.errorMessage = "";
    },
    clearUserToEdit() {
      this.editMode.userToEdit = "";
    },
    clearNewUser() {
      Object.keys(this.addMode.newUser).forEach((key) => {
        this.addMode.newUser[key] = "";
      });
      this.addMode.newUser.admin = false;
      this.addMode.newUser.projects = [];
    },
    deleteProject(projects, user) {
      const updatedUser = user;
      user.projects = projects;
      // Don't save immediately while editing
      if (user.email !== this.editMode.userToEdit && user.email !== this.addMode.newUser.email) {
        this.saveUser(updatedUser);
      }
    },
    editUser(user) {
      this.editMode.userToEdit = user.email;
    },
    getEditIndex() {
      const index = this.users.findIndex((user) => {
        console.log(user, user.email === this.editMode.userToEdit, this.editMode.userToEdit)
        return user.email === this.editMode.userToEdit;
      });
      console.log(index)
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
    removeUser(user) {
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
      const user = this.users[this.editMode.userToEditIndex];
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
          this.toggleAddRow();
        }
      });
    },
    saveProject(projects, newProject, projectBool) {
      projects.push(newProject);
      newProject = "";
      projectBool = false;
    },
    saveProjectInAddMode() {
      this.addMode.newUser.projects.push(this.addMode.project)
      this.addMode.project = "";
      this.addMode.addProjectToRow = false;
    },
    saveProjectInEditMode() {
      this.users[this.editMode.userToEditIndex].projects.push(this.editMode.project);
      this.editMode.project = "";
      this.editMode.addProjectToRow = false;
    },
    saveUser(user, callback) {
      this.clearUserMessages();
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
