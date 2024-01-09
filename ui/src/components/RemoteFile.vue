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

const input = ref("");
const file = ref(null);
const lines = ref([]);
const filterValue = ref("");
const numberOfLines = ref(-1);
const currentFocus = ref(0);

watch(
  () => props.fileId,
  (_val, _oldVal) => {
    // console.log(`FileID changed from ${oldVal} to ${val}`);
    fetchFile();
  }
);

watch(filterValue, (_newVal, _oldVal) => filteredLines());

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

// Find line numbers with matching string values
let matchedLines: number[] = [];
const filteredLines = () => {
  currentFocus.value = -1;
  matchedLines = lines.value
    .map((v: string, i: number) =>
      v.toLowerCase().includes(filterValue.value.toLowerCase()) ? i : -1
    )
    .filter((v) => v > -1);
  if (matchedLines.length == lines.value.length) {
    matchedLines = [];
  }
  numberOfLines.value = matchedLines.length;
  setTimeout(setFocusOnLine, 20, 0);
};

// Helper to highlight lines
const isMatchedLine = (lineNo: number) => matchedLines.includes(lineNo);

const setFocusOnLine = (item: number) => {
  const elements = document.getElementsByClassName("text-danger");
  if (elements.length > 0) {
    if (item < 0) item = 0;
    if (item >= elements.length) item = elements.length - 1;
    if (item >= matchedLines.length) item = matchedLines.length - 1;
    currentFocus.value = item;
    elements[item].scrollIntoView();
  }
};

function navigate(direction: string) {
  if (direction === "first") {
    currentFocus.value = 0;
  } else if (direction === "prev") {
    currentFocus.value -= 1;
  } else if (direction === "next") {
    currentFocus.value += 1;
  } else if (direction === "last") {
    currentFocus.value += matchedLines.length - 1;
  }
  setTimeout(setFocusOnLine, 20, currentFocus.value);
}
</script>

<template>
  <div v-if="file">
    <header>
      <button class="btn btn-info" type="button" @click="fetchFile">
        Reload @ server time {{ file.fetched }}
      </button>
      &nbsp;
      <button class="btn btn-primary" type="button" @click="downloadFile">
        Download '{{ file.name }}' file
      </button>
    </header>
    <main>
      <input
        type="text"
        v-model="filterValue"
        placeholder="Search..."
        v-on:change="filteredLines"
      />
      <button
        type="button"
        class="btn btn-primary btn-sm"
        @click="navigate('first')"
      >
        |&lt
      </button>
      <button
        type="button"
        class="btn btn-primary btn-sm"
        @click="navigate('prev')"
      >
        &lt;
      </button>
      <span>{{ currentFocus }} / {{ numberOfLines }}</span>
      <button
        type="button"
        class="btn btn-primary btn-sm"
        @click="navigate('next')"
      >
        &gt;
      </button>
      <button
        type="button"
        class="btn btn-primary btn-sm"
        @click="navigate('last')"
      >
        &gt|
      </button>
      <div class="content">
        <div class="line" v-for="(line, index) in lines" :key="index">
          <span
            class="line-content"
            :class="{ 'text-danger': isMatchedLine(index) }"
          >
            {{ line }}
          </span>
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
