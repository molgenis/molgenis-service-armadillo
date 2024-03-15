<template>
  <div class="input-group input-group-sm m-1 p-1 pe-2">
    <input
      type="text"
      class="form-control folder-name"
      placeholder="Folder name"
      v-model="folderName"
    />
    <button
      class="btn btn-sm btn-success"
      type="button"
      @click="addNewFolder(folderName)"
    >
      <i class="bi bi-check-lg"></i>
    </button>
    <button
      class="btn btn-sm btn-danger"
      type="button"
      @click="cancelNewFolder()"
    >
      <i class="bi bi-x-lg"></i>
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "FolderInput",
  data() {
    return {
      folderName: "",
    };
  },
  props: {
    addNewFolder: {
      default: (folderName) => {},
      type: Function,
    },
    cancelNewFolder: {
      default: () => {},
      type: Function,
    },
  },
});
</script>
