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
      <div class="col-0 col-sm-9"></div>
      <div class="col-12 col-sm-3">
        <SearchBar class="mt-1" v-model="searchString" />
      </div>
    </div>
    <!-- Loading spinner -->
    <LoadingSpinner v-if="loading"></LoadingSpinner>
    <!-- Actual table -->
    <Table
      :dataToShow="getFilteredAndSortedProjects()"
      :allData="projects"
      :indexToEdit="projectToEditIndex"
      :customColumns="['name']"
      :dataStructure="(projectsDataStructure as TypeObject)"
      :highlightedRowIndex="updatedProjectIndex"
    >
      <template #customType="customProps">
        <router-link :to="`/projects-explorer/${customProps.data}`">{{
          customProps.data
        }}</router-link>
      </template>
      <template v-slot:extraHeader>
        <!-- Add extra header for buttons (add user button) -->
        <th></th>
      </template>
      <template #extraColumn="columnProps">
        <!-- Add buttons for editing/deleting users -->
        <th scope="row">
          <ButtonGroup
            :buttonIcons="['pencil-fill', 'trash-fill']"
            :buttonColors="['primary', 'danger']"
            :clickCallbacks="[editProject, removeProject]"
            :callbackArguments="[columnProps.item, columnProps.item]"
          ></ButtonGroup>
        </th>
      </template>
      <template #arrayType="arrayProps">
        <!-- Show Projects as badges -->
        <BadgeList :itemArray="arrayProps.data" :canEdit="false"></BadgeList>
      </template>
      <template #editRow="rowProps">
        <InlineRowEdit
          :row="rowProps.row"
          :save="saveEditedProject"
          :cancel="clearProjectToEdit"
          :hideColumns="[]"
          :dataStructure="(projectsDataStructure as TypeObject)"
        />
      </template>
    </Table>
  </div>
</template>

<script lang="ts">
import Badge from "@/components/Badge.vue";
import BadgeList from "@/components/BadgeList.vue";
import ButtonGroup from "@/components/ButtonGroup.vue";
import InlineRowEdit from "@/components/InlineRowEdit.vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import SearchBar from "@/components/SearchBar.vue";
import Table from "@/components/Table.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import { deleteProject, getProjects, putProject } from "@/api/api";
import { sortAlphabetically, stringIncludesOtherString } from "@/helpers/utils";
import { defineComponent, onMounted, Ref, ref } from "vue";
import { Project } from "@/types/api";
import { TypeObject } from "@/types/types";
import { RouterLink, useRouter } from "vue-router";

export default defineComponent({
  name: "Projects",
  components: {
    Badge,
    BadgeList,
    ButtonGroup,
    FeedbackMessage,
    InlineRowEdit,
    LoadingSpinner,
    SearchBar,
    Table,
  },
  setup() {
    const projects: Ref<Project[]> = ref([]);
    const errorMessage: Ref<string> = ref("");
    const router = useRouter();
    onMounted(() => {
      loadProjects();
    });
    const loadProjects = async () => {
      projects.value = await getProjects().catch((error: string) => {
        if (error === "Unauthorized") {
          router.push("/login");
        } else {
          errorMessage.value = error;
        }
        return [];
      });
    };
    return {
      projects,
      errorMessage,
      loadProjects,
    };
  },
  data() {
    return {
      updatedProjectIndex: -1,
      projectsDataStructure: {
        name: "string",
        users: "array",
      },
      projectToEdit: "",
      projectToEditIndex: -1,
      loading: false,
      successMessage: "",
      searchString: "",
    };
  },
  watch: {
    projectToEdit() {
      this.projectToEditIndex = this.getEditIndex();
    },
  },
  methods: {
    clearUserMessages() {
      this.successMessage = "";
      this.errorMessage = "";
    },
    clearProjectToEdit() {
      this.projectToEdit = "";
      this.reloadProjects();
    },
    editProject(project: Project) {
      this.projectToEdit = project.name;
    },
    getEditIndex() {
      const index = this.projects.findIndex((project: Project) => {
        return project.name === this.projectToEdit;
      });
      // only change when project is cleared, otherwise it will return -1 when name is altered
      if (this.projectToEdit === "" || index !== -1) {
        return index;
      } else return this.projectToEditIndex;
    },
    async reloadProjects() {
      this.loading = true;
      try {
        await this.loadProjects();
        this.loading = false;
      } catch (error) {
        this.errorMessage = `Could not load projects: ${error}.`;
      }
    },
    getFilteredAndSortedProjects(): Project[] {
      let projects = this.projects;
      if (this.searchString) {
        projects = this.projects.filter((project: Project) => {
          return stringIncludesOtherString(project.name, this.searchString);
        });
      }
      if (this.projectToEdit) {
        return projects;
      } else {
        return sortAlphabetically(projects, "name") as Project[];
      }
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
    saveEditedProject() {
      const project: Project = this.projects[this.projectToEditIndex];
      this.saveProject(project, () => {
        // Check if name was altered, then delete the old row
        if (project.name != this.projectToEdit) {
          deleteProject(this.projectToEdit).then(() => {
            this.reloadProjects();
          });
        }
        this.clearProjectToEdit();
      });
    },
    saveProject(project: Project, callback: Function | undefined) {
      this.clearUserMessages();
      const projectName = project.name;
      console.log("this is my name", projectName);
      if (projectName === "") {
        this.errorMessage = "Cannot create project with empty name.";
      } else {
        putProject(project)
          .then(async () => {
            this.successMessage = `[${project.name}] was successfully saved.`;
            await this.reloadProjects();
            if (callback) {
              callback();
            }
            this.updatedProjectIndex = this.getFilteredAndSortedProjects().findIndex(
              (p) => {
                return p.name === projectName;
              }
            );
            setTimeout(this.clearUpdatedProjectIndex, 1000);
          })
          .catch((error) => {
            this.errorMessage = `Could not save [${project.name}]: ${error}.`;
          });
      }
    },
    clearUpdatedProjectIndex() {
      this.updatedProjectIndex = -1;
    },
  },
});
</script>
