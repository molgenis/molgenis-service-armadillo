<template>
  <div class="card text-bg-light mb-3 file-upload border border-5 text-primary" @dragover="dragover" >
    <div class="card-body">
      <p class="card-text align-middle text-center">
        <h1>
          <div><i class="bi bi-upload"></i></div>
        </h1>
        <div>Drag to upload</div>
        <input ref="file" v-on:change="handleFileUpload(object, project)"  type="file">
      </p>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, Ref, ref } from 'vue';
import { uploadFile } from '@/api/api'
import { file } from '@babel/types';

export default defineComponent({
  name: "FileUpload",
  setup () {
     const file: Ref = ref(null)

        const handleFileUpload = async(object:string, project: string) => {
           // debugger;
            console.log("selected file",file.value.files)
            //Upload to server
            uploadFile(file.value.files[0], object, project);
        }
        return {
          handleFileUpload,
          file
       }
  },
  props: {
    object: String,
    project: String
  },
  methods: {
    remove(index: number) {
      this.files.splice(index, 1);
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
