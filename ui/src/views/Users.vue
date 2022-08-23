<template>
  <div>
    <UserFeedback
      :successMessage="this.successMessage"
      :errorMessage="this.errorMessage"
    ></UserFeedback>
    <div class="spinner-border" role="status" v-if="this.loading">
      <span class="visually-hidden">Loading...</span>
    </div>
    <Table :data="users">
      <template v-slot:extraHeader>
        <th>
          <button
            type="button"
            class="btn btn-sm me-1"
            :class="
              this.addRow ? 'btn-danger  bg-danger' : 'btn-primary bg-primary'
            "
            @click="toggleAddRow"
          >
            <i class="bi bi-person-x-fill" v-if="this.addRow"></i>
            <i class="bi bi-person-plus-fill" v-else></i>
          </button>
        </th>
      </template>
      <template v-slot:extraRow v-if="addRow">
        <InlineRowEdit
          :row="this.newUser"
          :save="this.saveNewUser"
          :clear="this.clearNewUser"
        >
          <template #arrayEdit="array">
            <TableColumnBadges
              :data="this.newUser.projects"
              :row="array.rowData"
              :saveCallback="this.deleteProject"
            ></TableColumnBadges>
            <Badge v-if="this.addProjectToNewRow">
              <input
                type="text"
                v-model="projectToAdd"
              />
              <button
                class="check-badge text-light bg-secondary"
                @click="this.saveProject(array.rowData, array.arrayData)"
              >
                <i class="bi bi-check-lg"></i>
              </button>
            </Badge>
            <button
              class="btn btn-primary btn-sm float-end"
              @click="addProject"
            >
              <i class="bi bi-plus-lg"></i>
            </button>
          </template>
        </InlineRowEdit>
      </template>
      <template #extraColumn="columnProps">
        <th scope="row">
          <div class="btn-group" role="group">
            <button type="button" class="btn btn-primary btn-sm bg-primary">
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
        <TableColumnBadges
          :data="arrayProps.data"
          :row="arrayProps.row"
          :saveCallback="this.deleteProject"
        ></TableColumnBadges>
      </template>
      <template #boolType="boolProps">
        <input
          class="form-check-input"
          type="checkbox"
          :checked="boolProps.data"
          @change="this.updateAdmin(boolProps.row, boolProps.data)"
        />
      </template>
    </Table>
  </div>
</template>

<script>
import Table from "../components/Table.vue";
import InlineRowEdit from "../components/InlineRowEdit.vue";
import TableColumnBadges from "../components/TableColumnBadges.vue";
import UserFeedback from "../components/UserFeedback.vue";
import { getUsers, putUser, deleteUser } from "../api/api";
import { onMounted, ref } from "vue";
import Badge from "../components/Badge.vue";

export default {
  name: "Users",
  components: {
    InlineRowEdit,
    Table,
    TableColumnBadges,
    UserFeedback,
    Badge,
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
      addRow: false,
      errorMessage: "",
      successMessage: "",
      loading: false,
      projectToAdd: "",
      addProjectToNewRow: false,
      newUser: {
        email: "",
        firstName: "",
        lastName: "",
        institution: "",
        admin: false,
        projects: [],
      },
    };
  },
  computed: {
    newUserProjectsLength() {
      return this.newUser.projects.length;
    },
  },
  methods: {
    clearSuccess() {
      this.successMessage = "";
    },
    clearErrorMessage() {
      this.errorMessage = "";
    },
    addProject() {
      this.addProjectToNewRow = true;
    },
    saveProject() {
      this.newUser.projects[this.newUserProjectsLength] = this.projectToAdd;
      this.projectToAdd = "";
      this.addProjectToNewRow = false;
    },
    toggleAddRow() {
      this.addRow = !this.addRow;
    },
    deleteProject(projects, user) {
      const updatedUser = user;
      user.projects = projects;
      this.saveUser(updatedUser);
    },
    updateAdmin(user, admin) {
      user.admin = !admin;
      this.saveUser(user);
    },
    saveNewUser() {
      this.saveUser(this.newUser, () => {
        if (this.successMessage) {
          this.clearNewUser();
          this.toggleAddRow();
        }
      });
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
    saveUser(user, callback) {
      this.clearErrorMessage();
      this.clearSuccess();
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
    },
    clearNewUser() {
      Object.keys(this.newUser).forEach((key) => {
        this.newUser[key] = "";
      });
      this.newUser.admin = false;
      this.newUser.projects = [];
    },
  },
};
</script>

<style scoped>
button.check-badge {
  border: none;
  padding: 0;
  margin-left: 0.2em;
  margin-right: -0.2em;
}
</style>
