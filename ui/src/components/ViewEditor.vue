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
              <select class="form-select" v-model="srcProject">
                <option :value="option.name" v-for="option in projects">
                  {{ option.name }}
                </option>
              </select>
            </div>
          </div>
          <div class="row mb-3">
            <label for="inputViewFolder" class="col-sm-3 col-form-label">
              Source folder:
            </label>
            <div class="col-sm-9">
              <input
                type="string"
                class="form-control"
                id="inputViewFolder"
                v-model="srcFolder"
              />
            </div>
          </div>
          <div class="row mb-3">
            <label for="inputViewTable" class="col-sm-3 col-form-label">
              Source table:
            </label>
            <div class="col-sm-9">
              <input
                type="string"
                class="form-control"
                id="inputViewTable"
                v-model="srcTable"
              />
            </div>
          </div>
        </form>
      </div>
      <div class="row">
        <!-- <div class="col-12" v-if="variables && variables.length > 0">
          <VariableSelector :variables="variables" />
        </div> -->
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
import { getProjects, getTableVariables } from "@/api/api";
import { Project } from "@/types/api";
import { StringArray } from "@/types/types";
import { Ref, defineComponent, onMounted, ref } from "vue";

export default defineComponent({
  name: "ViewEditor",
  props: {
    sourceFolder: String,
    sourceTable: String,
    sourceProject: String,
    viewTable: String,
    viewProject: String,
    viewFolder: String,
    projects: {
      default: [],
      type: Array,
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
  data() {
    return {
      vwTable: this.viewTable ? this.viewTable : "",
      vwProject: this.viewProject ? this.viewProject : "",
      vwFolder: this.viewFolder ? this.viewFolder : "",
      srcTable: this.sourceTable ? this.sourceTable : "",
      srcProject: this.sourceProject ? this.sourceProject : "",
      srcFolder: this.sourceFolder ? this.sourceFolder : "",
    };
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
