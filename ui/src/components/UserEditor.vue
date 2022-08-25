<template>
  <InlineRowEdit
    :row="this.userToEdit"
    :save="this.saveCallback"
    :cancel="this.cancelCallback"
  >
    <!-- Enable adding/removing multiple projects -->
    <template #arrayEdit="array">
      <TableColumnBadges
        :itemArray="this.userToEdit.projects"
        :row="array.row"
        :saveCallback="this.deleteProjectCallback"
      ></TableColumnBadges>
      <Badge v-if="this.addProjectToRow">
        <input
          type="text"
          :value="modelValue"
          @input="$emit('update:modelValue', $event.target.value)"
        />
        <button
          class="check-badge text-light bg-secondary"
          @click="this.saveProjectCallback"
        >
          <i class="bi bi-check-lg"></i>
        </button>
      </Badge>
      <button
        class="btn btn-primary btn-sm float-end"
        @click="this.addProjectCallback"
      >
        <i class="bi bi-plus-lg"></i>
      </button>
    </template>
  </InlineRowEdit>
</template>
<script>
import Badge from "../components/Badge.vue";
import InlineRowEdit from "../components/InlineRowEdit.vue";
import TableColumnBadges from "../components/TableColumnBadges.vue";

export default {
  name: "UserEditor",
  components: {
    Badge,
    InlineRowEdit,
    TableColumnBadges,
  },
  props: {
    userToEdit: { String: String },
    saveCallback: Function,
    cancelCallback: Function,
    addProjectCallback: Function,
    deleteProjectCallback: Function,
    saveProjectCallback: Function,
    addProjectToRow: Boolean,
    // projectToAdd
    modelValue: String,
  },
};
</script>
