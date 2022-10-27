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
      <tr v-for="(row, index) in data">
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
import { StringObject } from "@/types/types";
import { defineComponent, PropType } from "vue";
import { truncate } from "@/helpers/utils";

export default defineComponent({
  name: "SimpleTable",
  props: {
    data: {
      type: Array as PropType<StringObject[]>,
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
    // dataToPreview() {
    //   const preview: StringObject[] = [];
    //   this.data.forEach((row) => {
    //     const newRow: StringObject = {};
    //     this.tableHeader.forEach((key) => {
    //       newRow[key] = row[key];
    //     });
    //     preview.push(newRow);
    //   });
    //   return preview;
    // },
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
