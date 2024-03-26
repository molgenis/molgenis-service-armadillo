<template>
  <div>
    <h3>Properties</h3>
    <table class="table">
      <thead>
        <tr>
          <td>Key</td>
          <td>Title</td>
          <td>Value</td>
          <td>Actions</td>
        </tr>
      </thead>
      <tbody v-for="(item, index) in items" :key="index">
        <tr v-if="editIndex !== index">
          <td>{{ item.key }}</td>
          <td>{{ item.title }}</td>
          <td>{{ item.value }}</td>
          <td>
            <button @click="editItem(index)">Edit</button>
          </td>
        </tr>
        <tr v-else>
          <td>{{ item.key }}</td>
          <td>{{ item.title }}</td>
          <td>
            <input v-model="tempItem.value" />
          </td>
          <td>
            <button @click="cancelEdit">Cancel</button>
            <button @click="saveItem(index)">Save</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "PropertiesEditor",
  data() {
    return {
      items: [
        { key: "a.c:", title: "A of B", type: "String", value: "Value" },
        { key: "a.c", title: "A of C", type: "integer", value: 12 },
      ],
      tempItem: null,
      editIndex: -1,
    };
  },
  methods: {
    editItem(index) {
      this.tempItem = { ...this.items[index] };
      this.editIndex = index;
    },
    saveItem(index) {
      this.items[index] = this.tempItem;
      this.editIndex = -1;
    },
    cancelEdit() {
      this.editIndex = -1;
    },
  },
});
</script>
