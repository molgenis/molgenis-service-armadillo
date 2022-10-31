<template>
  <div>
    <div v-for="(value, key) in modelValue">
      <Badge
        >{{ key }} = {{ value }}
        <button
          class="cancel-badge text-light bg-secondary"
          @click="remove(key)"
        >
          <i class="bi bi-x"></i>
        </button>
      </Badge>
    </div>
    <div v-if="showAdd == false">
      <i class="text-primary bi bi-plus-circle" @click="showAdd = true"></i>
    </div>
    <span v-else>
      <input
        type="text"
        id="key"
        class="form-control-sm"
        placeholder="option"
        v-model="newKey"
      />
      <input
        type="text"
        class="form-control-sm"
        v-model="newValue"
        placeholder="value"
      />
      <button class="check-badge text-light bg-secondary" @click="addNewValue">
        <i class="bi bi-check-lg"></i>
      </button>
      <button
        class="check-badge text-light bg-secondary"
        @click="cancelNewValue"
      >
        <i class="bi bi-x-lg"></i>
      </button>
    </span>
  </div>
</template>

<script lang="ts">
import Badge from "@/components/Badge.vue";

export default {
  name: "KeyValueInput",
  components: { Badge },
  props: {
    modelValue: { type: Object, required: true },
  },
  data() {
    return {
      showAdd: false,
      newKey: "",
      newValue: "",
    };
  },
  emits: ["update"],
  methods: {
    addNewValue() {
      const result = this.modelValue;
      result[this.newKey] = this.newValue;
      this.newValue = "";
      this.newKey = "";
      this.showAdd = false;
      this.$emit("update", result);
    },
    remove(key: string) {
      const result = this.modelValue;
      delete result[key];
      this.$emit("update", result);
    },
    cancelNewValue() {
      this.newValue = "";
      this.newKey = "";
      this.showAdd = false;
    },
  },
};
</script>

<style scoped>
button.cancel-badge {
  border: none;
  padding: 0;
  margin-right: -0.2em;
}
</style>
