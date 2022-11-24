<template>
  <div
    class="
      file-upload-container
      p-2
      border border-5
      text-primary
      d-flex
      flex-row
      justify-content-center
    "
    @dragover="dragover"
    @mouseenter="activeFileUpload = true"
    @mouseleave="activeFileUpload = false"
    :class="{ 'bg-secondary': activeFileUpload }"
  >
    <div
      class="
        upload-background
        text-center
        d-flex
        flex-column
        justify-content-center
      "
      :class="{ 'bg-secondary text-white': activeFileUpload }"
    >
      <i class="bi bi-upload"></i>
      <span>Drag and drop files or click to select</span>
    </div>
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
</template>

<script lang="ts">
import { defineComponent } from "vue";
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
  data() {
    return {
      uploadDone: false,
      activeFileUpload: false,
    };
  },
  methods: {
    handleFileUpload(event: Event) {
      this.activeFileUpload = false;
      const eventTarget = event.target as HTMLInputElement;
      if (eventTarget && eventTarget.files && eventTarget.files.length > 0) {
        const file = eventTarget.files[0];
        const response = uploadIntoProject(file, this.object, this.project);
        response
          .then(() => {
            this.$emit("upload_success", {
              object: this.object,
              filename: file.name,
            });
          })
          .catch((error: Error) => {
            this.$emit("upload_error", error);
          });
      } else {
        this.$emit("upload_error", "Please select a file");
      }
    },
    dragover(event: Event) {
      event.preventDefault();
    },
  },
});
</script>


<style scoped>
.bi-upload {
  font-size: 3rem;
  font-weight: bold;
}

.file-upload-container {
  position: relative;
  font-size: 2rem;
}

.file-upload-field {
  height: 15rem;
  width: 100%;
  position: absolute;
  z-index: 1337;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
}
.upload-background {
  height: 15rem;
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
