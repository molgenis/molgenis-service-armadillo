<script setup lang="ts">
import { ref, watch } from "vue";

import { getFileDetail } from "@/api/api";
import { getFileDownload } from "@/api/api";

import { RemoteFileDetail } from "@/types/api";

import SearchBar from "@/components/SearchBar.vue";

import { matchedLineIndices } from "@/helpers/insight";

const props = defineProps({
  fileId: {
    type: String,
    required: true,
  },
});

const file = ref<RemoteFileDetail>();
const lines = ref<Array<string>>([]);
const filterValue = ref("");
const numberOfLines = ref(-1);
const currentFocus = ref(0);

function resetStates() {
  lines.value = [];
  filterValue.value = "";
  numberOfLines.value = -1;
  currentFocus.value = 0;
}

// Watch for setting the component value
watch(
  () => props.fileId,
  (_val, _oldVal) => fetchFile()
);

// Watch for changes while searching
watch(filterValue, (_newVal, _oldVal) => filteredLines());

async function fetchFile() {
  resetStates();
  try {
    const res = await getFileDetail(props.fileId);
    let list = res.content.trim().split("\n");

    // we assume JSON lines if starts with {
    if (list.length && list[0].startsWith("{")) {
      // Just return pretty print?
      // return JSON.stringify(record.data, null, 2);

      // auditor fields are know
      const audit = ["timestamp", "principal", "type"];
      const mapper = (k: string, v: string | number) => `${k}: ${v}\n`;

      list = list.map((line) => {
        let html = "";
        const record = JSON.parse(line);
        let isAudit = false;
        audit.forEach((field) => {
          if (record[field]) {
            isAudit = true;
            html += mapper(field, record[field]);
          }
        });
        if (isAudit) {
          return (
            html +
            "\n" +
            mapper("data", "\n" + JSON.stringify(record.data, null, 2))
          );
        } else {
          return JSON.stringify(record.data, null, 2);
        }
      });
    }
    lines.value = list;
    file.value = res;
  } catch (error) {
    console.error(error);
  }
}

function downloadFile() {
  // FIXME: filedetails need name, extension
  const name = file.value?.name;
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
function filteredLines() {
  // find filter value in lines
  const searchFor = filterValue.value.toLowerCase();
  matchedLines = matchedLineIndices(lines.value, searchFor);

  numberOfLines.value = matchedLines.length;
  // FIXME: is this bad?
  setTimeout(setFocusOnLine, 20, 0);
}

// Helper to highlight lines
const isMatchedLine = (lineNo: number) => matchedLines.includes(lineNo);

/**
 * Scroll to one of the search results
 *
 * @param item index of element to set focus to.
 */
function setFocusOnLine(item: number) {
  const elements = document.getElementsByClassName("text-danger");
  if (elements.length > 0) {
    if (item < 0) item = 0;
    if (item >= elements.length) item = elements.length - 1;
    if (item >= matchedLines.length) item = matchedLines.length - 1;
    currentFocus.value = item;
    elements[item].scrollIntoView();
  }
}

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
    <div class="row">
      <div class="col-sm-6">
        <button class="btn btn-info" type="button" @click="fetchFile">
          Reload @ server time {{ file.fetched }}
        </button>
        <button class="btn btn-primary" type="button" @click="downloadFile">
          Download '{{ file.name }}' file
        </button>
      </div>
    </div>
    <div class="row">
      <div class="col-sm-3">
        <SearchBar id="searchbox" v-model="filterValue" />
      </div>

      <div class="col">
        <div
          class="btn-group"
          role="group"
          aria-label="navigation"
          v-if="true || (filterValue && matchedLines.length > 0)"
        >
          <button
            type="button"
            class="btn btn-primary"
            @click="navigate('first')"
          >
            |&lt
          </button>
          <button
            type="button"
            class="btn btn-primary"
            @click="navigate('prev')"
          >
            &lt;
          </button>
          <span>{{ currentFocus + 1 }} / {{ numberOfLines }}</span>
          <button
            type="button"
            class="btn btn-primary"
            @click="navigate('next')"
          >
            &gt;
          </button>
          <button
            type="button"
            class="btn btn-primary"
            @click="navigate('last')"
          >
            &gt|
          </button>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col">
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
      </div>
    </div>
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
