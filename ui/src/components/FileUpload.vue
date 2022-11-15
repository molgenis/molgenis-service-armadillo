<template>
  <div class="card text-bg-light mb-3 file-upload border border-5 text-primary" @dragover="dragover" >
    <div class="card-body">
      <p class="card-text align-middle text-center">
        <h1>
          <div><i class="bi bi-upload"></i></div>
        </h1>
        <!-- <div>Drag to upload</div> -->
        <div class="mb-3">
          <label class="form-label">Select file to upload</label>
          <input class="form-control form-control-sm" v-on:change="handleFileUpload" :class="uniqueClass" type="file">
        </div>
      </p>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import { uploadIntoProject } from '@/api/api'

export default defineComponent({
  name: "FileUpload",
  props: {
    object: { type: String, required: true},
    project: { type: String, required: true},
    uniqueClass: {type: String, required: true},
    triggerUpload: {type: Boolean, default: false},
  },
  emits: ['upload_success', 'upload_error', 'upload_triggered'],
  watch: {
    triggerUpload: function () {
      if(this.triggerUpload) {
        (document.getElementsByClassName(this.uniqueClass)[0] as HTMLButtonElement).click();
        this.$emit('upload_triggered');
      }
    },
  },
  data(){
    return {
      uploadDone: false
    }
  },
  methods: {
    handleFileUpload(event: Event) {
      const eventTarget = event.target as HTMLInputElement;
      if (eventTarget  && eventTarget.files && eventTarget.files.length > 0) {
        const file = eventTarget.files[0];
        const response = uploadIntoProject(file, this.object, this.project);
        response.then(()=>{
          this.$emit('upload_success', {object: this.object, filename: file.name});
        }).catch((error: Error) => {
          this.$emit('upload_error', error);
        })
      } else {
        this.$emit('upload_error', "Please select a file");
      }
    },
    dragover(event: Event) {
      event.preventDefault();
    },
    drop(event: Event & {dataTransfer: DataTransfer}) {
      event.preventDefault();
      // this.files = event.dataTransfer.files;
    }
    
  }
});
</script>
