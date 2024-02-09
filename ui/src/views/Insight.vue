<template>
  <div>
    <h2>Insight</h2>
    <div class="row">
      <div class="col">
        <div class="row">
          <div class="col">
            <h3>Files</h3>
            <RemoteFiles />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import RemoteFiles from "@/components/RemoteFiles.vue";
</script>
