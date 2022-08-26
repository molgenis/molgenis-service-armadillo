<template>
  <InlineRowEdit
    :row="this.rowToEdit"
    :save="this.saveCallback"
    :cancel="this.cancelCallback"
  >
    <!-- Enable adding/removing multiple array elements -->
    <template #arrayEdit="array">
      <BadgeList
        :itemArray="this.rowToEdit[arrayColumn]"
        :row="array.row"
        :saveCallback="this.deleteArrayElementCallback"
      ></BadgeList>
      <Badge v-if="this.addArrayElementToRow">
        <input
          type="text"
          :value="modelValue"
          @input="$emit('update:modelValue', $event.target.value)"
        />
        <button
          class="check-badge text-light bg-secondary"
          @click="this.saveArrayElementCallback"
        >
          <i class="bi bi-check-lg"></i>
        </button>
      </Badge>
      <button
        class="btn btn-primary btn-sm float-end"
        @click="this.addArrayElementCallback"
      >
        <i class="bi bi-plus-lg"></i>
      </button>
    </template>
  </InlineRowEdit>
</template>
<script>
import Badge from "../components/Badge.vue";
import InlineRowEdit from "../components/InlineRowEdit.vue";
import BadgeList from "../components/BadgeList.vue";

export default {
  name: "TableRowEditor",
  components: {
    Badge,
    InlineRowEdit,
    BadgeList,
  },
  props: {
    rowToEdit: { String: String },
    arrayColumn: String,
    saveCallback: Function,
    cancelCallback: Function,
    addArrayElementCallback: Function,
    deleteArrayElementCallback: Function,
    saveArrayElementCallback: Function,
    addArrayElementToRow: Boolean,
    // v-model to the element to model the input of the new array element to
    modelValue: String,
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
