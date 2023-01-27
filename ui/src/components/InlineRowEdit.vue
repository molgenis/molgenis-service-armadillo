<template>
  <tr class="align-middle">
    <th scope="row">
      <ButtonGroup
        :buttonIcons="buttonIcons"
        :buttonColors="buttonTypes"
        :clickCallbacks="[save, cancel]"
        :disabled="disabled"
      ></ButtonGroup>
    </th>
    <td v-for="(type, column) in dataStructure" :key="column">
      <div v-if="hideColumns.includes(column)">
        <!-- skipped column {{column}}-->
      </div>
      <div v-else-if="immutable.includes(column)">
        {{ rowData[column] }}
      </div>
      <div v-else-if="type === 'array'">
        <DropdownArrayInput
          v-model="rowData[column]"
          :availableOptions="availableOptions"
          @update="emitUpdate"
        />
      </div>
      <div v-else-if="type === 'object'">
        <KeyValueInput v-model="rowData[column]" />
      </div>
      <div v-else-if="type === 'boolean'">
        <input
          class="form-check-input"
          type="checkbox"
          v-model="rowData[column]"
        />
      </div>
      <div class="input-group mb-3" v-else>
        <input
          type="text"
          class="form-control"
          v-model="rowData[column]"
          :placeholder="rowData[column]"
          :aria-label="column"
        />
      </div>
    </td>
  </tr>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ButtonGroup from "@/components/ButtonGroup.vue";
import DropdownArrayInput from "@/components/DropdownArrayInput.vue";
import KeyValueInput from "@/components/KeyValueInput.vue";
import { BootstrapType, StringArray, TypeObject } from "@/types/types";

export default defineComponent({
  name: "InlineRowEdit",
   emits: ["update-array-element"],
  components: {
    KeyValueInput,
    DropdownArrayInput,
    ButtonGroup,
  },
  props: {
    disabled: {
      type: Boolean,
    },
    save: {
      type: Function,
      required: true,
    },
    cancel: {
      type: Function,
      required: true,
    },
    row: {
      type: Object as PropType<
        Record<string, StringArray | Object | boolean | string>
      >,
      required: true,
    },
    hideColumns: {
      type: Array,
      default: [],
    },
    immutable: {
      type: Array,
      default: [],
    },
    dataStructure: {
      type: Object as PropType<TypeObject>,
      required: true,
    },
    availableOptions: Array as PropType<StringArray>,
  },
  methods: {
    emitUpdate(event: Event) {
      this.$emit("update-array-element", event);
    },
  },
  data(): {
    buttonIcons: StringArray;
    buttonTypes: BootstrapType[];
    rowData: Record<string, StringArray | Object | boolean | string>;
  } {
    return {
      buttonIcons: ["check-lg", "x-lg"],
      buttonTypes: ["success", "danger"],
      rowData: this.row,
    };
  },
});
</script>
