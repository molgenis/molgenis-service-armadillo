<template>
  <div class="card-body">
    <div class="row">
      <div class="col-7">
        <ExpandedContainerProperty title="Image">
          <template #icon><DockerLogo :height="20" /></template>
          <template #value>
            <span class="font-monospace">{{ image }}</span>
          </template>
        </ExpandedContainerProperty>
        <ExpandedContainerProperty
          title="Version"
          :value="version"
          icon="file-diff"
        />
        <ExpandedContainerProperty
          title="Size"
          :value="size"
          icon="arrows-angle-expand"
        />
        <ExpandedContainerProperty
          title="Port"
          :value="port.toString()"
          icon="usb-symbol"
        />
        <ExpandedContainerProperty
          title="Creation date"
          :value="creationDate"
          icon="calendar-plus"
        />
        <ExpandedContainerProperty
          title="Installation date"
          :value="installationDate"
          icon="calendar"
        />
        <ExpandedContainerProperty title="Auto update" icon="arrow-clockwise">
          <template #value>
            <span
              class="badge bg-success"
              :class="`bg-${autoUpdate ? 'success' : 'danger'}`"
            >
              {{ autoUpdate ? "ON" : "OFF" }}
            </span>
          </template>
        </ExpandedContainerProperty>
        <ExpandedContainerProperty
          v-if="autoUpdate"
          title="Update schedule"
          :value="getFormattedUpdateSchedule(updateSchedule)"
          icon="calendar-event"
        />
        <div class="row mb-1">
          <div class="col mt-2">
            <b>Other options</b>
            <div v-for="(key, value) in options" :key="key">
              {{ key }} = {{ value }}
            </div>
          </div>
        </div>
      </div>
      <div class="col">
        <slot></slot>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { defineComponent, Prop, PropType } from "vue";
import DockerLogo from "@/components/DockerLogo.vue";
import ExpandedContainerProperty from "@/components/ExpandedContainerProperty.vue";
import { ContainerType } from "@/types/types";
import { UpdateSchedule } from "@/types/api";

export default defineComponent({
  name: "ExpandedContainerCard",
  components: { ExpandedContainerProperty, DockerLogo },
  props: {
    name: { type: String, required: true },
    image: { type: String, required: true },
    port: { type: Number, required: true },
    size: { type: String, required: true },
    version: { type: String, required: true },
    installationDate: { type: String, default: "-" },
    creationDate: { type: String, default: "-" },
    template: { type: String as PropType<ContainerType>, default: "default" },
    autoUpdate: { type: Boolean },
    updateSchedule: { type: Object as PropType<UpdateSchedule>, default: {} },
    options: { type: Object },
  },
  methods: {
    getFormattedUpdateSchedule() {
      if (this.updateSchedule !== {}) {
        return this.updateSchedule.frequency === "daily"
          ? `Daily at ${this.updateSchedule.time}`
          : `Weekly, ${this.updateSchedule.day} at ${this.updateSchedule.time}`;
      } else {
        return "";
      }
    },
  },
});
</script>

<style scoped></style>
