<script setup lang="ts">
import { ref } from "vue";

import RemoteFile from "./RemoteFile.vue";
import { getFiles } from "@/api/api";

import { RemoteFileInfo } from "@/types/api";

const remoteFiles = ref<Array<RemoteFileInfo>>([]);
const selectedFileID = ref("");

async function fetchData() {
  remoteFiles.value = [];
  const res = await getFiles();
  remoteFiles.value = res;
  selectedFileID.value = res[0].id;
}

fetchData();
</script>

<template>
  <div class="row">
    <p v-if="!remoteFiles">Loading...</p>
    <div class="col" v-else>
      <select v-model="selectedFileID" class="form-select form-select-lg mb-3">
        <option v-for="file in remoteFiles" :key="file.id" :value="file.id">
          {{ file.name }}
        </option>
      </select>
      <div class="row" v-if="selectedFileID">
        <RemoteFile :fileId="selectedFileID" />
      </div>
    </div>
  </div>
</template>

<style scoped>
select {
  filter: brightness(95%);
}
</style>
