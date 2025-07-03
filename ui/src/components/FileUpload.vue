<template>
  <div class="file-upload-container text-primary">
    <div class="upload-background text-center row p-3 mx-auto">
      <div
        v-if="!isUploadingFile"
        class="dropzone border border-5 col"
        :class="{ 'bg-light': isHoveringOverFileUpload }"
        @dragover="dragover"
        @mouseenter="isHoveringOverFileUpload = true"
        @mouseleave="isHoveringOverFileUpload = false"
      >
        <i class="bi bi-file-earmark-arrow-up-fill upload-icon"></i>
        <h4>Select a file to upload</h4>
        <span class="text-secondary">or drag and drop it here</span>
        <input
          @dragover="isHoveringOverFileUpload = true"
          @dragend="isHoveringOverFileUpload = false"
          @dragleave="isHoveringOverFileUpload = false"
          class="file-upload-field hidden-input"
          @change="handleFileUpload"
          :class="uniqueClass"
          type="file"
        />
      </div>
      <LoadingSpinner v-else image-width="60"></LoadingSpinner>
    </div>
    <div v-if="file && file.name" class="selected-file row text-start ms-2">
      <div class="col">
        <div class="text-muted fw-bold">
          {{ isUploadingFile ? "Uploading file: " : "Selected file: " }}
        </div>
        <span class="me-1">{{ getTruncatedFileName(file.name) }}</span>
        <button
          v-if="!isUploadingFile"
          class="btn btn-link btn-sm"
          @click="clearFile"
        >
          <i class="bi bi-x-circle"></i>
        </button>
        <button
          v-if="!isUploadingFile"
          class="btn btn-primary btn-sm float-end me-3"
          @click="uploadFile"
        >
          <i class="bi bi-upload"></i> Upload
        </button>
      </div>
      <div
        v-if="file.name.endsWith('.csv') || file.name.endsWith('.tsv')"
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
            <label class="form-check-label" for="csv-checkbox"
              >Convert file to parquet upon upload</label
            >
            <small id="csv-help" class="form-text text-muted"
              >Converts file so that can be read as table by DataSHIELD, however
              if you have another use case you may want to uncheck this</small
            >
          </div>
        </div>
        <label v-if="uploadCsvAsParquet" for="typeRows" class="form-label"
          >Determine types based on:</label
        >
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
  </div>
</template>

<script lang="ts">
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import { defineComponent } from "vue";
import { truncate } from "@/helpers/utils";
import { uploadIntoProject, uploadCsvIntoProject } from "@/api/api";

export default defineComponent({
  name: "FileUpload",
  props: {
    object: { type: String, required: true },
    project: { type: String, required: true },
    uniqueClass: { type: String, required: true },
    triggerUpload: { type: Boolean, default: false },
  },
  emits: ["upload_success", "upload_error", "upload_triggered"],
  components: {
    LoadingSpinner,
  },
  watch: {
    triggerUpload: function () {
      if (this.triggerUpload) {
        (
          document.getElementsByClassName(
            this.uniqueClass
          )[0] as HTMLButtonElement
        ).click();
        this.$emit("upload_triggered");
      }
    },
  },
  data(): {
    uploadCsvAsParquet: boolean;
    uploadDone: boolean;
    isHoveringOverFileUpload: boolean;
    isUploadingFile: boolean;
    file: undefined | File;
    typeRows: number;
  } {
    return {
      uploadCsvAsParquet: true,
      uploadDone: false,
      isHoveringOverFileUpload: false,
      isUploadingFile: false,
      file: undefined,
      typeRows: 100,
    };
  },
  methods: {
    clearFile() {
      this.file = undefined;
    },
    handleFileUpload(event: Event) {
      const eventTarget = event.target as HTMLInputElement;
      if (eventTarget && eventTarget.files && eventTarget.files.length > 0) {
        const file = eventTarget.files[0];
        this.file = file;
      } else {
        this.$emit("upload_error", "Please select a file.");
      }
    },
    getTruncatedFileName(filename: string) {
      return filename.length > 24 ? truncate(filename, 20) : filename;
    },
    getFileName() {
      return this.file && this.file.name ? this.file.name : "";
    },
    handleUploadResponse(response) {
      response
        .then(() => {
          this.isUploadingFile = false;
          this.$emit("upload_success", {
            object: this.object,
            filename: this.getFileName(),
          });
        })
        .catch((error: Error) => {
          this.isUploadingFile = false;
          this.$emit("upload_error", error);
        });
    },
    uploadFile() {
      if (this.file && this.file.name) {
        this.isUploadingFile = true;
        if (
          (this.file.name.endsWith(".tsv") ||
            this.file.name.endsWith(".csv")) &&
          this.uploadCsvAsParquet
        ) {
          const response = uploadCsvIntoProject(
            this.file,
            this.object,
            this.project,
            this.typeRows
          );
          this.handleUploadResponse(response);
        } else {
          const response = uploadIntoProject(
            this.file,
            this.object,
            this.project
          );
          this.handleUploadResponse(response);
        }
      } else {
        this.$emit("upload_error", "No file selected.");
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
