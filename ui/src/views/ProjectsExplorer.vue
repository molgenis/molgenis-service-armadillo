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
          :buttonIcons="['folder-plus', 'file-earmark-plus', 'trash-fill']"
          :buttonColors="['primary', 'primary', 'danger']"
          :disabledButtons="[
            false,
            selectedFolder === '',
            selectedFolder === '' || selectedFile === '',
          ]"
          :clickCallbacks="[
            setCreateNewFolder,
            clickUploadFile,
            deleteSelectedFile,
          ]"
        ></ButtonGroup>
        <div class="row mt-1 border border-1">
          <!-- Loading spinner -->
          <LoadingSpinner v-if="loading"></LoadingSpinner>
          <div class="col-6">
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
                  :listContent="projectFolders"
                  rowIcon="folder"
                  rowIconAlt="folder2-open"
                  :altIconCondition="showSelectedFolderIcon"
                  :preselectedItem="selectedFolder"
                  :selectionColor="selectedFile ? 'secondary' : 'primary'"
                ></ListGroup>
                <div
                  class="input-group input-group-sm m-1"
                  v-if="createNewFolder"
                >
                  <input
                    type="text"
                    class="form-control"
                    placeholder="Folder name"
                    v-model="newFolder"
                  />
                  <button
                    class="btn btn-sm btn-success"
                    type="button"
                    @click="addNewFolder"
                  >
                    <i class="bi bi-check-lg"></i>
                  </button>
                  <button
                    class="btn btn-sm btn-danger"
                    type="button"
                    @click="cancelNewFolder"
                  >
                    <i class="bi bi-x-lg"></i>
                  </button>
                </div>
              </div>
              <div class="col-6 p-0 m-0">
                <ListGroup
                  v-show="selectedFolder !== ''"
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
              <div class="col-6 p-0 mb-3" v-show="selectedFolder !== ''">
                <FileUpload
     
                  :project="projectId"
                  :object="selectedFolder"
                  @upload_success="onUploadSuccess"
                  @upload_error="showErrorMessage"
                  uniqueClass="project-file-upload"
                  :triggerUpload="triggerFileUpload"
                  @upload_triggered="resetFileUpload"
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
            <LoadingSpinner v-if="loading_preview"></LoadingSpinner>
            <div v-if="isNonTableType(selectedFile)">
              <div class="fst-italic">
                No preview available for: {{ selectedFile }}
              </div>
            </div>
            <div v-else-if="!loading_preview">
              <div class="text-end fst-italic">
                Preview:
                <!-- {{ `${selectedFile.replace(".parquet", "")} (108x1500)` }} -->
                {{ `${selectedFile.replace(".parquet", "")}` }}
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
import { useRoute, useRouter } from "vue-router";
import FileUpload from "@/components/FileUpload.vue";
import SimpleTable from "@/components/SimpleTable.vue";

export default defineComponent({
  name: "ProjectsExplorer",
  emits: ["triggerUploadFile"],
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
    const errorMessage: Ref<string> = ref("");
    const router = useRouter();
    const route = useRoute();
    const previewParam = ref();
    watch(
      () => route.params.folderId,
      (newVal) => {
        selectedFolder.value = newVal as string;
      }
    );
    onMounted(() => {
      loadProject(undefined);
      if (route.params.folderId)
        selectedFolder.value = route.params.folderId as string;
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
      project.value = await getProject(idParam).catch((error: string) => {
        if (error === "Unauthorized") {
          router.push("/login");
        } else {
          errorMessage.value = error;
        }
        return [];
      });
      projectId.value = idParam;
    };
    return {
      project,
      projectId,
      loadProject,
      errorMessage,
      folderComponent,
      fileComponent,
      selectedFolder,
      selectedFile,
      previewParam,
    };
  },
  data(): {
    triggerFileUpload: boolean;
    projectToEdit: string;
    projectToEditIndex: number;
    loading: boolean;
    successMessage: string;
    filePreview: Array<any>;
    createNewFolder: boolean;
    loading_preview: boolean;
    newFolder: string;
    projectContent: ObjectWithStringKeyAndStringArrayValue;
  } {
    return {
      triggerFileUpload: false,
      projectToEdit: "",
      projectToEditIndex: -1,
      loading: false,
      loading_preview: false,
      successMessage: "",
      filePreview: [{}],
      createNewFolder: false,
      newFolder: "",
      projectContent: {},
    };
  },
  watch: {
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
          .catch(() => {
            this.loading_preview = false;
          });
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
    projectFolders() {
      return Object.keys(this.projectContent);
    },
  },
  methods: {
    setProjectContent() {
      let content: ObjectWithStringKeyAndStringArrayValue = {};
      this.project.forEach((item) => {
        const splittedItem = item.split("/");
        if (splittedItem[0] == this.projectId) {
          splittedItem.splice(0, 1);
        }

        if (!splittedItem[0].startsWith(".")) {
          if (splittedItem[1] == "") {
            content[splittedItem[0]] = [];
          } else if (!splittedItem[1].startsWith(".")) {
            if (splittedItem[0] in content) {
              content[splittedItem[0]].push(splittedItem[1]);
            } else {
              content[splittedItem[0]] = [splittedItem[1]];
            }
          }
        }
      });
      this.projectContent = content;
    },
    setCreateNewFolder() {
      this.createNewFolder = true;
    },
    addNewFolder() {
      if (this.newFolder) {
        if (!this.newFolder.includes("/")) {
          this.project.push(this.newFolder.toLocaleLowerCase() + "/");
          this.successMessage = `Succesfully created folder: [${this.newFolder.toLocaleLowerCase()}]. Please be aware the folder will only persist if you upload files in them.`;
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
      this.newFolder = "";
    },
    resetFileUpload() {
      this.triggerFileUpload = false;
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
    clickUploadFile() {
      this.triggerFileUpload = true;
    },
    deleteSelectedFile() {
      const folder = this.selectedFolder;
      const file = this.selectedFile;
      const response = deleteObject(
        this.projectId,
        `${this.selectedFolder}%2F${this.selectedFile}`
      );
      response
        .then(() => {
          this.selectedFile = "";
          this.reloadProject( () => {
            if (this.projectFolders.length === 0) {
              this.project.push(folder + "/");
            }
            this.setProjectContent();
          });

          this.successMessage = `Successfully deleted file [${file}] from directory [${folder}] of project: [${this.projectId}]`;
        })
        .catch((error) => {
          this.errorMessage = error;
        });
    },
    editProject(project: Project) {
      this.projectToEdit = project.name;
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
    selectFolder(key: string) {
      this.selectedFolder = key;
    },
    showErrorMessage(error: string) {
      this.errorMessage = error;
    },
  },
});
</script>
