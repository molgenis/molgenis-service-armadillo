<template>
  <div class="row">
    <div
      class="col-12 fst-italic"
      v-if="projectFolders.length === 0 && !createNewFolder"
    >
      Create a folder to get started
    </div>
    <div class="col-6 p-0 m-0">
      <ListGroup
        ref="folderComponent"
        :listContent="getSortedFolders()"
        rowIcon="folder"
        rowIconAlt="folder2-open"
        :altIconCondition="showSelectedFolderIcon"
        :preselectedItem="selectedFolder"
        :selectionColor="selectedFile ? 'secondary' : 'primary'"
      ></ListGroup>
    </div>
    <div class="col-6 p-0 m-0">
      <ListGroup
        v-show="selectedFolder !== ''"
        ref="fileComponent"
        :listContent="getSortedFiles()"
        rowIcon="table"
        rowIconAlt="file-earmark"
        :altIconCondition="isNonTableType"
        selectionColor="primary"
      ></ListGroup>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, onMounted, PropType, Ref, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { StringArray } from "@/types/types";
import { sortAlphabetically, isNonTableType } from "@/helpers/utils";
import ListGroup from "./ListGroup.vue";

export default defineComponent({
  name: "FileExplorer",
  props: {
    projectContent: {
      required: true,
      type: Object as PropType<Record<string, string[]>>,
    },
    addNewFolder: {
      required: true,
      type: Function,
    },
  },
  components: {
    ListGroup,
  },
  setup(_props, { emit }) {
    const folderComponent: Ref = ref({});
    const fileComponent: Ref = ref({});
    const selectedFolder = ref("");
    const selectedFile = ref("");
    const route = useRoute();

    watch(
      () => route.params.folderId,
      (newVal) => {
        selectedFolder.value = newVal as string;
      }
    );
    onMounted(() => {
      if (route.params.folderId) {
        selectedFolder.value = route.params.folderId as string;
      }
      watch(
        () => folderComponent.value?.selectedItem,
        (newVal) => {
          if (newVal != undefined) {
            emit("selectFolder", newVal);
            selectedFolder.value = newVal;
          }
        }
      );
      watch(
        () => fileComponent.value?.selectedItem,
        (newVal) => {
          if (newVal) {
            selectedFile.value = newVal;
          } else {
            selectedFile.value = "";
          }
          emit("selectFile", selectedFile.value);
        }
      );
    });
    return {
      folderComponent,
      fileComponent,
      selectedFolder,
      selectedFile,
    };
  },
  data() {
    return {
      newFolder: "",
      selectedFolder: this.preSelectedFolder,
      selectedFile: this.preSelectedFile,
      createNewFolder: false,
    };
  },
  computed: {
    projectFolders(): StringArray {
      return Object.keys(this.projectContent) as StringArray;
    },
  },
  methods: {
    isNonTableType,
    setCreateNewFolder() {
      this.createNewFolder = true;
    },
    cancelNewFolder() {
      this.createNewFolder = false;
      this.newFolder = "";
    },
    showSelectedFolderIcon(item: string) {
      return item === this.selectedFolder;
    },
    getSortedFolders(): StringArray {
      return sortAlphabetically(this.projectFolders) as StringArray;
    },
    getSortedFiles() {
      return this.projectContent[this.selectedFolder]
        ? (sortAlphabetically(
            this.projectContent[this.selectedFolder]
          ) as StringArray)
        : [];
    },
  },
});
</script>
