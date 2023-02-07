<template>
  <table class="table">
    <thead>
      <tr>
        <slot name="extraHeader"></slot>
        <th scope="col" v-for="head in capitalizedHeaders" :key="head">
          {{ head }}
        </th>
      </tr>
    </thead>
    <tbody>
      <slot name="extraRow"></slot>
      <template
        v-for="(dataRow, dataRowIndex) in dataToShow"
        :key="dataRowIndex"
      >
        <tr
          v-if="getIndex(dataRow) != indexToEdit"
          class="align-middle"
          :class="
            getIndex(dataRow) == highlightedRowIndex ? 'table-success' : ''
          "
        >
          <slot name="extraColumn" :item="dataRow"></slot>
          <td
            v-for="(type, propertyName) in dataStructure"
            :key="type"
            class="table-column"
          >
            <span v-if="customColumns.includes(propertyName)">
              <slot
                name="customType"
                :data="dataRow[propertyName]"
                :row="dataRow"
              >
                {{ dataRow[propertyName] }}
              </slot>
            </span>
            <span v-else-if="type === 'array'">
              <BadgeList :item-array="dataRow[propertyName]" />
            </span>
            <span v-else-if="type === 'object'">
              <slot
                name="objectType"
                :data="dataRow[propertyName]"
                :row="dataRow"
              >
                {{ dataRow[propertyName] }}
              </slot>
            </span>
            <span v-else-if="type === 'boolean'">
              <slot
                name="boolType"
                :data="dataRow[propertyName]"
                :row="dataRow"
              >
                {{ dataRow[propertyName] }}
              </slot>
            </span>
            <span v-else>{{ dataRow[propertyName] }}</span>
          </td>
        </tr>
        <slot name="editRow" :row="dataRow" v-else></slot>
      </template>
    </tbody>
  </table>
</template>

<script lang="ts">
import {
  ListOfObjectsWithStringKey,
  ObjectWithStringKey,
  StringArray,
  TypeObject,
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
    dataStructure: {
      type: Object as PropType<TypeObject>,
      required: true,
    },
    highlightedRowIndex: {
      type: Number,
      default: -1,
    },
  },
  computed: {
    capitalizedHeaders() {
      return Object.keys(this.dataStructure).map((head) =>
        toCapitalizedWords(head)
      );
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
<style lang="scss" scoped>
.table-column {
  max-width: 30vw;
}
</style>
