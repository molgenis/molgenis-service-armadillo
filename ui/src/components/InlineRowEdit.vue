<template>
  <tr>
    <th scope="row">
      <ButtonGroup
        :buttonIcons='(["check-lg", "x-lg"] as StringArray)'
        :buttonColors='(["success", "danger"] as BootstrapType[])'
        :clickCallbacks="[save, cancel]"
      ></ButtonGroup>
    </th>
    <td v-for="(type, column) in dataStructure">
      <div v-if="hideColumns.includes(column)">
        <!-- skipped column {{column}}-->
      </div>
      <div v-else-if="type === 'array'">
        <StringArrayInput v-model="(rowData[column] as StringArray)"/>
      </div>
      <div v-else-if="type === 'object'">
        <KeyValueInput v-model="(rowData[column] as Object)"/>
      </div>
      <div v-else-if="type === 'boolean'">
        <input
          class="form-check-input"
          type="checkbox"
          v-model="(rowData[column] as boolean)"
        />
      </div>
      <div class="input-group mb-3" v-else>
        <input
          type="text"
          class="form-control"
          v-model="(rowData[column] as string)"
          :placeholder="rowData[column]"
          :aria-label="(column as string)"
        />
      </div>
    </td>
  </tr>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ButtonGroup from "@/components/ButtonGroup.vue";
import StringArrayInput from "@/components/StringArrayInput.vue";
import KeyValueInput from "@/components/KeyValueInput.vue";
import { StringArray, BootstrapType, TypeObject } from "@/types/types";

export default defineComponent({
  name: "InlineRowEdit",
  components: {
    KeyValueInput,
    StringArrayInput,
    ButtonGroup,
  },
  props: {
    save: {
      type: Function,
      required: true,
    },
    cancel: {
      type: Function,
      required: true,
    },
    row: {
      type: Object,
      required: true,
    },
    hideColumns: {
      type: Array,
      default: []
    },
    dataStructure: {
      type: Object as PropType<TypeObject>,
      required: true
    }
  },
  data() {
    return {
      rowData: this.row,
    };
  },
});
</script>
