<template>
  <InlineRowEdit :row="rowToEdit" :save="saveCallback" :cancel="cancelCallback" :hideColumns="hideColumns"/>
</template>

<script lang="ts">
import Badge from "../components/Badge.vue";
import InlineRowEdit from "@/components/InlineRowEdit.vue";
import BadgeList from "@/components/BadgeList.vue";
import {getEventValue} from "@/helpers/utils";
import {ObjectWithStringKey} from "@/types/types";
import {PropType} from "vue";

export default {
  name: "TableRowEditor",
  components: {
    Badge,
    InlineRowEdit,
    BadgeList,
  },
  props: {
    rowToEdit: {type: Object as PropType<ObjectWithStringKey>, required: true},
    arrayColumn: {type: String, required: true},
    saveCallback: {type: Function, required: true},
    cancelCallback: {type: Function, required: true},
    hideColumns: {type: Array, default: []}
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
