<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col">
        <h2>Insight</h2>
      </div>
      <div class="col mt-1">
        <button
          v-for="(tab, index) in tabs"
          :key="index"
          class="btn btn-sm float-end"
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
    </div>
    <div class="row">
      <div class="col">
        <RemoteFiles v-show="currentTab === 0" />
        <Actuator v-show="currentTab === 1" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import Actuator from "@/components/Actuator.vue";
import RemoteFiles from "@/components/RemoteFiles.vue";
import { ref } from "vue";

let currentTab = ref(0);
const tabs = ["Logs", "Server metrics"];
</script>
