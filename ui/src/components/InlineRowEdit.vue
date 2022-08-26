<template>
  <tr>
    <th scope="row">
      <ButtonGroup
        :buttonIcons="['check-lg', 'x-lg']"
        :buttonColors="['success', 'danger']"
        :clickCallbacks="[this.save, this.cancel]"
      ></ButtonGroup>
    </th>
    <td v-for="(value, column) in this.rowData">
      <div v-if="Array.isArray(value)">
        <slot name="arrayEdit" :arrayData="value" :row="this.rowData">
          <input
            type="text"
            class="form-control"
            v-model="rowData[column][0]"
            :placeholder="column"
            :aria-label="column"
          />
        </slot>
      </div>
      <div v-else-if="typeof value == 'boolean'">
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
          :placeholder="column"
          :aria-label="column"
        />
      </div>
    </td>
  </tr>
</template>

<script>
import ButtonGroup from "../components/ButtonGroup.vue";
export default {
  name: "InlineRowEdit",
  components: {
    ButtonGroup,
  },
  props: {
    save: Function,
    cancel: Function,
    row: { String: String },
  },
  data() {
    return {
      rowData: this.row,
    };
  },
};
</script>
