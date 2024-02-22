<template>
  <div class="row">
    <div class="col">
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

<script lang="ts">
import RemoteFile from "./RemoteFile.vue";
import { getFiles } from "@/api/api";
import { RemoteFileInfo } from "@/types/api";

import { defineComponent, onMounted, Ref, ref } from "vue";

export default defineComponent({
  name: "RemoteFiles",
  components: {
    RemoteFile,
  },
  emits: ["loading-done"],
  setup(_, { emit }) {
    const remoteFiles: Ref<RemoteFileInfo[]> = ref([]);
    const selectedFileID: Ref<string> = ref("");

    onMounted(() => {
      fetchData();
    });
    async function fetchData() {
      remoteFiles.value = [];
      await getFiles().then((data) => {
        remoteFiles.value = data;
        emit("loading-done");
        selectedFileID.value = data[0].id;
      });
    }
    return {
      remoteFiles,
      selectedFileID,
      fetchData,
    };
  },
  data() {
    return {};
  },
  computed: {},
  watch: {},
  methods: {},
});
</script>

<style scoped>
select {
  filter: brightness(95%);
}
</style>
