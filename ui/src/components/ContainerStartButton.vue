<template>
  <button
    class="btn p-0 btn-link"
    :class="`${status.text === 'Start' ? 'text-success' : 'text-danger'}`"
    @click="status.text === 'Start' ? startFunction(name) : stopFunction(name)"
    :disabled="isDisabled"
  >
    <LoadingSpinner v-if="isLoading" imageWidth="40" class="m-0 mt-1 p-0" />
    <h2 class="ms-2 mt-1 mb-1 me-0 p-0" v-else>
      <i :class="`bi bi-${status.icon}`"></i>
    </h2>
  </button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import { StatusMappingType } from "@/types/types";

export default defineComponent({
  name: "ContainerStartButton",
  components: { LoadingSpinner },
  props: {
    status: {
      type: Object as PropType<StatusMappingType>,
      required: true,
    },
    name: {
      type: String,
      required: true,
    },
    startFunction: {
      type: Function,
      required: true,
    },
    stopFunction: {
      type: Function,
      required: true,
    },
    isDisabled: {
      type: Boolean,
      required: true,
    },
    isLoading: {
      type: Boolean,
      required: true,
    },
  },
});
</script>

<style scoped></style>
