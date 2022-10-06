<template>
  <InlineRowEdit
    :row="rowToEdit"
    :save="saveCallback"
    :cancel="cancelCallback"
  >
    <!-- Enable adding/removing multiple array elements -->
    <template #arrayEdit="array">
      <BadgeList
        :itemArray="rowToEdit[arrayColumn]"
        :row="array.row"
        :saveCallback="deleteArrayElementCallback"
      ></BadgeList>
      <Badge v-if="addArrayElementToRow">
        <input
          type="text"
          :value="modelValue"
          @input="$emit('update:modelValue', getValue($event))"
        />
        <button
          class="check-badge text-light bg-secondary"
          @click="saveArrayElementCallback"
        >
          <i class="bi bi-check-lg"></i>
        </button>
      </Badge>
      <button
        class="btn btn-primary btn-sm float-end"
        @click="addArrayElementCallback"
      >
        <i class="bi bi-plus-lg"></i>
      </button>
    </template>
  </InlineRowEdit>
</template>

<script lang="ts">
import Badge from "../components/Badge.vue";
import InlineRowEdit from "@/components/InlineRowEdit.vue";
import BadgeList from "@/components/BadgeList.vue";
import { getEventValue } from "@/helpers/utils";

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
  methods: {
    // not excactly sure why, but calling this method directly won't work
    getValue(event: Event) {
      return getEventValue(event);
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
