<template>
  <div class="row mt-1">
    <p v-if="!remoteFiles">Loading...</p>
    <div class="col" v-else>
      <div class="row">
        <div class="col-3">
          <select v-model="selectedFileId" class="form-select">
            <option v-for="file in remoteFiles" :key="file.id" :value="file.id">
              {{ file.name }}
            </option>
          </select>
        </div>
        <div class="col">
          <button
            class="btn btn-primary me-1"
            type="button"
            @click="reloadFile = true"
          >
            <i class="bi bi-arrow-clockwise"></i> Reload
          </button>
          <a
            class="btn btn-primary"
            :href="'/insight/files/' + selectedFileId + '/download'"
          >
            <i class="bi bi-box-arrow-down"></i> Download
          </a>
        </div>
      </div>
      <div class="row" v-if="selectedFileId">
        <RemoteFile
          :fileId="selectedFileId"
          :reloadFile="reloadFile"
          @resetReload="reloadFile = false"
        />
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { onMounted, ref, defineComponent } from "vue";
import RemoteFile from "@/components/RemoteFile.vue";
import { getFiles } from "@/api/api";
import { RemoteFileInfo } from "@/types/api";

export default defineComponent({
  name: "RemoteFiles",
  components: {
    RemoteFile,
  },
  setup() {
    const remoteFiles = ref<Array<RemoteFileInfo>>([]);
    const selectedFileId = ref("");

    async function fetchData() {
      remoteFiles.value = [];
      const res = await getFiles();
      remoteFiles.value = res;
      selectedFileId.value = res[0].id;
    }

    onMounted(() => {
      fetchData();
    });

    fetchData();
    return {
      selectedFileId,
      remoteFiles,
    };
  },
  data() {
    return {
      reloadFile: false,
      selectedOption: "",
    };
  },
});
</script>

<style scoped>
select {
  filter: brightness(95%);
}
</style>
