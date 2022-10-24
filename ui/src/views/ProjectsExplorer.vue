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
                <FileUpload></FileUpload>
              </div>
              <div class="col-6">
                <FileUpload v-show="selectedFolder != ''"></FileUpload>
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
import { putProject, deleteProject, getProject } from "@/api/api";
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
      loadProject();
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
    const loadProject = async () => {
      const route = useRoute();
      const idParam = route.params.projectId as string;
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
          if (splittedItem[0] in content) {
            content[splittedItem[0]].push(splittedItem[1]);
          } else {
            content[splittedItem[0]] = [splittedItem[1]];
          }
        }
      });
      return content;
    },
  },
  methods: {
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
    reloadProjects() {
      this.loading = true;
      this.loadProject()
        .then(() => {
          this.loading = false;
        })
        .catch((error) => {
          this.errorMessage = `Could not load project: ${error}.`;
        });
    },
    removeProject(project: Project) {
      this.clearUserMessages();
      deleteProject(project.name)
        .then(() => {
          this.successMessage = `[${project.name}] was successfully deleted.`;
          this.reloadProjects();
        })
        .catch((error) => {
          this.errorMessage = `Could not delete [${project.name}]: ${error}.`;
        });
    },
    saveProject(project: Project, callback: Function | undefined) {
      this.clearUserMessages();
      if (project.name === "") {
        this.errorMessage = "Cannot create project with empty name.";
      } else {
        putProject(project)
          .then(() => {
            this.successMessage = `[${project.name}] was successfully saved.`;
            this.reloadProjects();
            if (callback) {
              callback();
            }
          })
          .catch((error) => {
            this.errorMessage = `Could not save [${project.name}]: ${error}.`;
          });
      }
    },
    selectFolder(key: string) {
      this.selectedFolder = key;
    },
  },
});
</script>
