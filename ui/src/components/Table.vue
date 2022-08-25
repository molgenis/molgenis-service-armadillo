<template>
  <table class="table">
    <thead>
      <tr>
        <slot name="extraHeader"></slot>
        <th scope="col" v-for="head in this.capitalizedHeaders">
          {{ head }}
        </th>
      </tr>
    </thead>
    <tbody>
      <slot name="extraRow"></slot>
      <template v-for="(item, index) in data">
        <tr v-if="index != this.indexToEdit">
          <slot name="extraColumn" :item="item"></slot>
          <td v-for="value in item">
            <span v-if="Array.isArray(value)">
              <slot name="arrayType" :data="value" :row="item">
                {{ value }}
              </slot>
            </span>
            <span v-else-if="typeof value == 'boolean'">
              <slot name="boolType" :data="value" :row="item">
                {{ value }}
              </slot>
            </span>
            <span v-else>{{ value }}</span>
          </td>
        </tr>
        <slot name="editRow" :row="item" v-else></slot>
      </template>
    </tbody>
  </table>
</template>

<script>
import { toCapitalizedWords } from "../helpers/utils.js";

export default {
  name: "Table",
  props: {
    data: Array,
    idCol: String,
    indexToEdit: Number,
  },
  computed: {
    capitalizedHeaders() {
     return this.data.length !== 0 ? Object.keys(this.data[0]).map((head) => toCapitalizedWords(head)) : [];
    },
  },
};
</script>
