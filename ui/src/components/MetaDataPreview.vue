<template>
  <div>
    <div class="text-secondary mt-1">
      <div class="filter float-end">
        <div class="row">
          <div class="col">
            <button class="btn btn-primary btn-sm float-end" @click="isFilterCollapsed = !isFilterCollapsed">
              <i class="bi bi-caret-down-fill" v-if="isFilterCollapsed"></i><i class="bi bi-caret-up-fill" v-else></i>
              Filter <i class="bi bi-funnel"></i>
            </button>
          </div>
        </div>
        <div class="row">
          <div class="col">
            <div class="text-sm-start border rounded p-2" v-show="!isFilterCollapsed">
          <div class="form-check">
            <input class="form-check-input" type="radio" name="radioDefault" value="none" id="radioNone" checked v-model="filterColumn">
            <label class="form-check-label" for="radioNone">
              None
            </label>
          </div>
          <div class="form-check">
            <input class="form-check-input" type="radio" name="radioDefault" value="column" id="radioDatatype" v-model="filterColumn">
            <label class="form-check-label" for="radioDatatype">
              Column search 
            </label>
            <div v-show="filterColumn == 'column'">
              <button class="btn btn-sm btn-secondary ms-2" style="margin-top:-4px; border-top-right-radius: 0px; border-bottom-right-radius: 0px;"><i class="bi bi-search"></i></button>
              <input style="padding: 2px; border-top-right-radius: 4px; border-bottom-right-radius: 4px" name="columnFilter" class="border-1 border-secondary" v-model="columnFilter"></input>
            </div>
          </div>
          <div class="form-check" v-if="typeOptions.length > 1">
            <input class="form-check-input" type="radio" name="radioDefault" value="datatype" id="radioDatatype" v-model="filterColumn">
            <label class="form-check-label" for="radioDatatype">
              Datatype 
            </label>
            <Dropdown v-show="filterColumn === 'datatype'" :options="typeOptions" @update="updateSelectedType" ref="dropdown"></Dropdown>
          </div>
          <div class="form-check">
            <input class="form-check-input" type="radio" name="radioDefault" value="missing" id="radioMissing" v-model="filterColumn">
            <label class="form-check-label" for="radioMissing">
              Missing
            </label>
            <span class="ms-2" style="width: 5rem;" v-show="filterColumn == 'missing'">
              <button class="btn btn-sm btn-secondary" style="margin-top:-3px; border-top-right-radius: 0px; border-bottom-right-radius: 0px;" @click="toggleMissingFilter">{{missingFilter}}=</button>
               <input type="number" min=0 max=100 class="border-1 border-secondary" style="padding: 2px; border-top-right-radius: 4px; border-bottom-right-radius: 4px" name="missingPercentage" v-model="missingFilterValue"> %
              </span>
          </div>
         </div>
          </div>
        </div>
      </div>
      <table class="table">
      <thead>
        <tr>
          <th>Columnname</th>
          <th>Data type</th>
          <th>Missing</th>
          <th v-if="typeOptions.includes('BINARY')">Levels</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(value, key) in metadata" :key="key" 
        v-show="showLine(key, value.type, value.missing)">
          <td>{{key}}</td>
          <td>{{getEmxDataType(value.type, value.levels)}}<br/> <span class="badge text-bg-secondary">{{value.type}}</span></td>
          <td>{{value.missing}} ({{ getPercentageOfMissing(value.missing) }}%)</td>
          <td  v-if="typeOptions.includes('BINARY')">{{value.levels?.join(", ")}}</td>
        </tr>
      </tbody>
    </table>
    </div>
  </div>
</template>

<script lang="ts">
import { toPercentage } from "@/helpers/utils";
import { defineComponent } from "vue";
import Dropdown from "./Dropdown.vue";
import { StringArray } from "@/types/types";

export default defineComponent({
  name: "MetaDataPreview",
  components: {
    Dropdown
  },
  props: {
    metadata: Object 
  },
  data(): {isFilterCollapsed: boolean, filterColumn: string, missingFilter: string, missingFilterValue: null|number, selectedType: string, columnFilter: string} {
    return {
      isFilterCollapsed: true,
      filterColumn: "none",
      missingFilter: '>',
      missingFilterValue: null,
      selectedType: '',
      columnFilter: ''
    };
  },
  computed: {
    typeOptions() {
      let options: string[] = [];
      if (this.metadata) {
        Object.values(this.metadata).forEach((element: {type: string, missing: string, levels: string[]}) => {
          if (!options.includes(element.type) ){
            options.push(element.type);
          }
        });
      }
      return options;
    }
  },
  methods: {
    updateSelectedType() {
      this.selectedType = this.$refs.dropdown.selectedOption;
    },
    toggleMissingFilter() {
      if (this.missingFilter == '>') {
        this.missingFilter = '<'
      } else {
        this.missingFilter = '>'
      }
    },
    showLine(column: string, datatype: string, missing: string) {
      if (this.filterColumn === 'none') {
        return true;
      } else if (this.filterColumn === 'column') {
        if (this.columnFilter === '') {
          return true;
        } else {
          return column.toLowerCase().includes(this.columnFilter.toLowerCase());
        }
      } else if (this.filterColumn === 'datatype') {
        if (this.selectedType === '') {
          return true;
        } else {
          return datatype === this.selectedType;
        }
      } else if (this.filterColumn === 'missing') {
        if (this.missingFilterValue === null) {
          return true;
        } else {
          const percentage = this.getPercentageOfMissing(missing);
          if (this.missingFilter == '>') {
            return percentage >= this.missingFilterValue;
          } else {
            return percentage <= this.missingFilterValue;
          } 
        }
      }
    },
    getEmxDataType(datatype: string, levels: StringArray | undefined) {
      if (datatype === "BINARY") {
        if (levels !== undefined && levels.length > 0) {
          return "Foreign key";
        } else {
          return "String";
        }
      } else if ( datatype === "INT32") {
        return "Integer";
      } else if ( datatype === "DOUBLE") {
        return "Decimal";
      } else if ( datatype === "BOOLEAN") {
        return "Boolean";
      } else {
        return datatype;
      }
    },
    getPercentageOfMissing(missingString: string): number {
      const missingInfo = missingString.split('/');
      return Number(toPercentage(Number(missingInfo[0]), Number(missingInfo[1])).toFixed(1));
    }
  },
});
</script>
