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
              </div>
            </div>
          </div>
          <div
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
} from "@/api/api";
import { isEmptyObject, isTableType, isNonTableType } from "@/helpers/utils";
import { defineComponent, onMounted, Ref, ref, watch } from "vue";
import { StringArray, ProjectsExplorerData } from "@/types/types";
import { useRoute, useRouter } from "vue-router";
import FileUpload from "@/components/FileUpload.vue";
import FileExplorer from "@/components/FileExplorer.vue";
import SimpleTable from "@/components/SimpleTable.vue";
import { processErrorMessages } from "@/helpers/errorProcessing";

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
      let content: Record<string, StringArray> = {};
      this.project.forEach((item) => {
        /** scrub the project folder from the name */
        const itemInProjectFolder = item.replace(`${this.projectId}/`, "");
        if (itemInProjectFolder.length && itemInProjectFolder[0] === ".") {
          return; /** if item starts with a . */
        }

        /** Check if it is in a subfolder */
        if (itemInProjectFolder.includes("/")) {
          const splittedItem = itemInProjectFolder.split("/");
          const folder = splittedItem[0];
          const folderItem = splittedItem[1];

          /** add to the content structure */
          if (content[folder]) {
            content[folder] = content[folder].concat(folderItem);
          } else {
            content[folder] = [folderItem];
            if (folderItem === "") {
              content[folder] = [];
            }
          }
        }
      });
      this.projectContent = content;
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
  },
});
</script>
