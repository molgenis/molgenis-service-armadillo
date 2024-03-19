<template>
  <h2>Insight</h2>
  <div class="row">
    <div class="col-2">
      <button
        v-for="(tab, index) in tabs"
        :key="index"
        class="btn btn-sm"
        :class="{
          active: currentTab === index,
          'btn-primary': currentTab === index,
          'btn-secondary': currentTab !== index,
        }"
        @click="currentTab = index"
      >
        {{ tab }}
      </button>
    </div>
    <div class="col-10">
      <div v-show="currentTab === 0">
        <RemoteFiles />
      </div>
      <div v-show="currentTab === 1">
        <Actuator />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import Actuator from "@/components/Actuator.vue";
import RemoteFiles from "@/components/RemoteFiles.vue";
import { ref } from "vue";

let currentTab = ref(0);
const tabs = ["Files", "Server metrics"];
</script>
