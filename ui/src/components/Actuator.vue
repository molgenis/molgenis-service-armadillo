<script setup lang="ts">
import { getMetrics } from "@/api/api";
import { ref, watch } from "vue";

const metrics = ref(null);
const names = ref<Array<string>>([]);

const loadMetrics = async () => {
  metrics.value = await getMetrics();
  console.log(metrics.value);
};

loadMetrics();

watch(metrics, async () => {
  const bare: Object = metrics.value ? ["_bare"] : [];
  names.value = Object.keys(bare);
});
</script>
<template>
  <h3>Actuator</h3>
  <div class="row">
    <pre>
      {{ JSON.stringify(metrics, null, 3) }}
    </pre>
    <div>
      <div v-for="name in names">
        <p>{{ name }}</p>
        <!-- <ActuatorItem :name="name"/> -->
      </div>
    </div>
  </div>
</template>
