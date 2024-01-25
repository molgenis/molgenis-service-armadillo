<script setup lang="ts">
import { getMetric } from "@/api/api";
import { ref } from "vue";

const props = defineProps({
  name: {
    type: String,
    required: true,
  },
});

const actuatorItem = ref(null);

async function fetchData() {
  debugger;
  let res = await getMetric(props.name);
  console.log(res);
  actuatorItem.value = res;
}
fetchData();
</script>
<template>
  <div>
    {{ actuatorItem }}
  </div>
</template>
