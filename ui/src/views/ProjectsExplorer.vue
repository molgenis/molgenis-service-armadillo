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
      <div class="col-0 col-sm-9"></div>
      <div class="col-12 col-sm-3">
        <SearchBar class="mt-1" v-model="searchString" />
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
          Project: {{ $route.params.project }}
        </h1>
        <div>{{ project }}</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import SearchBar from "@/components/SearchBar.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import { putProject, deleteProject, getProject } from "@/api/api";
import { defineComponent, onMounted, Ref, ref } from "vue";
import { Project } from "@/types/api";
import { StringArray } from "@/types/types";
import { useRoute } from "vue-router";

export default defineComponent({
  name: "Projects",
  components: {
    FeedbackMessage,
    LoadingSpinner,
    SearchBar,
  },
  setup() {
    const project: Ref<StringArray> = ref([]);
    onMounted(() => {
      loadProject();
    });
    const loadProject = async () => {
      const route = useRoute();
      const projectId = route.params.projectId as string;
      project.value = await getProject(projectId);
    };
    return {
      project,
      loadProject,
    };
  },
  data() {
    return {
      projectToEdit: "",
      projectToEditIndex: -1,
      errorMessage: "",
      loading: false,
      successMessage: "",
      searchString: "",
    };
  },
  methods: {
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
    inspectProject() {
      console.log("inspecting");
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
  },
});
</script>
