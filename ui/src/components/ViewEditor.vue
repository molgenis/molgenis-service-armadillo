<template>
  <div class="row">
    <div class="col-12">
      <div class="row mt-3">
        <div class="fw-bold">Source table information</div>
        <form>
          <div class="row mb-3 mt-3">
            <label for="inputViewProject" class="col-sm-3 col-form-label">
              Source project:
            </label>
            <div class="col-sm-9">
              <Dropdown
                :options="projects.map((project) => project.name)"
                @update="updateSrcProject"
              ></Dropdown>
            </div>
          </div>
          <div class="row mb-3">
            <label for="inputViewFolder" class="col-sm-3 col-form-label">
              Source folder:
            </label>
            <div class="col-sm-9">
              <Dropdown
                :options="Object.keys(projectData)"
                @update="updateSrcFolder"
              ></Dropdown>
            </div>
          </div>
          <div class="row mb-3">
            <label for="inputViewTable" class="col-sm-3 col-form-label">
              Source table:
            </label>
            <div class="col-sm-9">
              <Dropdown
                :options="getTablesFromListOfFiles(projectData[srcFolder])"
                @update="updateSrcTable"
              ></Dropdown>
            </div>
          </div>
        </form>
      </div>
      <div class="row">
        <div class="col-12" v-if="srcVars.length > 0">
          <VariableSelector :variables="srcVars" />
        </div>
      </div>
      <div class="row mt-3">
        <div class="fw-bold">View table information</div>
      </div>
      <div class="row mt-3">
        <div class="col">
          <form>
            <div class="row mb-3">
              <label for="inputViewProject" class="col-sm-3 col-form-label"
                >Project:</label
              >
              <div class="col-sm-9">
                <input
                  type="string"
                  class="form-control"
                  id="inputViewProject"
                  :disabled="viewProject !== undefined"
                  v-model="vwProject"
                />
              </div>
            </div>
            <div class="row mb-3">
              <label for="inputViewFolder" class="col-sm-3 col-form-label">
                Folder:
              </label>
              <div class="col-sm-9">
                <input
                  type="string"
                  class="form-control"
                  id="inputViewFolder"
                  :disabled="viewFolder !== undefined"
                  v-model="vwFolder"
                />
              </div>
            </div>
            <div class="row mb-3">
              <label for="inputViewTable" class="col-sm-3 col-form-label"
                >Table:</label
              >
              <div class="col-sm-9">
                <input
                  type="string"
                  class="form-control"
                  id="inputViewTable"
                  v-model="vwTable"
                />
              </div>
            </div>
          </form>
        </div>
      </div>
      <div class="d-grid gap-2 d-md-flex justify-content-md-end">
        <button
          class="btn btn-primary"
          type="button"
          @click="
            onSave(srcProject, srcProject, vwProject, viewFolder, variables)
          "
        >
          <i class="bi bi-floppy-fill"></i> Save
        </button>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { getProjects, getTableVariables, getProject } from "@/api/api";
import {
  getRestructuredProject,
  getTablesFromListOfFiles,
} from "@/helpers/utils";
import { Project } from "@/types/api";
import { StringArray, ViewEditorData } from "@/types/types";
import { PropType, Ref, defineComponent, onMounted, ref } from "vue";
import VariableSelector from "@/components/VariableSelector.vue";
import Dropdown from "@/components/Dropdown.vue";

export default defineComponent({
  name: "ViewEditor",
  components: {
    VariableSelector,
    Dropdown,
  },
  props: {
    sourceFolder: String,
    sourceTable: String,
    sourceProject: String,
    viewTable: String,
    viewProject: String,
    viewFolder: String,
    projects: {
      default: [],
      type: Array as PropType<Project[]>,
    },
    onSave: {
      default: () => {},
      type: Function,
    },
  },
  setup(props) {
    const variables = ref<StringArray>([]);
    const projects: Ref<Project[]> = ref([]);
    const errorMessage: Ref<string> = ref("");

    onMounted(() => {
      loadProjects();
      fetchVariables();
    });

    const loadProjects = async () => {
      projects.value = await getProjects().catch((error: string) => {
        errorMessage.value = error;
        return [];
      });
    };

    const isSrcTableSet = () => {
      return (
        props.sourceTable != "" &&
        props.sourceFolder != "" &&
        props.sourceProject != ""
      );
    };
    const fetchVariables = async () => {
      if (isSrcTableSet()) {
        await getTableVariables(
          props.sourceProject as string,
          `${props.sourceFolder}%2F${props.sourceTable}`
        )
          .then((data) => {
            variables.value = data;
          })
          .catch((error: any) => {
            errorMessage.value = `Cannot load variables for [${props.sourceFolder}/${props.sourceTable}] of project [${props.sourceProject}]. Because: ${error}.`;
          });
      }
    };
    return {
      projects,
      fetchVariables,
      variables,
      errorMessage,
    };
  },
  data(): ViewEditorData {
    return {
      projectData: {},
      vwTable: this.viewTable ? this.viewTable : "",
      vwProject: this.viewProject ? this.viewProject : "",
      vwFolder: this.viewFolder ? this.viewFolder : "",
      srcTable: this.sourceTable ? this.sourceTable : "",
      srcProject: this.sourceProject ? this.sourceProject : "",
      srcFolder: this.sourceFolder ? this.sourceFolder : "",
      srcVars: [],
    };
  },
  methods: {
    getTablesFromListOfFiles,
    async getProjectContent(project: string) {
      await getProject(project)
        .then((data) => {
          this.projectData = getRestructuredProject(data, project);
        })
        .catch((error: any) => {
          this.errorMessage = `Cannot load project for [${[
            project,
          ]}]. Because: ${error}.`;
        });
    },
    async getVariables(project: string, folder: string, file: string) {
      await getTableVariables(project, folder + "%2F" + file)
        .then((response) => {
          this.srcVars = response;
        })
        .catch((error) => {
          this.errorMessage = `Cannot retrieve variables for [${
            this.srcFolder + "/" + this.srcTable
          }] of project [${this.srcProject}], because: ${error}`;
        });
    },
    updateSrcProject(event: Event) {
      this.srcProject = event.toString();
    },
    updateSrcFolder(event: Event) {
      this.srcFolder = event.toString();
    },
    updateSrcTable(event: Event) {
      this.srcTable = event.toString();
    },
  },
  watch: {
    srcProject() {
      this.srcFolder = "";
      this.getProjectContent(this.srcProject);
    },
    srcFolder() {
      this.srcTable = "";
    },
    srcTable() {
      this.getVariables(this.srcProject, this.srcFolder, this.srcTable);
    },
  },
  computed: {
    linkedObject(): string {
      return `${this.vwFolder}/${this.vwTable}`;
    },
    sourceObject(): string {
      return `${this.srcFolder}/${this.srcTable?.replace(".parquet", "")}`;
    },
  },
});
</script>
