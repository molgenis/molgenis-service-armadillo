<template>
  <div class="file-upload-container text-primary">
    <div class="upload-background text-center row p-3 mx-auto">
      <div
        class="dropzone border border-5 col"
        :class="{ 'bg-secondary text-white': activeFileUpload }"
        @dragover="dragover"
        @mouseenter="activeFileUpload = true"
        @mouseleave="activeFileUpload = false"
      >
        <i class="bi bi-file-earmark-arrow-up-fill upload-icon"></i>
        <h4>Select a file to upload</h4>
        <span class="text-secondary">or drag and drop it here</span>
        <input
          @dragover="activeFileUpload = true"
          @dragend="activeFileUpload = false"
          @dragleave="activeFileUpload = false"
          class="file-upload-field hidden-input"
          @change="handleFileUpload"
          :class="uniqueClass"
          type="file"
        />
      </div>
    </div>
    <div v-if="file && file.name" class="selected-file row text-start ms-2">
      <div class="col">
        <div class="text-muted fw-bold">Selected file:</div>
        <span class="me-1">{{ getTruncatedFileName(file.name) }}</span>
        <button class="btn btn-link btn-sm" @click="clearFile">
          <i class="bi bi-x-circle"></i>
        </button>
        <button
          class="btn btn-primary btn-sm float-end me-3"
          @click="uploadFile"
        >
          <i class="bi bi-upload"></i> Upload
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { truncate } from "@/helpers/utils";
import { uploadIntoProject } from "@/api/api";

export default defineComponent({
  name: "FileUpload",
  props: {
    object: { type: String, required: true },
    project: { type: String, required: true },
    uniqueClass: { type: String, required: true },
    triggerUpload: { type: Boolean, default: false },
  },
  emits: ["upload_success", "upload_error", "upload_triggered"],
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
    uploadDone: boolean;
    activeFileUpload: boolean;
    file: undefined | File;
  } {
    return {
      uploadDone: false,
      activeFileUpload: false,
      file: undefined,
    };
  },
  methods: {
    clearFile() {
      this.file = undefined;
    },
    handleFileUpload(event: Event) {
      this.activeFileUpload = false;
      const eventTarget = event.target as HTMLInputElement;
      if (eventTarget && eventTarget.files && eventTarget.files.length > 0) {
        const file = eventTarget.files[0];
        this.file = file;
      } else {
        this.$emit("upload_error", "Please select a file.");
      }
    },
    getTruncatedFileName(filename: string) {
      return filename.length > 16 ? truncate(filename, 14) : filename;
    },
    getFileName() {
      return this.file && this.file.name ? this.file.name : "";
    },
    uploadFile() {
      if (this.file && this.file.name) {
        const response = uploadIntoProject(
          this.file,
          this.object,
          this.project
        );
        response
          .then(() => {
            this.$emit("upload_success", {
              object: this.object,
              filename: this.getFileName(),
            });
          })
          .catch((error: Error) => {
            this.$emit("upload_error", error);
          });
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
