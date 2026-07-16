<template>
  <div>
    <FileUpload ref="fileUpload" :uniqueClass="uniqueClass" :triggerUpload="triggerUpload" :uploadFileMethod="uploadDataFile" :isUploadingFile="isUploadingFile" @upload_error="emitError" @upload_success="emitSuccess"/>
    <div
      v-if="getFileName().endsWith('.csv') || getFileName().endsWith('.tsv')"
      class="row mb-3 small border-top mt-3 pt-2"
    >
      <div class="col">
        <div class="form-check">
          <input
            class="form-check-input"
            type="checkbox"
            id="csv-checkbox"
            v-model="uploadCsvAsParquet"
          />
          <label class="form-check-label" for="csv-checkbox">
            Convert file to parquet upon upload
          </label>
          <div>
            <small id="csv-help" class="form-text text-muted" >
              Converts file so that it can be read as table by DataSHIELD,
              however if you have another use case you may want to uncheck
              this.
            </small>
          </div>
        </div>
      </div>
      <label v-if="uploadCsvAsParquet" for="typeRows" class="form-label">
        Determine types based on:
      </label>
      <div class="col-4" v-if="uploadCsvAsParquet">
        <input
          type="text"
          id="typeRows"
          class="form-control form-control-sm"
          v-model="typeRows"
        />
      </div>
      <div class="col-8" v-if="uploadCsvAsParquet">lines</div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { uploadIntoProject, uploadCsvIntoProject } from "@/api/api";
import FileUpload from "@/components/FileUpload.vue";

export default defineComponent({
  name: "DataUpload",
  components: {
    FileUpload
  },
  props: {
    object: { type: String, required: true },
    project: { type: String, required: true },
    uniqueClass: { type: String, required: true },
    triggerUpload: { type: Boolean, default: false },
  },
  emits: ["upload_success", "upload_error"],
  computed: {
    file(): any {
      return this.$refs.fileUpload ? this.$refs.fileUpload as any : ""
    }
  },
  data(): {
    uploadCsvAsParquet: boolean;
    uploadDone: boolean;
    typeRows: number;
    isUploadingFile: boolean;
  } {
    return {
      uploadCsvAsParquet: true,
      uploadDone: false,
      typeRows: 100,
      isUploadingFile: false
    };
  },
  methods: {
    emitSuccess() {
      this.$emit("upload_success", {
          filename: this.getFileName(),
      });
    },
    emitError(error: string) {
      this.$emit("upload_error", error);
    },
    getFileName() {
      return this.$refs.fileUpload && (this.$refs.fileUpload as any).file ? (this.$refs.fileUpload as any).file.name : "";
    },
    uploadDataFile() {
      const fileName = this.getFileName();
      this.isUploadingFile = true;
      if (
        (fileName.endsWith(".tsv") ||
          fileName.endsWith(".csv")) &&
        this.uploadCsvAsParquet
      ) {
        return uploadCsvIntoProject(
          this.file.file,
          this.object,
          this.project,
          this.typeRows
        );
      } else {
        return uploadIntoProject(
          this.file.file,
          this.object,
          this.project
        );
      }
    },
    dragover(event: Event) {
      event.preventDefault();
    },
  },
});
</script>

<style scoped>
.upload-icon {
  font-size: 3rem;
  font-weight: bold;
}

.file-upload-container {
  position: relative;
}

.file-upload-field {
  height: 12rem;
  width: 100%;
  position: absolute;
  z-index: 1337;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
}
.upload-background {
  height: 12rem;
  width: 100%;
  position: relative;
  z-index: 1;
}

.hidden-input {
  opacity: 0%;
}

.hidden-input:hover {
  cursor: pointer;
}
</style>
