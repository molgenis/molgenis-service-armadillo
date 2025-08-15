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
          <i :class="'bi bi-' + icons[index]"></i> {{ tab }}
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

<script lang="ts">
import Actuator from "@/components/Actuator.vue";
import RemoteFiles from "@/components/RemoteFiles.vue";
import { defineComponent } from "vue";

export default defineComponent({
  name: "Insight",
  components: {
    Actuator,
    RemoteFiles,
  },
  data() {
    return {
      currentTab: 0,
      icons: ["card-text", "gear-fill"],
      tabs: ["Logs", "Server metrics"],
    };
  },
});
</script>
