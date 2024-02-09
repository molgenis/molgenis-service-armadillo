<template>
  <h2>Insight</h2>
  <!-- FIXME: Cannot use tabs as it
    uses router views but we have sub views.
    <Tabs :menu="tabs" :icons="tabIcons" />
  -->
  <div class="row">
    <div class="col">
      <div class="btn-group" role="group" aria-label="Basic outlined example">
        <button
          v-for="(tab, index) in tabs"
          :key="index"
          class="btn"
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
    <div>
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

<style scoped>
.active {
  border: 2p solid red;
  /* Styles for active tab */
}
</style>
