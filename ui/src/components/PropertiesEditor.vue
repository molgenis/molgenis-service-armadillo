<template>
  <div>
    <h3>Properties</h3>
    <!-- <pre> {{ items }}</pre> -->
    <table class="table">
      <thead>
        <tr>
          <td>Step</td>
          <td>Key</td>
          <td>Description</td>
          <td>Value</td>
          <td>Actions</td>
        </tr>
      </thead>
      <tbody v-for="(item, index) in items" :key="index">
        <!-- <pre> {{  item }}</pre> -->
        <tr v-if="editIndex !== index">
          <td>{{ item.step }}</td>
          <td>{{ item.key }}</td>
          <td>{{ item.description }}</td>
          <td>{{ item.value }}</td>
          <td>
            <button @click="editItem(index)">Edit</button>
            <button @click="deleteItem(index)">Delete</button>
          </td>
        </tr>
        <tr v-else>
          <td>{{ item.step }}</td>
          <td v-if="item.key.length > 0">{{ item.key }}</td>
          <td v-else>
            <input v-model="tempItem.key" />
          </td>

          <td>{{ item.description }}</td>
          <td>
            <input v-model="tempItem.value" />
          </td>
          <td>
            <button @click="cancelEdit">Cancel</button>
            <button @click="saveItem(index)">Update</button>
          </td>
        </tr>
      </tbody>
    </table>
    <button @click="addRow">Add row</button>
    <button @click="saveList">Save</button>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { getProperties, updateProperties } from "@/api/api";

export default defineComponent({
  name: "PropertiesEditor",
  data() {
    return {
      items: [
        // { key: "a.b", title: "A of B", type: "String", value: "Value" },
        // { key: "a.c", title: "A of C", type: "integer", value: 12 },
      ],
      tempItem: null,
      editIndex: -1,
    };
  },
  methods: {
    editItem(index: number) {
      this.tempItem = { ...this.items[index] };
      this.editIndex = index;
    },
    saveItem(index: number) {
      this.items[index] = this.tempItem;
      this.editIndex = -1;
    },
    cancelEdit() {
      this.editIndex = -1;
    },
    deleteItem(index: number) {
      this.items.splice(index, 1);
    },
    addRow() {
      this.items.push({ key: "", title: "na", type: "Unknown", value: "" });
      this.editIndex = this.items.length - 1;
      this.tempItem = { ...this.items[this.editIndex] };
    },
    saveList() {
      updateProperties(this.items);
    },
  },
  mounted() {
    getProperties().then((response) => {
      this.items = response;
    });
  },
});
</script>
