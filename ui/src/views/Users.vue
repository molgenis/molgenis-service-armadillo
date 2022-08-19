<template>
  <div>
    <Alert v-if="this.errorMessage" type="danger" class="mt-1">
      <strong>Could not create user:</strong> [{{ this.newUser.email }}].
      Reason: {{ this.errorMessage }}
    </Alert>
    <Alert v-if="this.success" type="success" class="mt-1">
      <strong>Successfully created user:</strong> [{{ this.newUser.email }}]
    </Alert>
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
        <tr>
          <th scope="row">
            <div class="btn-group" role="group">
              <button
                type="button"
                class="btn btn-success btn-sm bg-success"
                @click="saveUser"
              >
                <i class="bi bi-check-lg"></i>
              </button>
              <button
                type="button"
                class="btn btn-danger btn-sm bg-danger"
                @click="clearUser"
              >
                <i class="bi bi-x-lg"></i>
              </button>
            </div>
          </th>
          <td v-for="(value, column) in users[0]">
            <div class="input-group mb-3" v-if="column == 'projects'">
              <input
                type="text"
                class="form-control"
                v-model="newUser[column][0]"
                :placeholder="column"
                :aria-label="column"
              />
            </div>
            <div v-else-if="column == 'admin'">
              <input class="form-check-input" type="checkbox" v-model="newUser[column]" />
            </div>
            <div class="input-group mb-3" v-else>
              <input
                type="text"
                class="form-control"
                v-model="newUser[column]"
                :placeholder="column"
                :aria-label="column"
              />
            </div>
          </td>
        </tr>
      </template>
      <template #extraColumn="columnProps">
        <th scope="row">
          <div class="btn-group" role="group">
            <button type="button" class="btn btn-primary btn-sm bg-primary">
              <i class="bi bi-pencil-fill"></i>
            </button>
            <button type="button" class="btn btn-danger btn-sm bg-danger">
              <i class="bi bi-trash-fill"></i>
            </button>
          </div>
        </th>
      </template>
      <template #arrayType="arrayProps">
        <TableColumnBadges :data="arrayProps.data"></TableColumnBadges>
      </template>
      <template #boolType="boolProps">
        <input class="form-check-input" type="checkbox" :checked="boolProps.data" />
      </template>
    </Table>
  </div>
</template>

<script>
import Alert from "../components/Alert.vue";
import Table from "../components/Table.vue";
import TableColumnBadges from "../components/TableColumnBadges.vue";
import { getUsers, putUser } from "../api/api";
import { onMounted, ref } from "vue";

export default {
  name: "Users",
  components: {
    Alert,
    Table,
    TableColumnBadges,
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
      error: false,
      success: false,
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
  methods: {
    toggleAddRow() {
      this.addRow = !this.addRow;
    },
    updateUser() {
        console.log("TODO")
    },
    saveUser() {
      this.errorMessage = false;
      this.success = false;
      const response = putUser(this.newUser);
      response
        .then(() => {
          this.success = true;
          this.toggleAddRow();
          this.loadUsers();
          this.clearUser();
        })
        .catch((error) => {
          this.error = `${error}`;
        });
    },
    clearUser() {
      Object.keys(this.newUser).forEach((key) => {
        this.newUser[key] = "";
      });
      this.newUser.admin = false;
      this.newUser.projects = []
    },
  },
};
</script>
