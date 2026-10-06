<template>
  <span class="badge status-badge" :class="`bg-${status?.color}`">
    <i class="bi bi-broadcast"></i>
    &nbsp;{{ status.status }}
  </span>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { StatusMappingType } from "@/types/types";

export default defineComponent({
  name: "OnlineStatus",
  props: {
    status: Object as PropType<StatusMappingType>,
  },
});
</script>
