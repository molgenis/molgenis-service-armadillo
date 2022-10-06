<template>
  <tr>
    <th scope="row">
      <ButtonGroup
        :buttonIcons='(["check-lg", "x-lg"] as StringArray)'
        :buttonColors='(["success", "danger"] as BootstrapType[])'
        :clickCallbacks="[save, cancel]"
      ></ButtonGroup>
    </th>
    <td v-for="(value, column) in rowData">
      <div v-if="Array.isArray(value)">
        <slot name="arrayEdit" :arrayData="value" :row="rowData">
          <input
            type="text"
            class="form-control"
            v-model="(rowData[column][0] as string)"
            :placeholder="value[0]"
            :aria-label="column"
          />
        </slot>
      </div>
      <div v-else-if="typeof value == 'boolean'">
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
          :placeholder="value"
          :aria-label="column"
        />
      </div>
    </td>
  </tr>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { StringArray, BootstrapType } from "@/types/types";
import ButtonGroup from "../components/ButtonGroup.vue";

export default defineComponent({
  name: "InlineRowEdit",
  components: {
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
  },
  data() {
    return {
      rowData: this.row,
    };
  },
});
</script>
