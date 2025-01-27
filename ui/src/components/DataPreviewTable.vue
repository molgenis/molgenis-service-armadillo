<template>
  <table
    class="table table-bordered table-sm simple-table"
    v-if="data.length > 0"
  >
    <thead>
      <tr>
        <!-- for each key of the first element of data-->
        <slot name="extraHeader"></slot>
        <th scope="col" v-if="maxWidth" v-for="key in tableHeader" :key="key">
          {{ key }}
        </th>
        <th scope="col" v-else v-for="(key, index) in tableKeys" :key="index">
          {{ toCapitalizedWords(key) }}
        </th>
      </tr>
    </thead>
    <tbody class="table-group-divider">
      <!-- for each row-->
      <tr v-for="(row, index) in dataToPreview" :key="index">
        <!-- additional column for each row (row header) -->
        <slot name="extraColumn" :item="row"></slot>
        <!-- for each value in row -->
        <td v-for="(value, key, index) in row" :key="key">
          <span
            v-if="
              maxWidth && value.toString().length > tableHeader[index].length
            "
          >
            {{ value.toString().slice(0, tableHeader[index].length - 2) }}..
          </span>
          <span v-else>
            {{ value }}
          </span>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script lang="ts">
import { ListOfObjectsWithStringKey, StringArray } from "@/types/types";
import { defineComponent, PropType } from "vue";
import {
  isIntArray,
  transformTable,
  truncate,
  toCapitalizedWords,
} from "@/helpers/utils";

export default defineComponent({
  name: "DataPreviewTable",
  props: {
    data: {
      type: Array as PropType<{ [key: string]: string | number }[]>,
      required: true,
    },
    maxWidth: {
      type: Number,
      required: false,
    },
    nRows: {
      type: Number,
      required: true,
    },
    sortedHeaders: {
      type: Array,
      required: false,
    },
  },
  computed: {
    maxNumberCharacters() {
      const l = this.tableKeys.length;
      // max width divided by number of characters * fontsize to evenly spread headers
      return this.maxWidth ? Math.floor(this.maxWidth / (l * 16)) : undefined;
    },
    dataToPreview() {
      // converting ints to in, otherwise the id numbers look awkward
      const dataToPreview: ListOfObjectsWithStringKey = [];
      const transformedTable = transformTable(this.data);
      const intKeys: StringArray = [];
      Object.keys(transformedTable).forEach((key) => {
        if (isIntArray(transformedTable[key])) {
          intKeys.push(key);
        }
      });
      this.data.forEach((row) => {
        let newRow: { [key: string]: string | number } = {};
        this.tableKeys.forEach((key) => {
          if (intKeys.includes(key)) {
            newRow[key] = parseInt(row[key]);
          } else {
            newRow[key] = row[key];
          }
        });
        dataToPreview.push(newRow);
      });
      return dataToPreview;
    },
    tableHeader() {
      return this.data.length > 0
        ? this.tableKeys.map((item) => {
            return item.length > this.maxNumberCharacters + 2
              ? truncate(item, this.maxNumberCharacters)
              : item;
          })
        : [];
    },
    tableKeys() {
      return this.sortedHeaders
        ? this.sortedHeaders
        : Object.keys(this.data[0]);
    },
  },
  methods: {
    toCapitalizedWords,
  },
});
</script>
