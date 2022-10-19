<template>
  <div class="card text-bg-light mb-3 file-upload border border-5 text-primary">
    <div class="card-body">
      <p class="card-text align-middle text-center">
        <h1>
          <div><i class="bi bi-upload"></i></div>
        </h1>
        <div>Drag to upload</div>
        <button type="button" class="btn btn-secondary btn-sm mt-3">Select file</button>
        <!-- <div class="text-secondary">Or click <a class="link-primary">here</a></div> -->
      </p>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  name: "FileUpload",
};
</script>
