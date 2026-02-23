<template>
  <div class="card mb-2">
    <!-- Title-->
    <h5 class="card-header">
      <span class="row">
        <span class="col-3 mt-2 position-relative">
          <!--          <CollapseButton ref="collapseButton" @update="updateIsCollapsed"/>-->
          <button
            class="btn-sm ms-0 btn btn-secondary me-2"
            @click="isCollapsed = !isCollapsed"
          >
            <i class="bi bi-chevron-down" v-if="isCollapsed"></i>
            <i class="bi bi-chevron-up" v-else></i>
          </button>
          {{ name }}
        </span>
        <span class="col-5 mt-1 position-relative">
          <span class="container-info">
            <span class="row fw-semibold font-monospace">{{ image }}</span>
            <small class="row font-monospace mt-1">Port: {{ port }}</small>
          </span>
        </span>
        <span class="col-1 ps-0 position-relative">
          <ContainerTypeLogo
            class="position-absolute top-50 start-50 translate-middle"
            :name="name"
            :template="template"
          />
        </span>
        <span class="col-1 ps-1 pe-1 position-relative">
          <OnlineStatus
            class="position-absolute top-50 start-50 translate-middle"
            :status="status"
          />
        </span>
        <span class="col-1">
          <ContainerStartButton
            :name="name"
            :status="status"
            :startFunction="startFunction"
            :stopFunction="stopFunction"
            :isDisabled="loadingEnabled"
            :isLoading="isLoading"
          />
        </span>
        <span class="col-1 position-relative">
          <span
            class="btn-group position-absolute top-50 start-50 translate-middle"
            role="group"
          >
            <button
              class="btn btn-sm btn-primary bg-primary"
              @click="$emit('showEditContainer', name)"
            >
              <!-- pencil -->
              <i class="bi bi-pencil-fill"></i>
            </button>
            <button
              type="button"
              class="btn btn-sm btn-danger bg-danger"
              @click="deleteFunction(name)"
            >
              <!-- trashcan -->
              <i class="bi bi-trash-fill"></i>
            </button>
          </span>
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
      :creationDate="creationDate"
      :installationDate="installationDate"
      :updateSchedule="updateSchedule"
      :options="options"
      :autoUpdate="autoUpdate"
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
import containers from "@/views/Containers.vue";
import OnlineStatus from "@/components/OnlineStatus.vue";
import ExpandedContainerCard from "@/components/ExpandedContainerCard.vue";
import DataShieldContainerInfo from "@/components/DataShieldContainerInfo.vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import ContainerStartButton from "@/components/ContainerStartButton.vue";
import ContainerTypeLogo from "@/components/ContainerTypeLogo.vue";
import CollapseButton from "@/components/CollapseButton.vue";
import { UpdateSchedule } from "@/types/api";

export default defineComponent({
  name: "ContainersCard",
  computed: {
    containers() {
      return containers;
    },
  },
  emits: ["showEditContainer"],
  components: {
    CollapseButton,
    ContainerTypeLogo,
    ContainerStartButton,
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
    installationDate: { type: String, default: "-" },
    creationDate: { type: String, default: "-" },
    autoUpdate: { type: Boolean },
    updateSchedule: { type: Object as PropType<UpdateSchedule>, default: {} },
    options: { type: Object, default: {} },
  },
  data() {
    return {
      isCollapsed: true,
    };
  },
  methods: {
    updateIsCollapsed() {
      this.isCollapsed =
        this.$refs.collapseButton &&
        (this.$refs.collapseButton as any).isCollapsed
          ? (this.$refs.collapseButton as any).isCollapsed
          : true;
      console.log(
        this.isCollapsed,
        (this.$refs.collapseButton as any).isCollapsed
      );
    },
  },
});
</script>

<style :scoped>
.container-info {
  font-size: 1rem;
}
</style>
