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
          <input class="form-control form-control-sm" ref="file" v-on:change="handleFileUpload()" :class="uniqueClass" type="file">
        </div>
      </p>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, Ref, ref } from 'vue';
import { uploadIntoProject } from '@/api/api'
import { file } from '@babel/types';

export default defineComponent({
  name: "FileUpload",
  setup (props, { emit }) {
     const file: Ref = ref(null);

        const handleFileUpload = async() => {
            //Upload to server
            const response = uploadIntoProject(file.value.files[0], props.object, props.project);
            response.then(()=>{
              emit('upload_success', {object: props.object, filename: file.value.files[0].name});
            }).catch((error: Error) => {
              emit('upload_error', error);
            })
        }
        return {
          handleFileUpload,
          file
       }
  },
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
