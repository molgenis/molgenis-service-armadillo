<script setup lang="ts">
import { getMetrics } from "@/api/api";
import { ref, watch } from "vue";
import ActuatorItem from "./ActuatorItem.vue";

const metrics = ref(null);
const names = ref<Array<string>>([]);

const loadMetrics = async () => {
  metrics.value = await getMetrics();
  console.log("Loaded?", metrics.value);
  const bare = metrics.value ? ["_bare"] : {};
  names.value = Object.keys(bare);
  console.log("Names?", names.value);
};

loadMetrics();

watch(metrics, async () => {
  if (metrics.value) {
    // names.value = Object.keys(metrics.value?["_bare"]:{});
    // console.log("Names", names.value);
  }
});
</script>
<template>
  <h3>Actuator</h3>
  <div class="row">
    <div>
      <div v-for="(node, path) in metrics">
        <ActuatorItem :name="path" />
      </div>
    </div>
  </div>
</template>
