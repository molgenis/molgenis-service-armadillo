<template>
  <div class="card-body">
    <div class="row">
      <div class="col-5">
        <div class="row">
          <div class="col-4 col-md-4">
            <DockerLogo :height="20" />
            <b class="ms-4">Image</b>
          </div>
          <div class="col font-monospace">{{ image }}</div>
        </div>
        <div class="row">
          <div class="col-4 col-md-4">
            <i class="bi bi-file-diff"></i>
            <b class="ms-4">Version</b>
          </div>
          <div class="col">
            {{ version }}
          </div>
        </div>
        <div class="row">
          <div class="col-4 col-md-4">
            <i class="bi bi-arrows-angle-expand"></i>
            <b class="ms-4">Size</b>
          </div>
          <div class="col">
            {{ size }}
          </div>
        </div>
        <div class="row">
          <div class="col-4 col-md-4">
            <i class="bi bi-usb-symbol"></i>
            <b class="ms-4">Port</b>
          </div>
          <div class="col">
            {{ port }}
          </div>
        </div>
        <div class="row">
          <div class="col-4 col-md-4">
            <i class="bi bi-calendar-plus"></i>
            <b class="ms-4">Creation date</b>
          </div>
          <div class="col">21/09/2025</div>
        </div>
        <div class="row">
          <div class="col-4 col-md-4">
            <i class="bi bi-calendar"></i>
            <b class="ms-4">Installation date</b>
          </div>
          <div class="col">29/09/2025</div>
        </div>
        <div class="row">
          <div class="col-4 col-md-4">
            <i class="bi bi-arrow-clockwise"></i>
            <b class="ms-4">Auto update</b>
          </div>
          <div class="col">
            <span class="badge bg-success">ON</span>
          </div>
        </div>
        <div class="row">
          <div class="col-4 col-md-4">
            <i class="bi bi-calendar-event"></i>
            <b class="ms-4">Update schedule</b>
          </div>
          <div class="col">Daily at 03:00</div>
        </div>
        <div class="row">
          <div class="col mt-2">
            <b>Other options</b>
            <div>-</div>
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
import { defineComponent, PropType } from "vue";
import DockerLogo from "@/components/DockerLogo.vue";
import { ContainerType, StatusMappingType } from "@/types/types";

export default defineComponent({
  name: "ExpandedContainerCard",
  components: { DockerLogo },
  props: {
    name: { type: String, required: true },
    image: { type: String, required: true },
    port: { type: Number, required: true },
    size: { type: String, required: true },
    version: { type: String, required: true },
    template: { type: String as PropType<ContainerType>, default: "default" },
  },
});
</script>

<style scoped></style>
