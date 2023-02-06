<template>
  <div>
    <BadgeList
      :item-array="modelValue"
      :canEdit="true"
      
    />
    <div v-if="!showAdd">
      <button class="btn-add-value btn btn-link p-0" @click="showAdd = true">
        <i class="bi bi-plus-circle text-primary"></i>
      </button>
    </div>
    <div v-else class="pt-0">
      <input
        class="array-element-input mt-1"
        list="datalistOptions"
        id="arrayInput"
        placeholder="Type to search..."
        v-model="newValue"
      />
      <datalist id="datalistOptions">
        <option v-for="option in availableOptions" :value="option" :key="option"></option>
      </datalist>
      <div class="btn-group mt-0" role="group">
        <button
          class="btn btn-sm check-badge btn-success me-0 add-new-value"
          @click="addNewValue"
        >
          <i class="bi bi-check"></i>
        </button>
        <button
          class="btn btn-sm check-badge btn-danger cancel-new-value"
          @click="cancelNewValue"
        >
          <i class="bi bi-x"></i>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
button.check-badge {
  border: none;
  padding: 0;
  margin-left: 0.2em;
  margin-right: -0.2em;
}
</style>

<script lang="ts">
import BadgeList from "@/components/BadgeList.vue";
import { StringArray } from "@/types/types";
import { defineComponent, PropType } from "vue";

export default defineComponent({
  name: "DropdownArrayInput",
  components: { BadgeList },
  props: {
    modelValue: { type: Array as PropType<StringArray>, required: true },
    availableOptions: { type: Array as PropType<StringArray> },
  },
  emits: ["update"],
  data() {
    return {
      showAdd: false,
      newValue: "",
    };
  },
  methods: {
    addNewValue() {
      this.$emit("update", this.newValue);
      this.showAdd = false;      
      this.newValue = "";
    },
    cancelNewValue() {
      this.newValue = "";
      this.showAdd = false;
    },
  },
});
</script>
