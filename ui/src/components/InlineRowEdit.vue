<template>
  <tr class="align-middle table-info">
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
          v-if="Object.keys(dropDowns).includes(column)"
          v-model="rowData[column]"
          :availableOptions="dropDowns[column]"
          @update="emitUpdate"
        />
        <StringArrayInput v-else v-model="rowData[column]" />
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
        <textarea
          class="form-control p-2"
          v-model="rowData[column]"
          rows="1"
          style="
            white-space: normal;
            word-break: break-word;
            overflow: hidden;
            resize: none;
            font-family: inherit;
            line-height: 1.5;
          "
          @input="autoResize($event)"
        ></textarea>
      </div>
    </td>
  </tr>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ButtonGroup from "@/components/ButtonGroup.vue";
import StringArrayInput from "@/components/StringArrayInput.vue";
import DropdownArrayInput from "@/components/DropdownArrayInput.vue";
import KeyValueInput from "@/components/KeyValueInput.vue";
import { BootstrapType, StringArray, TypeObject } from "@/types/types";

export default defineComponent({
  name: "InlineRowEdit",
  emits: ["update-array-element"],
  components: {
    StringArrayInput,
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
    dropDowns: {
      type: Object as PropType<Record<string, StringArray>>,
      default: {},
    },
  },
  methods: {
    emitUpdate(event: Event) {
      this.$emit("update-array-element", event);
    },
    autoResize(e: Event) {
      const el = e.target as HTMLTextAreaElement;
      el.style.height = "auto";
      el.style.height = `${el.scrollHeight}px`;
    },
    resizeAll() {
      this.$nextTick(() => {
        const tbs = this.$el.querySelectorAll("textarea.form-control");
        tbs.forEach((ta: HTMLTextAreaElement) => {
          ta.style.height = "auto";
          ta.style.height = `${ta.scrollHeight}px`;
        });
      });
    },
  },
  mounted() {
    this.resizeAll();
  },
  updated() {
    this.resizeAll;
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
