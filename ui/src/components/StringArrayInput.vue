<template>
  <div>
    <BadgeList
      :item-array="modelValue"
      :canEdit="true"
      @update="$emit('update')"
    />
    <div v-if="!showAdd">
      <button class="btn-add-value btn btn-link p-0" @click="showAdd = true">
        <i class="bi bi-plus-circle text-primary"></i>
      </button>
    </div>
    <div v-else class="pt-0">
      <input type="text" class="array-element-input mt-1" v-model="newValue" />
      <div class="btn-group mt-0" role="group" aria-label="Basic example">
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
  name: "StringArrayInput",
  components: { BadgeList },
  props: {
    modelValue: { type: Array as PropType<StringArray>, required: true },
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
      const result = this.modelValue;
      result.push(this.newValue);
      this.newValue = "";
      this.showAdd = false;
      this.$emit("update", result);
    },
    cancelNewValue() {
      this.newValue = "";
      this.showAdd = false;
    },
  },
});
</script>
