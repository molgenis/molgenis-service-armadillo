<template>
  <div v-if="file">
    <p class="m-0 mb-2 fst-italic">
      Log file size: {{ fileInfo.convertedSize }}
    </p>
    <div class="row">
      <div class="col-sm-3 buttons">
        <button class="btn btn-info me-1" type="button">
          <i class="bi bi-arrow-clockwise"></i>Reload
        </button>
        <a
          class="btn btn-primary"
          :href="'/insight/files/' + file.id + '/download'"
        >
          <i class="bi bi-box-arrow-down"></i> Download
        </a>
      </div>
    </div>
    <div class="row stats">
      <div
        class="btn-group me-2"
        role="group"
        aria-label="First group"
        v-if="pages.length < 10"
      >
        <button
          type="button"
          class="btn btn-primary"
          v-for="index in pages"
          :key="index"
        >
          {{ index + 1 }}
        </button>
      </div>

      <div class="text-secondary fst-italic">
        <p class="m-0">Last reload @ server time {{ fileInfo.reloadTime }}</p>
      </div>
    </div>
    <div class="row filtering">
      <div class="col-sm-3">
        <SearchBar id="searchbox" v-model="filterValue" />
        <div v-if="numberOfLines > -1" class="text-secondary fst-italic">
          <span>{{ currentFocus + 1 }} / {{ numberOfLines }}</span>
        </div>
        <div v-else class="text-secondary fst-italic">
          <span>No search results</span>
        </div>
      </div>
      <div class="col search-navigation">
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
      <div class="col-2">
        <div class="row">
          <div class="col">
            Page
            <input
              type="number"
              v-model="file.page_num"
              @change="changeSelected"
              min="0"
            />
          </div>
        </div>
      </div>
      <div class="col-2">
        <div class="row">
          <div class="col">Sort on:</div>
          <div class="col-3">
            <i
              v-if="sortType == 'timeDesc'"
              class="bi bi-sort-numeric-up-alt"
            ></i>
            <i
              v-else-if="sortType == 'timeAsc'"
              class="bi bi-sort-numeric-down-alt"
            ></i>
          </div>
        </div>
        <select
          v-model="sortType"
          @change="changeSelected"
          class="form-select mb-3 form-select-sm"
        >
          <option value="timeDesc">Time (new -> old)</option>
          <option value="timeAsc">Time (old -> new)</option>
        </select>
      </div>
    </div>

    <div class="row">
      <div class="col">
        <div class="content">
          <div class="line" v-for="(line, index) in lines" :key="index">
            <LogLine
              v-if="file.id === 'LOG_FILE'"
              class="line-content"
              :logLine="line"
              :class="{ 'text-danger': isMatchedLine(index) }"
            >
              {{ line }}
            </LogLine>
            <AuditLogLine
              v-else
              :logLine="line"
              :class="{ 'text-danger': isMatchedLine(index) }"
            />
          </div>
          <button class="btn btn-primary" @click="loadMore()">
            <i class="bi bi-arrow-clockwise"></i> Load more
          </button>
        </div>
      </div>
    </div>
  </div>
  <div v-else>
    <LoadingSpinner></LoadingSpinner>
  </div>
</template>

<script lang="ts">
import { getFileDetail } from "@/api/api";
import { onMounted, ref } from "vue";
import LoadingSpinner from "./LoadingSpinner.vue";
import SearchBar from "./SearchBar.vue";
import { RemoteFileDetail } from "@/types/api";
import { auditJsonLinesToLines, matchedLineIndices } from "@/helpers/insight";
import { convertBytes } from "@/helpers/utils";
import AuditLogLine from "./AuditLogLine.vue";
import LogLine from "./LogLine.vue";

export default {
  name: "RemoteFile",
  components: {
    SearchBar,
    LoadingSpinner,
    AuditLogLine,
    LogLine,
  },
  props: {
    fileId: {
      type: String,
      required: true,
    },
  },
  setup(props) {
    const file = ref<RemoteFileDetail | null>();
    const lines = ref<Array<string>>([]);
    const fromBeginOrEnd = ref<string>("end");
    const currentFocus = ref(0);

    function resetStates() {
      file.value = null;
      lines.value = [];
      currentFocus.value = 0;
    }

    async function fetchFile() {
      let page_num = 0;
      let direction = "end";

      if (file.value) {
        page_num = file.value.page_num;
      }
      if (fromBeginOrEnd.value) {
        direction = fromBeginOrEnd.value;
      }

      resetStates();
      try {
        const res = await getFileDetail(props.fileId, page_num, direction);

        let list = res.content.trim().split("\n");

        if (res.content_type === "application/x-ndjson") {
          // We assume it is an Audit file for now
          list = auditJsonLinesToLines(list);
        }

        lines.value = list;
        file.value = res;
      } catch (error) {
        console.error(error);
      }
    }
    onMounted(() => {
      fetchFile();
    });

    return {
      file,
      lines,
      fromBeginOrEnd,
      resetStates,
      currentFocus,
      fetchFile,
    };
  },
  data(): {
    currentFocus: number;
    matchedLines: number[];
    filterValue: string;
    numberOfLines: number;
    sortType: string;
  } {
    return {
      currentFocus: 0,
      matchedLines: [],
      filterValue: "",
      numberOfLines: -1,
      sortType: "timeDesc",
    };
  },
  computed: {
    pages() {
      return [...Array(this.maxNumberOfPages).keys()];
    },
    fileInfo() {
      const splittedInfo = this.file?.fetched.split(": ");
      const bytes = splittedInfo ? splittedInfo[1] : "";
      return {
        reloadTime: this.file?.fetched.replace(": " + bytes, ""),
        size: parseInt(bytes),
        convertedSize: convertBytes(parseInt(bytes)),
      };
    },
    maxNumberOfPages() {
      return Math.ceil(this.fileInfo.size / 10000);
    },
    isMatchedLine() {
      return (lineNo: number) =>
        !(this.numberOfLines === -1) && this.matchedLines.includes(lineNo);
    },
  },
  watch: {
    sortType() {
      if (this.sortType === "timeDesc") {
        this.fromBeginOrEnd = "start";
        this.changeSelected();
      } else if (this.sortType === "timeAsc") {
        this.fromBeginOrEnd = "end";
        this.changeSelected();
      } else if (this.sortType === "errors") {
        const failure = "_FAILURE";
        this.lines = this.lines.sort((a, b) => {
          // console.log(a, b)
          if (a.includes(failure) && b.includes(failure)) {
            console.log("both");
            return 0;
          } else if (a.includes(failure)) {
            console.log("a");
            return 1;
          } else {
            console.log("b");
            return -1;
          }
        });
      }
    },
    fileId: {
      deep: true,
      handler() {
        this.changeSelected();
      },
    },
    filterValue() {
      this.filteredLines();
    },
  },
  methods: {
    loadMore() {
      if (this.file && this.file.page_num != this.maxNumberOfPages) {
        this.file.page_num += 1;
        this.changeSelected();
      }
    },
    changeSelected() {
      this.fetchFile();
      this.resetData();
      this.resetStates();
    },
    filteredLines() {
      // find filter value in lines
      const searchFor = this.filterValue.toLowerCase();
      this.matchedLines = matchedLineIndices(this.lines, searchFor);

      this.numberOfLines = this.matchedLines.length || -1;
      // FIXME: is this bad?
      setTimeout(this.setFocusOnLine, 20, 0);
    },
    resetData() {
      this.numberOfLines = -1;
      this.filterValue = "";
      this.matchedLines = [];
    },
    navigate(direction: string) {
      let curValue = this.currentFocus;
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
        curValue += this.matchedLines.length - 1;
      }
      this.currentFocus = curValue;
      setTimeout(this.setFocusOnLine, 20, this.currentFocus);
    },
    setFocusOnLine(item: number) {
      const elements = document.getElementsByClassName("text-danger");
      if (elements.length > 0) {
        if (item < 0) item = 0;
        if (item >= elements.length) item = elements.length - 1;
        if (item >= this.matchedLines.length)
          item = this.matchedLines.length - 1;
        this.currentFocus = item;
        elements[item].scrollIntoView();
      }
    },
  },
};
</script>

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
</style>
