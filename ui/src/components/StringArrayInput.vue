<template>
  <div>
    <BadgeList :item-array="modelValue" :canEdit="true" @update="$emit(event)"/>
    <div v-if="showAdd == false"><i class="bi bi-plus-circle text-primary" @click="showAdd = true"></i></div>
    <span v-else>
      <input type="text"
             class="arrayElementInput"
             v-model="newValue"/>
      <button class="check-badge text-light bg-secondary"
              @click="addNewValue"
      >
        <i class="bi bi-check-lg"></i>
      </button>
      <button class="check-badge text-light bg-secondary"
              @click="cancelNewValue"
      >
        <i class="bi bi-x-lg"></i>
      </button>
    </span>
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

<script type="ts">

import BadgeList from "@/components/BadgeList.vue";

export default {
  name: "StringArrayInput",
  components: {BadgeList},
  props: {
    modelValue: Array
  },
  emits: ['update'],
  data() {
    return {
      showAdd: false,
      newValue: ""
    }
  },
  methods: {
    addNewValue() {
      const result = this.modelValue;
      result.push(this.newValue);
      this.newValue = "";
      this.showAdd = false;
      this.$emit('update', result);
    },
    removeItem(index) {
      const result = this.modelValue;
      result.slice(index, 0);
      this.$emit('update', result);
    },
    cancelNewValue() {
      this.newValue = "";
      this.showAdd = false;
    }
  }
}
</script>