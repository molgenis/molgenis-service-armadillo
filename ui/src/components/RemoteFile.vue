<script setup lang="ts">
import { ref, watch } from "vue";

import { getFileDetail } from "@/api/api";

import { RemoteFileDetail } from "@/types/api";

import SearchBar from "@/components/SearchBar.vue";

import { matchedLineIndices } from "@/helpers/insight";
import LoadingSpinner from "./LoadingSpinner.vue";

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

fetchFile();

// Find line numbers with matching string values
let matchedLines: number[] = [];
function filteredLines() {
  // find filter value in lines
  const searchFor = filterValue.value.toLowerCase();
  matchedLines = matchedLineIndices(lines.value, searchFor);

  numberOfLines.value = matchedLines.length || -1;
  // FIXME: is this bad?
  setTimeout(setFocusOnLine, 20, 0);
}

// Helper to highlight lines
const isMatchedLine = (lineNo: number) =>
  !(numberOfLines.value === -1) && matchedLines.includes(lineNo);

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
  let curValue = currentFocus.value;
  if (direction === "first") {
    curValue = 0;
  } else if (direction === "prev") {
    curValue -= 1;
    if (curValue < -1) {
      curValue = 0;
    }
  } else if (direction === "next") {
    curValue += 1;
  } else if (direction === "last") {
    curValue += matchedLines.length - 1;
  }
  currentFocus.value = curValue;
  setTimeout(setFocusOnLine, 20, currentFocus.value);
}
</script>

<template>
  <div v-if="file">
    <div class="row">
      <div class="col-sm-6">
        <button class="btn btn-info me-1" type="button" @click="fetchFile">
          <i class="bi bi-arrow-clockwise"></i>Reload
        </button>
        <a
          class="btn btn-primary"
          :href="'/insight/files/' + file.id + '/download'"
          ><i class="bi bi-box-arrow-down"></i>Download</a
        >
      </div>
    </div>
    <div class="row">
      <div class="text-secondary fst-italic">
        Last reload @ server time {{ file.fetched }}
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
            :disabled="numberOfLines < 1"
            class="btn btn-primary me-1"
            @click="navigate('first')"
          >
            <i class="bi bi-skip-backward-fill"></i>
          </button>
          <button
            type="button"
            :disabled="numberOfLines < 1"
            class="btn btn-primary me-1"
            @click="navigate('prev')"
          >
            <i class="bi bi-skip-start-fill"></i>
          </button>
          <button
            type="button"
            :disabled="numberOfLines < 1"
            class="btn btn-primary me-1"
            @click="navigate('next')"
          >
            <i class="bi bi-skip-end-fill"></i>
          </button>
          <button
            type="button"
            :disabled="numberOfLines < 1"
            class="btn btn-primary"
            @click="navigate('last')"
          >
            <i class="bi bi-skip-forward-fill"></i>
          </button>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col">
        <div v-if="numberOfLines > -1" class="text-secondary fst-italic">
          <span>{{ currentFocus + 1 }} / {{ numberOfLines }}</span>
        </div>
        <div v-else class="text-secondary fst-italic">
          <span>No search results</span>
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
  <div v-else>
    <LoadingSpinner></LoadingSpinner>
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
