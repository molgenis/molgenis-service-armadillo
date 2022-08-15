<template>
  <div>
    <Table :data="users"></Table>
  </div>
</template>

<script>
import Table from "../components/Table.vue";
import { getUsers } from "../api/api";
import { onMounted, ref } from "vue";
export default {
  name: "Users",
  components: {
    Table,
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
  props: {}
};
</script>
