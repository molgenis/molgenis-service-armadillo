<script setup lang="ts">
import { ref, watch } from "vue";

import { getFileDetail } from "@/api/api";
import { getFileDownload } from "@/api/api";

import { RemoteFileDetail } from "@/types/api";

const props = defineProps({
  fileId: {
    type: String,
    required: true,
  },
});

watch(
  () => props.fileId,
  (_val, _oldVal) => {
    // console.log(`FileID changed from ${oldVal} to ${val}`);
    fetchFile();
  }
);

const file = ref(null);
const lines = ref(null);

async function fetchFile() {
  try {
    file.value = null;

    const res = await getFileDetail(props.fileId);
    lines.value = res.content.split("\n");
    file.value = res;
  } catch (error) {
    console.error(error);
  }
}

function downloadFile() {
  console.log("Downloading: " + props.fileId);
  // FIXME: filedetails need name, extension
  const name = file.value.name;
  const ext = "log";
  getFileDownload(props.fileId)
    .then((r) => {
      return r;
    })
    .then((response) => response.blob())
    .then((blob) => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `${name}.${ext}`);
      document.body.appendChild(link);
      link.click();
      // Release the reference to the object URL after the download starts
      setTimeout(() => URL.revokeObjectURL(url), 100);
      link.remove();
    })
    .catch(console.error);
}

fetchFile();
</script>

<template>
  <div v-if="file">
    <header>
      <span class="alert alert-warning" role="alert">
        Fetched @ server time {{ file.fetched }}
      </span>
      &nbsp;
      <button class="btn btn-primary" type="button" @click="downloadFile">
        Download '{{ file.name }}' file
      </button>
    </header>
    <main>
      <div class="content">
        <div class="line" v-for="(line, index) in lines" :key="index">
          <span class="line-content">{{ line }}</span>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
* {
  padding: 2px;
}

.content {
  overflow-y: scroll;
  max-height: 65vh;
  border: none;
  padding-top: 0.5em;
}
.line-content {
  white-space: pre-wrap;
}

.line {
  background-color: white;
}
.line:nth-child(odd) {
  filter: brightness(95%);
}
</style>
