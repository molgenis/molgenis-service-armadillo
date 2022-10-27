<template>
  <div>
    <div class="row">
      <div class="col">
        <!-- Error messages will appear here -->
        <FeedbackMessage
          :successMessage="successMessage"
          :errorMessage="errorMessage"
        ></FeedbackMessage>
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
          :buttonIcons="[
            // 'folder-plus',
            'file-earmark-plus',
            'trash-fill',
          ]"
          :buttonColors="[
            // 'primary',
            'primary',
            'danger',
          ]"
          :disabledButtons="[
            // false,
            selectedFolder === '',
            selectedFolder === '' || selectedFile === '',
          ]"
          :clickCallbacks="[
            // function () {},
            function () {},
            deleteSelectedFile,
          ]"
        ></ButtonGroup>
        <div class="row mt-1 border border-1">
          <!-- Loading spinner -->
          <LoadingSpinner v-if="loading"></LoadingSpinner>
          <div class="col-6">
            <div class="row">
              <div class="col-6 p-0 m-0">
                <ListGroup
                  ref="folderComponent"
                  :listContent="Object.keys(projectContent)"
                  rowIcon="folder"
                  rowIconAlt="folder2-open"
                  :altIconCondition="showSelectedFolderIcon"
                  :selectionColor="selectedFile ? 'secondary' : 'primary'"
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
                  :altIconCondition="isNonTableType"
                  selectionColor="primary"
                ></ListGroup>
              </div>
            </div>
            <div class="row mt-3">
              <div class="col-6">
                <!-- Placeholder for file upload for uploading complete project in future-->
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
          <div
            class="col-6"
            :style="{visibility: selectedFile && selectedFolder ? 'visible' : 'hidden'}"
            ref="previewContainer"
          >
            <!-- Loading spinner -->
            <LoadingSpinner v-if="loading_preview"></LoadingSpinner>
            <div v-if="isNonTableType(selectedFile)">
              <div class="fst-italic">
                No preview available for: {{ selectedFile }}
              </div>
            </div>
            <div v-else-if="!loading_preview">
              <div class="text-end fst-italic">
                Preview:
                {{ `${selectedFile.replace(".parquet", "")} (108x1500)` }}
              </div>
              <SimpleTable
                :data="filePreview"
                :maxWidth="previewContainerWidth"
              ></SimpleTable>
            </div>
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
import { getProject, deleteObject, previewObject } from "@/api/api";
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
      loading_preview: false,
      successMessage: "",
      filePreview: [{}],
    };
  },
  watch: {
    // whenever question changes, this function will run
    selectedFile() {
      if (this.selectedFile.endsWith(".parquet")) {
        this.loading_preview = true;
        const response = previewObject(
          this.projectId,
          `${this.selectedFolder}%2F${this.selectedFile}`
        );
        response
          .then((data) => {
            this.filePreview = data;
            this.loading_preview = false;
          })
          .catch((error) => {
            console.error(error);
            this.loading_preview = false;
          });
      }
    },
  },
  computed: {
    previewContainerWidth(): number {
      return this.$refs.previewContainer.clientWidth;
    },
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
    onUploadSuccess({
      object,
      filename,
    }: {
      object: string;
      filename: string;
    }) {
      this.reloadProject();
      this.successMessage = `Successfully uploaded file [${filename}] into directory [${object}] of project: [${this.projectId}]`;
    },
    showSelectedFolderIcon(item: string) {
      return item === this.selectedFolder;
    },
    isNonTableType(item: string) {
      return !item.endsWith(".parquet");
    },
    clearUserMessages() {
      this.successMessage = "";
      this.errorMessage = "";
    },
    clearProjectToEdit() {
      this.projectToEdit = "";
    },
    deleteSelectedFile() {
      const response = deleteObject(
        this.projectId,
        `${this.selectedFolder}%2F${this.selectedFile}`
      );
      response
        .then(() => {
          this.reloadProject();
          this.successMessage = `Successfully deleted file [${this.selectedFile}] from directory [${this.selectedFolder}] of project: [${this.projectId}]`;
        })
        .catch((error) => {
          this.errorMessage = error;
        });
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
          this.loading = false;
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
