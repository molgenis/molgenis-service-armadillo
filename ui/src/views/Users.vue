<template>
  <div>
    <Table :data="users">
      <template #arrayType="arrayProps">
        <TableColumnBadges :data="arrayProps.data"></TableColumnBadges>
      </template>
    </Table>
  </div>
</template>

<script>
import Table from "../components/Table.vue";
import { getUsers } from "../api/api";
import { onMounted, ref } from "vue";
import TableColumnBadges from "../components/TableColumnBadges.vue";
export default {
  name: "Users",
  components: {
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
};
</script>
