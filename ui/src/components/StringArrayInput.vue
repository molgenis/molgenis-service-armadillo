<template>
  <div>
    <BadgeList
      :item-array="modelValue"
      :canEdit="true"
      @update="$emit('update')"
    />
    <div v-if="!showAdd">
      <i class="bi bi-plus-circle text-primary" @click="showAdd = true"></i>
    </div>
    <div v-else class="pt-0">
      <input type="text" class="arrayElementInput mt-1" v-model="newValue" />
      <div class="btn-group mt-0" role="group" aria-label="Basic example">
        <button
          class="btn btn-sm check-badge btn-success me-0"
          @click="addNewValue"
        >
          <i class="bi bi-check"></i>
        </button>
        <button
          class="btn btn-sm check-badge btn-danger"
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
    removeItem(index: number) {
      const result = this.modelValue;
      result.slice(index, 0);
      this.$emit("update", result);
    },
    cancelNewValue() {
      this.newValue = "";
      this.showAdd = false;
    },
  },
});
</script>
