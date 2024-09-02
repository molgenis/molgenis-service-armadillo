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
          Project: {{ route.params.projectId }}
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
              @selectFolder="onSelectFolder($event)"
              @selectFile="onSelectFile($event)"
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
                    @click="setCreateLinkFromTarget"
                  >
                    <i class="bi bi-box-arrow-in-up-right"></i> Select table to
                    link from ...
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div class="col-6 p-3 border" v-if="createLinkFromTarget === true">
            <div class="row">
              <div class="col">
                <h3>Create view on table</h3>
              </div>
              <div class="col d-grid d-md-flex justify-content-md-end">
                <button
                  @click="createLinkFromTarget = false"
                  type="button"
                  class="btn btn-danger btn-sm m-1"
                >
                  <i class="bi bi-x"></i> Cancel
                </button>
              </div>
            </div>
            <ViewEditor
              :viewProject="projectId"
              :viewFolder="selectedFolder"
              :onSave="doCreateLinkFile"
              :preselectedVariables="[]"
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
            <div
              v-if="
                isNonTableType(selectedFile) && !isLinkFileType(selectedFile)
              "
            >
              <div class="fst-italic">
                No preview available for: {{ selectedFile }} ({{
                  fileInfo.fileSize
                }})
              </div>
            </div>
            <div v-else-if="!loading_preview && !askIfPreviewIsEmpty()">
              <div class="text-end fst-italic">
                Preview:
                {{ `${selectedFile.replace(".parquet", "")}` }} ({{
                  `${fileInfo.dataSizeRows}x${fileInfo.dataSizeColumns}`
                }})
                <div v-if="isLinkFileType(selectedFile)">
                  Linked from: {{ fileInfo.sourceLink }}
                </div>
                <button
                  v-if="!createLinkFromSrc && isTableType(selectedFile)"
                  @click="createLinkFromSrc = true"
                  type="button"
                  class="btn btn-primary btn-sm m-1"
                >
                  <i class="bi bi-box-arrow-in-up-right"></i> Create view
                </button>
                <button
                  v-else-if="!editView && isLinkFileType(selectedFile)"
                  @click="editView = true"
                  type="button"
                  class="btn btn-primary btn-sm m-1"
                >
                  <i class="bi bi-box-arrow-in-up-right"></i> Edit view
                </button>
                <button
                  v-else
                  @click="cancelView()"
                  type="button"
                  class="btn btn-danger btn-sm m-1"
                >
                  <i class="bi bi-x"></i> Cancel
                </button>
              </div>
              <ViewEditor
                v-if="createLinkFromSrc === true"
                :sourceFolder="selectedFolder"
                :sourceTable="selectedFile"
                :sourceProject="projectId"
                :onSave="doCreateLinkFile"
                :preselectedVariables="[]"
              ></ViewEditor>
              <ViewEditor
                v-else-if="editView === true"
                :sourceFolder="fileInfo.sourceLink.split('/')[1]"
                :sourceTable="fileInfo.sourceLink.split('/')[2] + '.parquet'"
                :sourceProject="fileInfo.sourceLink.split('/')[0]"
                :preselectedVariables="fileInfo.variables"
                :viewProject="projectId"
                :viewFolder="selectedFolder"
                :viewTable="selectedFile"
                :onSave="doCreateLinkFile"
              ></ViewEditor>
              <DataPreviewTable
                v-else
                :data="filePreview"
                :maxWidth="previewContainerWidth"
                :n-rows="fileInfo.dataSizeRows"
              ></DataPreviewTable>
              <ColumnNamesPreview
                :columnNames="columnNames"
                :buttonName="'+ ' + (columnNames.length - 10) + ' variables: '"
              >
              </ColumnNamesPreview>
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
import ColumnNamesPreview from "@/components/ColumnNamesPreview.vue";
import {
  getProject,
  deleteObject,
  previewObject,
  getFileDetails,
  createLinkFile,
  getTableVariables,
} from "@/api/api";
import {
  isEmptyObject,
  isLinkFileType,
  isTableType,
  isNonTableType,
  getRestructuredProject,
} from "@/helpers/utils";
import { defineComponent, onMounted, Ref, ref } from "vue";
import { StringArray, ProjectsExplorerData } from "@/types/types";
import { useRoute, useRouter } from "vue-router";
import FileUpload from "@/components/FileUpload.vue";
import FileExplorer from "@/components/FileExplorer.vue";
import DataPreviewTable from "@/components/DataPreviewTable.vue";
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
    DataPreviewTable,
    ViewEditor,
    ColumnNamesPreview,
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
      editView: false,
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
      createNewFolder: false,
      projectContent: {},
      fileInfo: {
        fileSize: "",
        dataSizeRows: 0,
        dataSizeColumns: 0,
        sourceLink: "",
        variables: [] as StringArray,
      },
      createLinkFromTarget: false,
      createLinkFromSrc: false,
      columnNames: [],
    };
  },
  watch: {
    selectedFile() {
      if (
        this.projectId !== "" &&
        this.selectedFolder !== "" &&
        this.selectedFile !== ""
      ) {
        this.resetCreateLinkFile();
        if (
          this.isTableType(this.selectedFile) ||
          this.isLinkFileType(this.selectedFile)
        ) {
          this.loading_preview = true;
          previewObject(
            this.projectId,
            `${this.selectedObject}`
          )
            .then((data) => {
              this.filePreview = data;
              this.loading_preview = false;
            })
            .catch((error) => {
              this.errorMessage = `Cannot load preview for [${this.selectedObject}] of project [${this.projectId}]. Because: ${error}.`;
              this.clearFilePreview();
              this.loading_preview = false;
            });

          getFileDetails(
          this.projectId,
          `${this.selectedObject}`
        )
          .then((data) => {
            this.fileInfo.fileSize = data["size"];
            this.fileInfo.dataSizeRows = parseInt(data["rows"]);
            this.fileInfo.dataSizeColumns = parseInt(data["columns"]);
            this.fileInfo.sourceLink = data["sourceLink"];
            this.fileInfo.variables = data["variables"];
            if (isLinkFileType(this.selectedFile)) {
              this.columnNames = this.fileInfo.variables
            } else {
              this.getTableColumnNames( this.projectId, `${this.selectedObject}`)
            }
          })
          .catch((error) => {
            this.errorMessage = `Cannot load details for [${this.selectedObject}] of project [${this.projectId}]. Because: ${error}.`;
          });  
        }
      }
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
    selectedObject(): String {
      return `${this.selectedFolder}/${this.selectedFile}`
    }
  },
  methods: {
    isTableType,
    isLinkFileType,
    isNonTableType,
    askIfPreviewIsEmpty() {
      return isEmptyObject(this.filePreview[0]);
    },
    cancelView() {
      this.createLinkFromSrc = false;
      this.editView = false;
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
    resetCreateLinkFile() {
      this.createLinkFromSrc = false;
      this.createLinkFromTarget = false;
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
    getTableColumnNames (project: string, object: string) {
      getTableVariables(
        project,
        object
      )
        .then((data) => {
          this.columnNames = data; 
        })
        .catch((error) => {
          this.errorMessage = `Cannot load column names for [${object}] of project [${project}]. Because: ${error}.`;
        });
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
        `${this.selectedObject}`
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
    createNewLinkFile(
      sourceProject: string,
      sourceObject: string,
      viewProject: string,
      viewObject: string,
      variables: string[]
    ) {
      if (
        sourceProject !== "" &&
        sourceObject !== "" &&
        viewProject !== "" &&
        viewObject !== "" &&
        variables.length !== 0
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
            this.resetCreateLinkFile();
            this.reloadProject();
            this.selectedFolder = "";
            this.selectedFile = "";
          })
          .catch((error) => {
            this.errorMessage = `${error}`;
          });
      } else {
        this.errorMessage =
          "Cannot save, ensure all fields are filled in properly";
      }
    },
    onSelectFolder(folder: string) {
      this.selectedFolder = folder;
    },
    onSelectFile(file: string) {
      this.selectedFile = file;
    },
    setCreateLinkFromTarget() {
      this.selectedFile = "";
      this.editView = false;
      this.resetFileInfo();
      this.createLinkFromTarget = true;
    },
    resetFileInfo() {
      this.fileInfo = {
        fileSize: "",
        dataSizeRows: 0,
        dataSizeColumns: 0,
        sourceLink: "",
        variables: [] as StringArray,
      };
    },
    doCreateLinkFile(
      sourceProject: string,
      sourceObject: string,
      viewProject: string,
      viewObject: string,
      variables: string[]
    ) {
      if (this.editView) {
        // first check is saving will work
        const tmpResponse = createLinkFile(
          sourceProject,
          sourceObject,
          viewProject,
          viewObject + ".tmp",
          variables
        );
        tmpResponse
          .then(() => {
            const deleteResponse = deleteObject(viewProject, viewObject);
            deleteResponse
              .then(() => {
                this.createNewLinkFile(
                  sourceProject,
                  sourceObject,
                  viewProject,
                  viewObject.replace(".alf", ""),
                  variables
                );
                deleteObject(viewProject, viewObject + ".tmp.alf");
                this.editView = false;
                if (this.projectId !== "" && this.selectedFolder !== "") {
                  this.router.push(
                    `/projects-explorer/${this.projectId}/${this.selectedObject}`
                  );
                }
              })
              .catch((error) => {
                this.errorMessage = `${error}`;
              });
            this.reloadProject();
            this.resetCreateLinkFile();
          })
          .catch((error) => {
            this.errorMessage = `${error}`;
          });
      } else {
        this.createNewLinkFile(
          sourceProject,
          sourceObject,
          viewProject,
          viewObject,
          variables
        );
      }
    },
  },
});
</script>
