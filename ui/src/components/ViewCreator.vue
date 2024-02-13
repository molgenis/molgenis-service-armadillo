<template>
  <div class="row">
    <div class="col-12">
      <h3>Create view on data</h3>
      <div class="row mt-3">
        <div class="fw-bold">Source table information</div>
        <form>
          <div class="row mb-3 mt-3">
            <label for="inputViewProject" class="col-sm-3 col-form-label"
              >Source project:</label
            >
            <div class="col-sm-9">
              <input
                type="string"
                class="form-control"
                id="inputViewProject"
                v-model="srcProject"
                :disabled="projectId !== undefined"
              />
            </div>
          </div>
          <div class="row mb-3">
            <label for="inputViewFolder" class="col-sm-3 col-form-label"
              >Source folder:</label
            >
            <div class="col-sm-9">
              <input
                type="string"
                class="form-control"
                id="inputViewFolder"
                v-model="srcFolder"
                :disabled="selectedFolder !== undefined"
              />
            </div>
          </div>
          <div class="row mb-3">
            <label for="inputViewTable" class="col-sm-3 col-form-label"
              >Source table:</label
            >
            <div class="col-sm-9">
              <input
                type="string"
                class="form-control"
                id="inputViewTable"
                v-model="srcTable"
                :disabled="selectedFile !== undefined"
              />
            </div>
          </div>
        </form>
      </div>
      <div class="row">
        <div class="col-12" v-if="variables && variables.length > 0">
          {{ variables }}
          <VariableSelector :variables="variables" />
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
                  v-model="viewProject"
                />
              </div>
            </div>
            <div class="row mb-3">
              <label for="inputViewFolder" class="col-sm-3 col-form-label"
                >Folder:</label
              >
              <div class="col-sm-9">
                <input
                  type="string"
                  class="form-control"
                  id="inputViewFolder"
                  v-model="viewFolder"
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
                  v-model="viewTable"
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
            onSave(
              projectId,
              sourceObject,
              viewProject,
              linkedObject,
              variables
            )
          "
        >
          <i class="bi bi-floppy-fill"></i> Save
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { getTableVariables } from "@/api/api";
import VariableSelector from "@/components/VariableSelector.vue";

export default {
  name: "VariableCreator",
  props: {
    selectedFolder: String,
    selectedFile: String,
    projectId: String,
    projects: {
      default: [],
      type: Array,
    },
    onSave: {
      default: () => {},
      type: Function,
    },
  },
  components: {
    VariableSelector,
  },
  computed: {
    linkedObject(): string {
      return `${this.viewFolder}/${this.viewTable}`;
    },
    sourceObject(): string {
      return `${this.selectedFolder}/${this.selectedFile?.replace(
        ".parquet",
        ""
      )}`;
    },
    sourcesAvailable(): boolean {
      return (
        this.srcTable !== undefined &&
        this.srcFolder !== undefined &&
        this.srcProject !== undefined
      );
    },
  },
  data() {
    return {
      viewTable: "",
      viewProject: "",
      viewFolder: "",
      srcTable: this.selectedFile ? this.selectedFile : "",
      srcProject: this.projectId ? this.projectId : "",
      srcFolder: this.selectedFolder ? this.selectedFolder : "",
      error: "",
      variables: [] as string[],
    };
  },
  methods: {
    async fetchVariableData() {
      if (this.srcTable && this.srcFolder && this.srcProject) {
        await getTableVariables(
          this.projectId as string,
          `${this.selectedFolder}%2F${this.selectedFile}`
        )
          .then((data: string[]) => {
            console.log(data);
            this.variables = data;
          })
          .catch((error: any) => {
            this.error = `Cannot load variables for [${this.srcFolder}/${this.srcTable}] of project [${this.srcProject}]. Because: ${error}.`;
          });
      } else {
        return [];
      }
    },
  },
};
</script>
