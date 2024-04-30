<template>
  <table
    class="table table-bordered table-sm simple-table"
    v-if="data.length > 0"
  >
    <thead>
      <tr>
        <!-- for each key of the first element of data-->
        <th scope="col" v-for="key in tableHeader" :key="key">
          {{ key }}
        </th>
        <!-- Not required if there are not at least 11 columns -->
        <th v-if="nCols > 10" scope="col">...</th>
      </tr>
    </thead>
    <tbody class="table-group-divider">
      <!-- for each row-->
      <tr v-for="(row, index) in dataToPreview" :key="index">
        <!-- for each value in row -->
        <td v-for="(value, key) in row" :key="key">
          {{ value }}
        </td>
        <!-- Will display the "+ x more" if 11 or more columns are present, else hides the column entirely -->
        <td rowspan="10" v-if="index === 0 && nCols > 10" class="fst-italic">
          {{ `+ ${nCols - 10} more` }}
        </td>
      </tr>
      <tr class="text-end fst-italic">
        <!-- Same goes for rows, only display when there are at least 11 or more rows -->
        <td colspan="11" v-if="nRows > 10">
          {{ `+ ${nRows - 10} more rows` }}
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script lang="ts">
import { ListOfObjectsWithStringKey, StringArray } from "@/types/types";
import { defineComponent, PropType } from "vue";
import { isIntArray, transformTable, truncate } from "@/helpers/utils";
import { timingSafeEqual } from "crypto";

export default defineComponent({
  name: "SimpleTable",
  props: {
    data: {
      type: Array as PropType<{ [key: string]: string }[]>,
      required: true,
    },
    maxWidth: {
      type: Number,
      required: true,
    },
    nRows: {
      type: Number,
      required: true,
    },
    nCols: {
      type: Number,
      required: true,
    },
  },
  computed: {
    maxNumberCharacters() {
      const l = this.tableKeys.length;
      // max width divided by number of characters * fontsize to evenly spread headers
      return Math.floor(this.maxWidth / (l * 16));
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
    tableKeys() {
      return Object.keys(this.data[0]);
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
  },
});
</script>
