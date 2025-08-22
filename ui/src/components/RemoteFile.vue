<template>
  <div v-if="file">
    <div class="row fs-6">
      <div class="col-3">
        <p class="m-0 fst-italic">
          Log file size: {{ fileInfo?.convertedSize }}
        </p>
      </div>
      <div class="col-3 text-secondary">
        <p class="m-0 fst-italic">Last reload: {{ fileInfo?.reloadTime }}</p>
      </div>
    </div>
    <div class="row mb-1 mt-0">
      <div class="row align-items-end">
        <ShowSwitch class="col-3" @switched="switchShowAll($event)" />
        <div class="col-2 p-0 offset-1">
          <SearchBar id="searchbox" v-model="filterValue" />
        </div>
        <NavigationButtons
          class="col-2"
          :currentValue="
            numberOfLines != -1
              ? `${currentFocus + 1} / ${numberOfLines}`
              : filterValue
              ? 'No results found'
              : ''
          "
          textValueColour="text-secondary"
          :icons="{
            first: 'skip-backward-fill',
            prev: 'skip-start-fill',
            next: 'skip-end-fill',
            last: 'skip-forward-fill',
          }"
          :functions="{
            first: () => navigate('first'),
            prev: () => navigate('prev'),
            next: () => navigate('next'),
            last: () => navigate('last'),
          }"
          :disabled="{
            first: numberOfLines < 1 || currentFocus === 0,
            prev: numberOfLines < 1 || currentFocus === 0,
            next: numberOfLines < 1 || currentFocus === numberOfLines - 1,
            last: numberOfLines < 1 || currentFocus === numberOfLines - 1,
          }"
        />
        <PageSorter
          :sortType="sortType"
          class="col-2 offset-1"
          @selectChanged="changeSelected"
        />
        <NavigationButtons
          class="col-1 p-0"
          :isSmall="true"
          :currentValue="`Page ${file.page_num + 1}`"
          :icons="{
            first: 'chevron-double-left',
            prev: 'chevron-left',
            next: 'chevron-right',
            last: 'chevron-double-right',
          }"
          :functions="{
            first: resetPageNum,
            prev: decreasePageNum,
            next: loadMore,
            last: goToLastPage,
          }"
          :disabled="{
            first: file.page_num === 0,
            prev: file.page_num === 0,
            next: file.page_num === maxNumberOfPages - 1,
            last: file.page_num === maxNumberOfPages - 1,
          }"
        />
      </div>
    </div>
    <div class="row">
      <div class="col">
        <div
          v-if="showOnlyErrors && errorLines.length === 0"
          class="fst-italic mb-3"
        >
          No lines found containing errors on page {{ file.page_num + 1 }}
        </div>
        <span v-else class="m-0" v-for="(line, index) in lines" :key="index">
          <LogLine
            v-if="showLine(line)"
            :fileId="file.id"
            :isMatchedLine="isMatchedLine(index)"
            :logLine="line"
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
  <div v-else>
    <LoadingSpinner />
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
import LogLine from "./LogLine.vue";
import ShowSwitch from "./ShowSwitch.vue";
import NavigationButtons from "./NavigationButtons.vue";
import PageSorter from "./PageSorter.vue";

export default {
  name: "RemoteFile",
  components: {
    SearchBar,
    LoadingSpinner,
    LogLine,
    ShowSwitch,
    NavigationButtons,
    PageSorter,
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
    const errorMessage = ref("");

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
    errorLines() {
      return this.lines.filter((line) => this.isErrorLine(line));
    },
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
    isMatchedLine(index: number) {
      return (
        !(this.numberOfLines === -1) &&
        this.matchedLines.includes(index) &&
        (!this.showOnlyErrors || this.isErrorLine(this.lines[index]))
      );
    },
    showLine(line: string) {
      return !this.showOnlyErrors || this.isErrorLine(line);
    },
    isErrorLine(line: string) {
      return line.includes("_FAILURE");
    },
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
      this.matchedLines = matchedLineIndices(this.lines, searchFor).filter(
        (index) => {
          return this.errorLines.includes(this.lines[index]);
        }
      );

      this.numberOfLines = this.matchedLines.length || -1;
      setTimeout(this.setFocusOnLine, 20, 0);
    },
    resetData() {
      this.numberOfLines = -1;
      this.filterValue = "";
      this.matchedLines = [];
      this.showOnlyErrors = false;
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
        if (this.filterValue !== "" && elements[item]) {
          elements[item].scrollIntoView();
        }
      }
    },
  },
};
</script>
