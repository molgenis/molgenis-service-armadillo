<template>
  <tr>
    <th scope="row">
      <div class="btn-group" role="group">
        <button
          type="button"
          class="btn btn-success btn-sm bg-success"
          @click="this.save"
        >
          <i class="bi bi-check-lg"></i>
        </button>
        <button
          type="button"
          class="btn btn-danger btn-sm bg-danger"
          @click="this.clear"
        >
          <i class="bi bi-x-lg"></i>
        </button>
      </div>
    </th>
    <td v-for="(value, column) in this.rowData">
      <div class="input-group mb-3" v-if="Array.isArray(value)">
        <input
          type="text"
          class="form-control"
          v-model="rowData[column][0]"
          :placeholder="column"
          :aria-label="column"
        />
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
export default {
  name: "InlineRowEdit",
  props: {
    save: Function,
    clear: Function,
    row: { String: String },
  },
  data() {
    return {
      rowData: this.row,
    };
  },
};
</script>
