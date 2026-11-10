<template>
  <span>
    <DsLogo :height="25" v-if="template === 'ds'" />
    <FlowerLogo
      :height="20"
      v-else-if="template.includes('flwr') || name.startsWith('flower')"
    />
    <DockerLogo :height="25" v-else />
  </span>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import DockerLogo from "@/components/DockerLogo.vue";
import FlowerLogo from "@/components/FlowerLogo.vue";
import DsLogo from "@/components/DsLogo.vue";
import { ContainerType } from "@/types/types";

export default defineComponent({
  name: "ContainerTypeLogo",
  components: { DsLogo, FlowerLogo, DockerLogo },
  props: {
    template: {
      type: String as PropType<ContainerType>,
      required: true,
    },
    name: {
      type: String,
      required: true,
    },
  },
});
</script>

<style scoped></style>
