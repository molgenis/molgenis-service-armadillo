<template>
  <table class="table">
    <thead>
      <tr>
        <slot name="extraHeader"></slot>
        <th scope="col" v-for="head in capitalizedHeaders">
          {{ head }}
        </th>
      </tr>
    </thead>
    <tbody>
      <slot name="extraRow"></slot>
      <template v-for="item in dataToShow">
        <tr v-if="getIndex(item) != indexToEdit">
          <slot name="extraColumn" :item="item"></slot>
          <td v-for="(value, propertyName) in item">
            <span v-if="customColumns.includes(propertyName as string)">
              <slot name="customType" :data="value" :row="item">
                custom {{ value }}
              </slot>
            </span>
            <span v-else-if="Array.isArray(value)">
              <BadgeList :item-array="value" />
            </span>
            <span v-else-if="typeof value === 'object'">
              <slot name="objectType" :data="value" :row="item">
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

<script lang="ts">
import {
  ListOfObjectsWithStringKey,
  ObjectWithStringKey,
  StringArray,
} from "@/types/types";
import { defineComponent, PropType } from "vue";
import { toCapitalizedWords } from "@/helpers/utils";
import BadgeList from "@/components/BadgeList.vue";

export default defineComponent({
  name: "Table",
  components: { BadgeList },
  props: {
    // filtered data
    dataToShow: {
      type: Array as PropType<ListOfObjectsWithStringKey>,
      required: true,
    },
    // all data to ensure we have correct index when editing
    allData: {
      type: Array as PropType<ListOfObjectsWithStringKey>,
      required: true,
    },
    indexToEdit: {
      type: Number,
      required: true,
    },
    customColumns: {
      type: Array as PropType<StringArray>,
      default: [],
    },
  },
  computed: {
    capitalizedHeaders() {
      return this.dataToShow.length !== 0
        ? Object.keys(this.dataToShow[0]).map((head) =>
            toCapitalizedWords(head)
          )
        : [];
    },
  },
  methods: {
    getIndex(itemToFind: ObjectWithStringKey) {
      return this.allData.findIndex((item) => {
        return item === itemToFind;
      });
    },
  },
});
</script>
