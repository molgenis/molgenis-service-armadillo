<template>
  <table
    class="table table-bordered table-sm simple-table"
    v-if="data.length > 0"
  >
    <thead>
      <tr>
        <!-- for each key of the first element of data-->
        <th scope="col" v-for="key in tableHeader">
          {{ key }}
        </th>
        <th scope="col">...</th>
      </tr>
    </thead>
    <tbody class="table-group-divider">
      <!-- for each row-->
      <tr v-for="(row, index) in dataToPreview">
        <!-- for each value in row -->
        <td v-for="value in row">
          {{ value }}
        </td>
        <!-- if index is 0 -->
        <!-- <td rowspan="10" v-if="index === 0" class="fst-italic">+ 102</td> -->
        <td rowspan="10" v-if="index === 0" class="fst-italic">+ more</td>
      </tr>
      <tr class="text-end fst-italic">
        <!-- <td colspan="11">1470 more rows</td> -->
        <td colspan="11">+ more rows</td>
      </tr>
    </tbody>
  </table>
</template>

<script lang="ts">
import {
  ListOfObjectsWithStringKey,
  StringArray,
} from "@/types/types";
import { defineComponent, PropType } from "vue";
import { isIntArray, transformTable, truncate } from "@/helpers/utils";

export default defineComponent({
  name: "SimpleTable",
  props: {
    data: {
      type: Array as PropType<{[key: string]: string}[]>,
      required: true,
    },
    maxWidth: {
      type: Number,
      required: true,
    },
  },
  computed: {
    maxNumberCharacters() {
      return Math.ceil(this.maxWidth / 200);
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
        let newRow: {[key: string]: string | number} = {};
        Object.keys(row).forEach((key) => {
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
        ? Object.keys(this.data[0]).map((item) => {
            return truncate(item, this.maxNumberCharacters);
          })
        : [];
    },
    numberOfColumnsToPreview() {
      return Math.ceil(this.maxWidth / 100);
    },
  },
});
</script>
