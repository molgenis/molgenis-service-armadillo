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
import { ListOfObjectsWithStringKey, StringArray } from "@/types/types";
import { defineComponent, PropType } from "vue";
import { isIntArray, transformTable, truncate } from "@/helpers/utils";

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
  },
  computed: {
    maxNumberCharacters() {
      // don't question the logic, it's a formula that figures out how many characters fit in each header label
      // but if you do question it:
      // maxWidth/200 spreads out nicely for 10 columns, to get 200 when the length of columns is 10, we do it times 20 (20 * l)
      // for 5 columns, this leaves a lot of whitespace. There maxWidth/50, rather than 100, fits better.
      // therefore, we need to substract 50 from the 20 * l if the number of columns is 5 and 0 if the number of columns is 1
      // to get that: (10 / l - 1) * 50, that's what we substract from the 20 * l
      // example (l = 10):
      // 20 * 10 = 200
      // 10 / 10 - 1 = 0 -> 0 * 50 = 0
      // 200 - 0 = 200
      // example (l = 5):
      // 20 * 5 = 100
      // 10 / 5 - 1 = 1 -> 1 * 50 = 50
      // 100 - 50 = 50
      const l = this.tableKeys.length;
      return Math.ceil(this.maxWidth / (20 * l - (10 / l - 1) * 50));
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
