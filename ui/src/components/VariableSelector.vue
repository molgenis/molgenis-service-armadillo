<template>
  <div class="row">
    <div class="col-5">
      <div class="row justify-content-end pe-0"></div>
      <div class="row m-1">
        <SearchBar
          class="mt-1 mb-2 col-12 align-self-end"
          v-model="searchString"
        />
        <div class="overflow-y-scroll variable-select border p-1 col-12">
          <div class="form-check" v-for="variable in getFilteredVariables()">
            <input
              class="form-check-input"
              type="checkbox"
              :value="variable"
              :checked="selectedVariables.includes(variable)"
              @change="updateVariables(variable)"
            />
            <label class="form-check-label" for="flexCheckDefault">
              {{ variable }}
            </label>
          </div>
        </div>
      </div>
    </div>
    <div class="col-7">
      <div class="row">
        <div class="col-2">
          <h3 class="mt-5">
            <i class="bi bi-arrow-right-circle-fill text-primary"></i>
          </h3>
        </div>
        <div class="col-10 overflow-y-scroll selected-variables border">
          <div v-for="variable in selectedVariables" class="fst-italic">
            {{ variable }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import SearchBar from "@/components/SearchBar.vue";
import { PropType, defineComponent } from "vue";
import { stringIncludesOtherString } from "@/helpers/utils";
import { StringArray } from "@/types/types";

export default defineComponent({
  name: "VariableSelector",
  props: {
    variables: {
      default: [],
      type: Array,
    },
    preselectedVariables: {
      default: [] as PropType<StringArray>,
      type: Array,
    },
  },
  components: {
    SearchBar,
  },
  data(): { selectedVariables: string[]; searchString: string } {
    return {
      selectedVariables: this.preselectedVariables as StringArray,
      searchString: "",
    };
  },
  methods: {
    updateVariables(variable: string) {
      if (this.selectedVariables.includes(variable)) {
        const index = this.selectedVariables.indexOf(variable);
        this.selectedVariables.splice(index, 1);
      } else {
        this.selectedVariables.push(variable);
      }
    },
    getFilteredVariables(): string[] {
      let variables = this.variables;
      if (this.searchString) {
        variables = this.variables.filter((variable) => {
          return stringIncludesOtherString(
            variable as string,
            this.searchString
          );
        });
      }
      return variables as string[];
    },
  },
});
</script>
<style :scoped>
.variable-select {
  height: 10em;
}
.selected-variables {
  height: 13em;
}
</style>
