<template>
  <div class="card mb-2">
    <!-- Title-->
    <h5 class="card-header">
      <span class="row">
        <span class="col-3 position-relative">
          <button
            class="btn-sm ms-0 btn btn-secondary me-2"
            @click="isCollapsed = !isCollapsed"
          >
            <i class="bi bi-chevron-down" v-if="isCollapsed"></i>
            <i class="bi bi-chevron-up" v-else></i>
          </button>
          {{ name }}
        </span>
        <span class="col-5 position-relative">
          <span class="container-info">
            <span class="row fst-italic font-monospace">{{ image }}</span>
            <span class="row">Port: {{ port }}</span>
          </span>
        </span>
        <span class="col-1 ps-0 position-relative">
          <DsLogo :height="25" v-if="template === 'ds'" />
          <FlowerLogo
            class="position-absolute top-50 start-50 translate-middle"
            :height="20"
            v-else-if="template.includes('flwr') || name.startsWith('flower')"
          />
          <DockerLogo
            class="position-absolute top-50 start-50 translate-middle"
            :height="25"
            v-else
          />
        </span>
        <span class="col-1 ps-1 pe-1 position-relative">
          <OnlineStatus
            class="position-absolute top-50 start-50 translate-middle"
            :status="status"
          />
        </span>
        <span class="col-1">
          <button
            class="btn p-0 btn-link"
            :class="`${
              status.text === 'Start' ? 'text-success' : 'text-danger'
            }`"
            @click="
              status.text === 'Start' ? startFunction(name) : stopFunction(name)
            "
            :disabled="loadingEnabled"
          >
            <LoadingSpinner
              v-if="isLoading"
              imageWidth="40"
              class="m-0 mt-1 p-0"
            />
            <h2 class="ms-2 m-0 p-0" v-else>
              <i :class="`bi bi-${status.icon}`"></i>
            </h2>
          </button>
        </span>
        <span class="col-1 position-relative">
          <div
            class="btn-group position-absolute top-50 start-50 translate-middle"
            role="group"
          >
            <button class="btn btn-sm btn-primary bg-primary">
              <!-- pencil -->
              <i class="bi bi-pencil-fill"></i>
            </button>
            <button
              type="button"
              class="btn btn-sm btn-danger bg-danger"
              @click="deleteFunction(this.name)"
            >
              <!-- trashcan -->
              <i class="bi bi-trash-fill"></i>
            </button>
          </div>
        </span>
      </span>
    </h5>
    <ExpandedContainerCard
      v-show="!isCollapsed"
      :name="name"
      :image="image"
      :version="version"
      :size="size"
      :port="port"
      :status="status"
      :template="template"
    >
      <slot></slot>
    </ExpandedContainerCard>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ContainerStatus from "@/components/ContainerStatus.vue";
import { ContainerType, StatusMappingType } from "@/types/types";
import DsLogo from "@/components/DsLogo.vue";
import DockerLogo from "@/components/DockerLogo.vue";
import FlowerLogo from "@/components/FlowerLogo.vue";
import { stopContainer } from "@/api/api";
import containers from "@/views/Containers.vue";
import OnlineStatus from "@/components/OnlineStatus.vue";
import ExpandedContainerCard from "@/components/ExpandedContainerCard.vue";
import ExpandedDSContainerCard from "@/components/ExpandedDSContainerCard.vue";
import DataShieldContainerInfo from "@/components/DataShieldContainerInfo.vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";

export default defineComponent({
  name: "ContainersCard",
  computed: {
    containers() {
      return containers;
    },
  },
  data() {
    return {
      isCollapsed: true,
    };
  },
  components: {
    LoadingSpinner,
    DataShieldContainerInfo,
    ExpandedContainerCard,
    OnlineStatus,
    FlowerLogo,
    DockerLogo,
    DsLogo,
    ContainerStatus,
  },
  props: {
    name: { type: String, required: true },
    image: { type: String, required: true },
    port: { type: Number, required: true },
    size: { type: String, required: true },
    version: { type: String, required: true },
    status: {
      type: Object as PropType<StatusMappingType>,
      required: true,
    },
    template: { type: String as PropType<ContainerType>, default: "default" },
    isLoading: { type: Boolean, default: false },
    loadingEnabled: { type: Boolean, default: false },
    deleteFunction: { type: Function, required: true },
    startFunction: { type: Function, required: true },
    stopFunction: { type: Function, required: true },
  },
});
</script>

<style :scoped>
.container-info {
  font-size: 1rem;
}
</style>
