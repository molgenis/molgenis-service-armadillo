<template>
  <div>
    <h5>DataSHIELD options</h5>
    <!-- options passed in props and are now shown here if starting with nfilter and datashield
  Descriptions will be in data -->
    <div v-for="(description, key) in getDsDescriptions()">
      <label for="containerName" class="form-label fw-bold">
        {{ toCapitalizedWords(key) }}
      </label>
      <input
        type="text"
        class="form-control container-input"
        v-model="editedOptions[key]"
      />
      <div v-html="description" class="form-text mb-1"></div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { getDataType, toCapitalizedWords } from "@/helpers/utils";
import { getDsDescriptions } from "@/helpers/dsInfo";

export default defineComponent({
  name: "DataShieldOptions",
  data() {
    return {
      editedOptions: this.options,
    };
  },
  methods: {
    getDataType,
    toCapitalizedWords,
    getDsDescriptions,
  },
  props: {
    options: {
      type: Object,
    },
  },
});
</script>
