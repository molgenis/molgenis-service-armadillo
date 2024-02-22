<template>
  <div class="row mt-3">
    <div class="col-2">
      <h2>Insight</h2>
    </div>
  </div>
  <div class="row">
    <div class="col-2">
      <ul class="nav flex-column">
        <li class="nav-item">
          <a
            class="nav-link btn text-start"
            :class="currentTab == 0 ? 'active' : ''"
            aria-current="page"
            @click="currentTab = 0"
            ><i class="bi bi-file-earmark-binary"></i> Logs</a
          >
        </li>
        <li class="nav-item">
          <a
            class="nav-link btn text-start"
            :class="currentTab == 1 ? 'active' : ''"
            @click="currentTab = 1"
            ><i class="bi bi-hdd-stack-fill"></i> Metrics</a
          >
        </li>
        <!-- <li class="nav-item">
          <a class="nav-link btn text-start" :class="currentTab == 2 ? 'active' : ''"><i class="bi bi-clipboard2-data-fill" disabled></i> Dashboard</a>
        </li> -->
      </ul>
    </div>
    <div class="col-10 p-2 border">
      <div v-if="currentTab === 0">
        <RemoteFiles />
      </div>
      <div v-else-if="currentTab === 1">
        <Actuator />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Actuator from "@/components/Actuator.vue";
import RemoteFiles from "@/components/RemoteFiles.vue";

export default {
  name: "Insight",
  components: {
    Actuator,
    RemoteFiles,
  },
  data() {
    return {
      currentTab: 0,
      loading: false,
    };
  },
};
</script>

<style :scoped>
.nav-link.active {
  color: black;
  font-weight: bold;
}
</style>
