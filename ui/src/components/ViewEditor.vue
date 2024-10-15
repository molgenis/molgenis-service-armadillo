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
              <input
                v-if="sourceProject"
                type="string"
                class="form-control"
                disabled
                v-model="srcProject"
              />
              <Dropdown
                v-else
                :options="projects.map((project) => project.name)"
                @update="updateSrcProject"
                :class="formValidated && !srcProject ? 'invalid-field' : ''"
              ></Dropdown>
              <div v-if="formValidated && !srcProject" class="feedback">
                Please select a source project to link from 
              </div>
            </div>
          </div>
          <div class="row mb-3">
            <label for="inputViewFolder" class="col-sm-3 col-form-label">
              Source folder:
            </label>
            <div class="col-sm-9">
              <input
                v-if="sourceFolder"
                type="string"
                class="form-control"
                disabled
                v-model="srcFolder"
              />
              <Dropdown
                v-else
                :options="Object.keys(projectData)"
                @update="updateSrcFolder"
                :class="formValidated && !srcFolder ? 'invalid-field' : ''"
              ></Dropdown>
              <div v-if="formValidated && !srcFolder" class="feedback">
                Please select a source folder to link from 
              </div>
            </div>
          </div>
          <div class="row mb-3">
            <label for="inputViewTable" class="col-sm-3 col-form-label">
              Source table:
            </label>
            <div class="col-sm-9">
              <input
                v-if="sourceTable"
                type="string"
                class="form-control"
                disabled
                v-model="srcTable"
              />
              <Dropdown
                v-else
                :options="getTablesFromListOfFiles(projectData[srcFolder])"
                @update="updateSrcTable"
                :class="formValidated && !srcTable ? 'invalid-field' : ''"
              ></Dropdown>
              <div v-if="formValidated && !srcTable" class="feedback">
                Please select a source table to link from
              </div>
            </div>
          </div>
        </form>
      </div>
      <div class="row">
        <div class="col-12" v-if="variables.length > 0">
          <VariableSelector
            :variables="variables"
            :preselectedVariables="preselectedVariables"
            ref="variableSelector"
            :class="formValidated && ($refs.variableSelector as any).selectedVariables.length === 0 ? 'invalid-field' : ''"
          />
          <div v-if="formValidated && ($refs.variableSelector as any).selectedVariables.length === 0" class="feedback">
            Please select at least one variable
          </div>
        </div>
      </div>
      <div class="row mt-3">
        <div class="fw-bold">View table information</div>
      </div>
      <div class="row mt-3">
        <div class="col">
          <form>
            <div class="row mb-3">
              <label for="inputViewProject" class="col-sm-3 col-form-label">
                Project:
              </label>
              <div class="col-sm-9">
                <input
                  v-if="viewProject"
                  type="string"
                  class="form-control"
                  disabled
                  v-model="vwProject"
                />
                <Dropdown
                  v-else
                  :options="projects.map((project) => project.name)"
                  @update="updateVwProject"
                   :class="formValidated && !vwProject ? 'invalid-field' : ''"
                  required
                ></Dropdown>
                <div v-if="formValidated && !vwProject" class="feedback">
                  Please select a project
                </div>
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
                  :disabled="viewFolder !== undefined"
                  v-model="vwFolder"
                  :class="formValidated && !vwFolder ? 'invalid-field' : ''"
                  required
                />
                <div v-if="formValidated && !vwFolder" class="feedback">
                  Please enter a folder name
                </div>
              </div>
            </div>
            <div class="row mb-3">
              <label for="inputViewTable" class="col-sm-3 col-form-label">
                Table:
              </label>
              <div class="col-sm-9">
                <input
                  type="string"
                  class="form-control"
                  :disabled="isEditMode"
                   :class="formValidated && !vwTable ? 'invalid-field' : ''"
                  v-model="vwTable"
                  required
                />
                <div v-if="formValidated && !vwTable" class="feedback">
                  Please enter a table name
                </div>
              </div>
            </div>
            <div class="d-grid gap-2 d-md-flex justify-content-md-end">
              <button
                class="btn btn-primary"
                type="submit"
                @click.prevent="saveIfValid"
              >
                <i class="bi bi-floppy-fill"></i> Save
              </button>
            </div>
          </form>
        </div>
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
    preselectedVariables: {
      default: [],
      type: Array as PropType<string[]>,
    },
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
        props.sourceTable !== "" &&
        props.sourceFolder !== "" &&
        props.sourceProject !== "" &&
        props.sourceTable !== undefined &&
        props.sourceFolder !== undefined &&
        props.sourceProject !== undefined
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
      formValidated: false,
      vwTable: this.viewTable ? this.viewTable : "",
      vwProject: this.viewProject ? this.viewProject : "",
      vwFolder: this.viewFolder ? this.viewFolder : "",
      srcTable: this.sourceTable ? this.sourceTable : "",
      srcProject: this.sourceProject ? this.sourceProject : "",
      srcFolder: this.sourceFolder ? this.sourceFolder : "",
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
          this.variables = response;
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
    updateVwProject(event: Event) {
      this.vwProject = event.toString();
    },
    updateSrcFolder(event: Event) {
      this.srcFolder = event.toString();
    },
    updateSrcTable(event: Event) {
      this.srcTable = event.toString();
    },
    saveIfValid(){
      if(this.vwTable && this.vwFolder && this.vwProject){
        this.onSave(
                    this.srcProject,
                    this.sourceObject,
                    this.vwProject,
                    this.linkedObject,
                    (this.$refs.variableSelector as any).selectedVariables
                  )
      } else {
        this.formValidated = true;
      }
    }
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
    isEditMode(): boolean {
      // when all items are preselected, we are in edit mode
      return (
        this.sourceFolder !== undefined &&
        this.sourceProject !== undefined &&
        this.sourceTable !== undefined &&
        this.viewFolder !== undefined &&
        this.viewProject !== undefined &&
        this.viewTable !== undefined &&
        this.preselectedVariables.length > 0
      );
    },
  },
});
</script>

<style scoped>
.invalid-field {
  border: 1px;
  border-color: rgb(220, 53, 69);
  border-style: solid;
}

.feedback {
  color:  rgb(220, 53, 69);
  font-style: italic;
  font-size: 0.9rem;
}

</style>