<template>
  <div class="card text-bg-light mb-3 file-upload border border-5 text-primary" @dragover="dragover" >
    <div class="card-body">
      <p class="card-text align-middle text-center">
        <h1>
          <div><i class="bi bi-upload"></i></div>
        </h1>
        <div>Drag to upload</div>
        <input ref="file" v-on:change="handleFileUpload()"  type="file">
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
  setup (props, { emit }) {
     const file: Ref = ref(null);

        const handleFileUpload = async() => {
            //Upload to server
            const response = uploadFile(file.value.files[0], props.object, props.project);
            response.then(()=>{
              emit('upload_success');
            }).catch((error) => {
              emit('upload_error');
              console.error(error);
            })
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
  emits: ['upload_success', 'upload_error'],
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
