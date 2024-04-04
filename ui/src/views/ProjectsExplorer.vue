<template>
  <div>
    <div class="row">
      <div class="col">
        <!-- Error messages will appear here -->
        <FeedbackMessage
          :successMessage="successMessage"
          :errorMessage="errorMessage"
        ></FeedbackMessage>
        <ConfirmationDialog
          v-if="fileToDelete != ''"
          :record="`${folderToDeleteFrom}/${fileToDelete}`"
          action="delete"
          recordType="file"
          @proceed="proceedDelete"
          @cancel="clearRecordToDelete"
        ></ConfirmationDialog>
      </div>
    </div>
    <div class="row">
      <div class="col-12">
        <h2 class="mt-3">
          <button type="button" class="btn btn-sm me-1 btn-primary bg-primary">
            <router-link to="/projects">
              <i class="bi bi-arrow-left text-light"></i>
            </router-link>
          </button>
          Project: {{ $route.params.projectId }}
        </h2>
        <ButtonGroup
          :buttonIcons="['folder-plus', 'trash-fill']"
          :buttonColors="['primary', 'danger']"
          :disabledButtons="[
            false,
            selectedFolder === '' || selectedFile === '',
          ]"
          :clickCallbacks="[setCreateNewFolder, deleteSelectedFile]"
        ></ButtonGroup>
        <div class="row">
          <div class="col-3">
            <FolderInput
              v-if="createNewFolder"
              :addNewFolder="addNewFolder"
              :cancelNewFolder="cancelNewFolder"
            ></FolderInput>
          </div>
        </div>
        <div class="row mt-1 border border-1">
          <!-- Loading spinner -->
          <LoadingSpinner v-if="loading" class="pt-3 mt-3"></LoadingSpinner>
          <div class="col-6" v-else>
            <FileExplorer
              v-if="!loading"
              :projectContent="projectContent"
              :addNewFolder="addNewFolder"
              @selectFolder="selectedFolder = $event"
              @selectFile="selectedFile = $event"
            />
            <div class="row mt-3">
              <div class="col-6">
                <!-- Placeholder for file upload for uploading complete project in future -->
              </div>
              <div class="col-6 p-0 mb-3" v-show="selectedFolder !== ''">
                <FileUpload
                  :project="projectId"
                  :object="selectedFolder"
                  @upload_success="onUploadSuccess"
                  @upload_error="showErrorMessage"
                  uniqueClass="project-file-upload"
                  :preselectedItem="selectedFile"
                ></FileUpload>
                <div class="d-grid gap-2 d-md-flex justify-content-md-end">
                  <button
                    class="btn btn-primary me-md-2"
                    style="width: 100%"
                    type="button"
                    @click="showViewEditor = true"
                  >
                    <i class="bi bi-box-arrow-in-up-right"></i> Select table to
                    link from ...
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div class="col-6 p-3 border" v-if="showViewEditor == true">
            <div class="row">
              <div class="col">
                <h3>Create view on table</h3>
              </div>
              <div class="col d-grid d-md-flex justify-content-md-end">
                <button
                  @click="showViewEditor = false"
                  type="button"
                  class="btn btn-danger btn-sm m-1"
                >
                  <i class="bi bi-x"></i> Cancel
                </button>
              </div>
            </div>
            <ViewEditor
              sourceFolder=""
              sourceTable=""
              sourceProject=""
              viewTable=""
              :viewProject="projectId"
              :viewFolder="selectedFolder"
              :onSave="doCreateLinkFile"
            ></ViewEditor>
          </div>
          <div
            v-else
            class="col-6"
            :style="{
              visibility: selectedFile && selectedFolder ? 'visible' : 'hidden',
            }"
            ref="previewContainer"
          >
            <!-- Loading spinner -->
            <LoadingSpinner
              v-if="loading_preview"
              class="pt-3 mt-3"
            ></LoadingSpinner>
            <div v-if="isNonTableType(selectedFile)">
              <div class="fst-italic">
                No preview available for: {{ selectedFile }} ({{ fileSize }})
              </div>
            </div>
            <div v-else-if="!loading_preview && !askIfPreviewIsEmpty()">
              <div class="text-end fst-italic">
                Preview:
                {{ `${selectedFile.replace(".parquet", "")}` }} ({{
                  `${dataSizeRows}x${dataSizeColumns}`
                }})
              </div>
              <SimpleTable
                :data="filePreview"
                :maxWidth="previewContainerWidth"
                :n-rows="dataSizeRows"
                :n-cols="dataSizeColumns"
              ></SimpleTable>
            </div>
            <div v-else-if="!loading_preview && askIfPreviewIsEmpty()">
              <div class="fst-italic">
                Error loading: [{{ selectedFile }}]. No preview available.
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import ButtonGroup from "@/components/ButtonGroup.vue";
import ConfirmationDialog from "@/components/ConfirmationDialog.vue";
import FolderInput from "@/components/FolderInput.vue";
import ListGroup from "@/components/ListGroup.vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import {
  getProject,
  deleteObject,
  previewObject,
  getFileDetails,
  createLinkFile,
} from "@/api/api";
import {
  isEmptyObject,
  isTableType,
  isNonTableType,
  getRestructuredProject,
} from "@/helpers/utils";
import { defineComponent, onMounted, Ref, ref } from "vue";
import { StringArray, ProjectsExplorerData } from "@/types/types";
import { useRoute, useRouter } from "vue-router";
import FileUpload from "@/components/FileUpload.vue";
import FileExplorer from "@/components/FileExplorer.vue";
import SimpleTable from "@/components/SimpleTable.vue";
import { processErrorMessages } from "@/helpers/errorProcessing";
import ViewEditor from "@/components/ViewEditor.vue";

export default defineComponent({
  name: "ProjectsExplorer",
  emits: ["triggerUploadFile"],
  components: {
    ButtonGroup,
    ConfirmationDialog,
    FeedbackMessage,
    ListGroup,
    LoadingSpinner,
    FileUpload,
    FileExplorer,
    FolderInput,
    SimpleTable,
    ViewEditor,
  },
  setup() {
    const project: Ref<StringArray> = ref([]);
    const projectId: Ref<string> = ref("");
    const errorMessage: Ref<string> = ref("");
    const router = useRouter();
    const route = useRoute();
    const previewParam = ref();
    onMounted(() => {
      loadProject(undefined);
    });
    const loadProject = async (idParam: string | undefined) => {
      if (idParam === undefined) {
        idParam = route.params.projectId as string;
      }
      project.value = await getProject(idParam).catch((error: string) => {
        errorMessage.value = processErrorMessages(error, "project", router);
        return [];
      });
      projectId.value = idParam;
    };
    return {
      route,
      router,
      project,
      projectId,
      loadProject,
      errorMessage,
      previewParam,
    };
  },
  data(): ProjectsExplorerData {
    return {
      selectedFile: "",
      selectedFolder: "",
      fileToDelete: "",
      folderToDeleteFrom: "",
      projectToEdit: "",
      projectToEditIndex: -1,
      loading: false,
      loading_preview: false,
      successMessage: "",
      filePreview: [{}],
      fileSize: "",
      dataSizeRows: 0,
      dataSizeColumns: 0,
      createNewFolder: false,
      projectContent: {},
      showViewEditor: false,
    };
  },
  watch: {
    selectedFile() {
      if (this.isTableType(this.selectedFile)) {
        this.loading_preview = true;
        previewObject(
          this.projectId,
          `${this.selectedFolder}%2F${this.selectedFile}`
        )
          .then((data) => {
            this.filePreview = data;
            this.loading_preview = false;
          })
          .catch((error) => {
            this.errorMessage = `Cannot load preview for [${this.selectedFolder}/${this.selectedFile}] of project [${this.projectId}]. Because: ${error}.`;
            this.clearFilePreview();
            this.loading_preview = false;
          });
      }
      getFileDetails(
        this.projectId,
        `${this.selectedFolder}%2F${this.selectedFile}`
      )
        .then((data) => {
          this.fileSize = data["size"];
          this.dataSizeRows = data["rows"];
          this.dataSizeColumns = data["columns"];
        })
        .catch((error) => {
          this.errorMessage = `Cannot load details for [${this.selectedFolder}/${this.selectedFile}] of project [${this.projectId}]. Because: ${error}.`;
        });
    },
    project() {
      this.setProjectContent();
    },
  },
  computed: {
    previewContainerWidth(): number {
      const previewContainer: Element = this.$refs.previewContainer as Element;
      return previewContainer.clientWidth;
    },
    projectFolders(): StringArray {
      return Object.keys(this.projectContent) as StringArray;
    },
  },
  methods: {
    isTableType,
    isNonTableType,
    askIfPreviewIsEmpty() {
      return isEmptyObject(this.filePreview[0]);
    },
    clearFilePreview() {
      this.filePreview = [{}];
    },
    setProjectContent() {
      this.projectContent = getRestructuredProject(
        this.project,
        this.projectId
      );
    },
    setCreateNewFolder() {
      this.createNewFolder = true;
    },
    addNewFolder(folderName: string) {
      if (folderName) {
        if (!folderName.includes("/")) {
          this.project.push(folderName.toLocaleLowerCase() + "/");
          this.successMessage = `Succesfully created folder: [${folderName.toLocaleLowerCase()}]. Please be aware the folder will only persist if you upload files in them.`;
          this.setProjectContent();
          this.cancelNewFolder();
        } else {
          this.errorMessage = "Folder name cannot contain /";
        }
      } else {
        this.errorMessage = "Folder name cannot be empty";
      }
    },
    cancelNewFolder() {
      this.createNewFolder = false;
    },
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
    deleteSelectedFile() {
      this.fileToDelete = this.selectedFile;
      this.folderToDeleteFrom = this.selectedFolder;
    },
    clearRecordToDelete() {
      this.fileToDelete = "";
      this.folderToDeleteFrom = "";
    },
    proceedDelete(fileAndFolder: string) {
      this.clearRecordToDelete();
      const splittedFileAndFolder = fileAndFolder.split("/");
      const file = splittedFileAndFolder[1];
      const folder = splittedFileAndFolder[0];
      const response = deleteObject(
        this.projectId,
        `${this.selectedFolder}%2F${this.selectedFile}`
      );
      response
        .then(() => {
          this.selectedFile = "";
          this.reloadProject(() => {
            if (this.projectFolders.length === 0) {
              this.project.push(folder + "/");
            }
            this.setProjectContent();
          });

          this.successMessage = `Successfully deleted file [${file}] from directory [${folder}] of project: [${this.projectId}]`;
        })
        .catch((error) => {
          this.errorMessage = `${error}`;
        });
    },
    reloadProject(callback: Function | undefined = undefined) {
      this.loading = true;
      this.loadProject(this.projectId)
        .then(() => {
          this.loading = false;
          if (callback) {
            callback();
          }
        })
        .catch((error) => {
          this.loading = false;
          this.errorMessage = `Could not load project: ${error}.`;
        });
    },
    showErrorMessage(error: string) {
      this.errorMessage = error;
    },
    doCreateLinkFile(
      sourceProject: string,
      sourceObject: string,
      viewProject: string,
      viewObject: string,
      variables: string[]
    ) {
      const response = createLinkFile(
        sourceProject,
        sourceObject,
        viewProject,
        viewObject,
        variables
      );
      response
        .then(() => {
          this.successMessage = `Successfully created view from [${sourceProject}/${sourceObject}] in [${viewProject}/${viewObject}]`;
          this.showViewEditor = false;
        })
        .catch((error) => {
          this.errorMessage = `${error}`;
        });
    },
  },
});
</script>
