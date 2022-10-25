<template>
  <div>
    <div class="row">
      <div class="col">
        <!-- Error messages will appear here -->
        <FeedbackMessage
          :successMessage="successMessage"
          :errorMessage="errorMessage"
        ></FeedbackMessage>
        <!-- Loading spinner -->
        <LoadingSpinner v-if="loading"></LoadingSpinner>
      </div>
    </div>
    <div class="row">
      <div class="col-12">
        <h1>
          <button type="button" class="btn btn-sm me-1 btn-primary bg-primary">
            <router-link to="/projects">
              <i class="bi bi-arrow-left text-light"></i>
            </router-link>
          </button>
          Project: {{ $route.params.projectId }}
        </h1>
        <ButtonGroup
          :buttonIcons="['folder-plus', 'file-earmark-plus', 'trash-fill']"
          :buttonColors="['primary', 'primary', 'danger']"
          :clickCallbacks="[function () {}, function () {}, function () {}]"
        ></ButtonGroup>
        <div class="row mt-1 border border-1">
          <div class="col-6">
            <div class="row">
              <div class="col-6 p-0 m-0">
                <ListGroup
                  ref="folderComponent"
                  :listContent="Object.keys(projectContent)"
                  rowIcon="folder"
                  rowIconAlt="folder2-open"
                  :altIconCondition="showSelectedFolderIcon"
                  selectionColor="secondary"
                ></ListGroup>
              </div>
              <div class="col-6 p-0 m-0">
                <ListGroup
                  v-show="selectedFolder != ''"
                  ref="fileComponent"
                  :listContent="
                    projectContent[selectedFolder]
                      ? projectContent[selectedFolder]
                      : []
                  "
                  rowIcon="table"
                  rowIconAlt="file-earmark"
                  :altIconCondition="showFileIcon"
                  selectionColor="primary"
                ></ListGroup>
              </div>
            </div>
            <div class="row mt-3">
              <div class="col-6">
                <!-- <FileUpload></FileUpload> -->
              </div>
              <div class="col-6">
                <FileUpload
                  v-show="selectedFolder != ''"
                  :project="projectId"
                  :object="selectedFolder"
                  @upload_success="onUploadSuccess"
                  @upload_error="showErrorMessage"
                ></FileUpload>
              </div>
            </div>
          </div>
          <div class="col-6" v-show="selectedFile">
            <div class="text-end fst-italic">
              Preview:
              {{ `${selectedFile.replace(".parquet", "")} (108x1500)` }}
            </div>
            <SimpleTable></SimpleTable>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import ButtonGroup from "@/components/ButtonGroup.vue";
import ListGroup from "@/components/ListGroup.vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import { getProject } from "@/api/api";
import { defineComponent, onMounted, Ref, ref, watch } from "vue";
import { Project } from "@/types/api";
import {
  StringArray,
  ObjectWithStringKeyAndStringArrayValue,
} from "@/types/types";
import { useRoute } from "vue-router";
import FileUpload from "@/components/FileUpload.vue";
import SimpleTable from "@/components/SimpleTable.vue";

export default defineComponent({
  name: "ProjectsExplorer",
  components: {
    ButtonGroup,
    FeedbackMessage,
    ListGroup,
    LoadingSpinner,
    FileUpload,
    SimpleTable,
  },
  setup() {
    const project: Ref<StringArray> = ref([]);
    const projectId: Ref<string> = ref("");
    const folderComponent: Ref = ref({});
    const fileComponent: Ref = ref({});
    const selectedFolder = ref("");
    const selectedFile = ref("");
    onMounted(() => {
      loadProject(undefined);
      watch(
        () => folderComponent.value.selectedItem,
        (newVal) => {
          selectedFolder.value = newVal;
        }
      );

      watch(
        () => fileComponent.value.selectedItem,
        (newVal) => {
          selectedFile.value = newVal;
        }
      );
    });
    const loadProject = async (idParam: string | undefined) => {
      const route = useRoute();
      if (idParam === undefined) {
        idParam = route.params.projectId as string;
      }
      project.value = await getProject(idParam);
      projectId.value = idParam;
    };
    return {
      project,
      projectId,
      loadProject,
      folderComponent,
      fileComponent,
      selectedFolder,
      selectedFile,
    };
  },
  data() {
    return {
      projectToEdit: "",
      projectToEditIndex: -1,
      errorMessage: "",
      loading: false,
      successMessage: "",
    };
  },
  computed: {
    projectContent(): ObjectWithStringKeyAndStringArrayValue {
      let content: ObjectWithStringKeyAndStringArrayValue = {};
      this.project.forEach((item) => {
        const splittedItem = item.split("/");
        if (splittedItem[0] == this.projectId) {
          splittedItem.splice(0, 1);
        }

        if (!splittedItem[0].startsWith(".")) {
          if (!splittedItem[1].startsWith(".")) {
            if (splittedItem[0] in content) {
              content[splittedItem[0]].push(splittedItem[1]);
            } else {
              content[splittedItem[0]] = [splittedItem[1]];
            }
          }
        }
      });
      return content;
    },
  },
  methods: {
    onUploadSuccess() {
      const currentFiles = this.projectContent[this.projectId];
      this.reloadProject();
      const refreshedFiles = this.projectContent[this.projectId];
      const newFile = currentFiles.filter((x) => refreshedFiles.includes(x));
      this.successMessage = `Successfully uploaded file [${newFile}] into project: [${this.projectId}]`;
    },
    showSelectedFolderIcon(item: string) {
      return item === this.selectedFolder;
    },
    showFileIcon(item: string) {
      return !item.endsWith(".parquet");
    },
    clearUserMessages() {
      this.successMessage = "";
      this.errorMessage = "";
    },
    clearProjectToEdit() {
      this.projectToEdit = "";
    },
    editProject(project: Project) {
      this.projectToEdit = project.name;
    },
    reloadProject() {
      this.loading = true;
      this.loadProject(this.projectId)
        .then(() => {
          this.loading = false;
        })
        .catch((error) => {
          this.errorMessage = `Could not load project: ${error}.`;
        });
    },
    selectFolder(key: string) {
      this.selectedFolder = key;
    },
    showErrorMessage(error: string) {
      this.errorMessage = error;
    },
  },
});
</script>
