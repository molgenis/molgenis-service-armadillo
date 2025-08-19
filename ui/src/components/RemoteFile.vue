<template>
  <div v-if="file">
    <div class="row">
      <div class="col-3">
        <p class="m-0 fst-italic">
          Log file size: {{ fileInfo?.convertedSize }}
        </p>
      </div>
    </div>
    <div class="row mb-1 mt-0">
      <div class="row align-items-end">
        <ShowSwitch class="col-3" @switched="switchShowAll($event)" />
        <div class="col-2 p-0 offset-1">
          <SearchBar id="searchbox" v-model="filterValue" />
        </div>
        <div class="col-2">
          <div class="row text-center">
            <div v-if="numberOfLines > -1" class="text-secondary fst-italic">
              <span>{{ currentFocus + 1 }} / {{ numberOfLines }}</span>
            </div>
            <div
              v-else-if="filterValue != ''"
              class="text-secondary fst-italic"
            >
              <span>No search results</span>
            </div>
            <div v-else>
              <div class="text-white">-</div>
            </div>
          </div>
          <div class="row">
            <div class="col ps-1">
              <div
                class="btn-group"
                role="group"
                aria-label="navigation"
                v-if="true || (filterValue && matchedLines.length > 0)"
              >
                <button
                  type="button"
                  :disabled="numberOfLines < 1"
                  class="btn btn-primary"
                  @click="navigate('first')"
                >
                  <i class="bi bi-skip-backward-fill"></i>
                </button>
                <button
                  type="button"
                  :disabled="numberOfLines < 1"
                  class="btn btn-primary"
                  @click="navigate('prev')"
                >
                  <i class="bi bi-skip-start-fill"></i>
                </button>
                <button
                  type="button"
                  :disabled="numberOfLines < 1"
                  class="btn btn-primary"
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
        </div>
        <div class="col-2 offset-1">
          <div class="row">
            <div class="col">Sort page on:</div>
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
            class="form-select form-select-sm mt-1"
          >
            <option value="timeDesc">Time (new -> old)</option>
            <option value="timeAsc">Time (old -> new)</option>
          </select>
        </div>
        <div class="col-1 p-0">
          <div class="row">
            <div class="col fst-italic text-end">
              Page <span>{{ file.page_num + 1 }}</span>
            </div>
          </div>
          <div class="row">
            <div class="btn-group btn-group-s ps-1">
              <button
                class="btn btn-primary btn-sm"
                type="button"
                @click="resetPageNum"
                :disabled="file.page_num === 0"
              >
                <i class="bi bi-chevron-double-left"></i>
              </button>
              <button
                class="btn btn-primary btn-sm"
                type="button"
                @click="decreasePageNum"
                :disabled="file.page_num === 0"
              >
                <i class="bi bi-chevron-compact-left"></i>
              </button>
              <button
                class="btn btn-primary btn-sm"
                type="button"
                @click="loadMore"
                :disabled="file.page_num === maxNumberOfPages - 1"
              >
                <i class="bi bi-chevron-compact-right"></i>
              </button>
              <button
                class="btn btn-primary btn-sm"
                type="button"
                @click="goToLastPage"
                :disabled="file.page_num === maxNumberOfPages - 1"
              >
                <i class="bi bi-chevron-double-right"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col">
        <div class="content">
          <div
            v-if="
              showOnlyErrors &&
              lines.filter((line) => line.includes('_FAILURE')).length == 0
            "
            class="fst-italic mb-3"
          >
            No lines found containing errors on page {{ file.page_num + 1 }}
          </div>
          <span class="m-0" v-for="(line, index) in lines" :key="index" v-else>
            <LogLine
              v-if="
                file.id === 'LOG_FILE' &&
                (!showOnlyErrors || line.includes('_FAILURE'))
              "
              class="line-content"
              :logLine="line"
              :class="{ 'text-primary': isMatchedLine(index) }"
            >
              {{ line }}
            </LogLine>
            <AuditLogLine
              v-else-if="!showOnlyErrors || line.includes('_FAILURE')"
              :logLine="line"
              :class="{ 'text-primary': isMatchedLine(index) }"
            />
          </span>
          <button
            class="btn btn-primary"
            @click="loadMore()"
            v-if="file.page_num != maxNumberOfPages - 1"
          >
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
import ShowSwitch from "./ShowSwitch.vue";

export default {
  name: "RemoteFile",
  components: {
    SearchBar,
    LoadingSpinner,
    AuditLogLine,
    LogLine,
    ShowSwitch,
  },
  emits: ["resetReload"],
  props: {
    fileId: {
      type: String,
      required: true,
    },
    reloadFile: {
      type: Boolean,
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
    matchedLines: number[];
    filterValue: string;
    numberOfLines: number;
    sortType: string;
    showOnlyErrors: boolean;
  } {
    return {
      matchedLines: [],
      filterValue: "",
      numberOfLines: -1,
      sortType: "timeDesc",
      showOnlyErrors: false,
    };
  },
  computed: {
    pages() {
      return [...Array(this.maxNumberOfPages).keys()];
    },
    fileInfo() {
      if (this.file && this.file.fetched) {
        const splittedInfo = this.file.fetched.split(": ");
        const bytes = splittedInfo ? splittedInfo[1] : "";
        return {
          reloadTime: this.file.fetched.replace(": " + bytes, ""),
          size: parseInt(bytes),
          convertedSize: convertBytes(parseInt(bytes)),
        };
      }
    },
    maxNumberOfPages() {
      return this.fileInfo ? Math.ceil(this.fileInfo.size / 10000) : 0;
    },
    isMatchedLine() {
      return (lineNo: number) =>
        !(this.numberOfLines === -1) && this.matchedLines.includes(lineNo);
    },
  },
  watch: {
    reloadFile() {
      if (this.reloadFile) {
        this.fetchFile();
        this.$emit("resetReload");
      }
    },
    fileId: {
      deep: true,
      handler() {
        this.resetStates();
        this.changeSelected();
      },
    },
    filterValue() {
      this.filterLines();
    },
  },
  methods: {
    switchShowAll(eventValue: boolean) {
      this.showOnlyErrors = eventValue;
    },
    resetPageNum() {
      if (this.file) {
        this.file.page_num = 0;
        this.changeSelected();
      }
    },
    goToLastPage() {
      if (this.file) {
        this.file.page_num = this.maxNumberOfPages - 1;
        this.changeSelected();
      }
    },
    decreasePageNum() {
      if (this.file && this.file.page_num != 0) {
        this.file.page_num -= 1;
        this.changeSelected();
      }
    },
    loadMore() {
      if (this.file && this.file.page_num != this.maxNumberOfPages - 1) {
        this.file.page_num += 1;
        this.changeSelected();
      }
    },
    changeSelected() {
      this.fetchFile();
      this.resetData();
      this.resetStates();
    },
    filterLines() {
      // find filter value in lines
      const searchFor = this.filterValue.toLowerCase();
      this.matchedLines = matchedLineIndices(this.lines, searchFor);

      this.numberOfLines = this.matchedLines.length || -1;
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
      const elements = document.getElementsByClassName("text-primary");
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
