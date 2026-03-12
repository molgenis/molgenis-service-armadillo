<template>
  <div class="card">
    <h5 class="card-header">
      <i class="bi bi-door-open-fill"></i> Authentication server
      <button
        class="btn btn-primary btn-sm float-end"
        @click="turnOnEditmode"
        v-if="!isEditMode"
      >
        <i class="bi bi-pencil-fill"></i>
      </button>
    </h5>
    <div class="card-body">
      <FormInput
        v-if="renderComponent"
        ref="serverUri"
        label="Server URL"
        icon="database-fill"
        :value="serverUri"
        :hasCopyButton="true"
        type="text"
        :isEditMode="isEditMode"
      />
      <FormInput
        v-if="renderComponent"
        ref="clientId"
        label="Client ID"
        icon="fingerprint"
        :value="clientId"
        :hasCopyButton="true"
        type="text"
        :isEditMode="isEditMode"
      />
      <FormInput
        v-if="renderComponent"
        ref="clientSecret"
        label="Client Secret"
        icon="incognito"
        :value="clientSecret"
        :hasCopyButton="true"
        type="password"
        :isEditMode="isEditMode"
      />
      <button class="btn btn-danger" v-if="isEditMode" @click="cancelEdit">
        <i class="bi bi-x-lg"></i> Cancel
      </button>
      <button class="btn btn-primary" v-if="isEditMode" @click="triggerSave">
        <i class="bi bi-floppy-fill"></i> Save
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import FormInput from "./FormInput.vue";

export default defineComponent({
  name: "OidcConfig",
  props: {
    presetServerUri: {
      type: String,
      required: true,
    },
    presetClientId: {
      type: String,
      required: true,
    },
    presetClientSecret: {
      type: String,
      required: true,
    },
  },
  components: { FormInput },
  data() {
    return {
      renderComponent: true,
      serverUri: this.presetServerUri,
      clientId: this.presetClientId,
      clientSecret: this.presetClientSecret,
      isEditMode: false,
    };
  },
  emits: ["saveOidcConfig"],
  methods: {
    async forceRerender() {
      // Remove MyComponent from the DOM
      this.renderComponent = false;
      // Wait for the change to get flushed to the DOM
      await this.$nextTick();
      // Add the component back in
      this.renderComponent = true;
    },
    turnOffEditmode() {
      this.isEditMode = false;
    },
    turnOnEditmode() {
      this.isEditMode = true;
    },
    cancelEdit() {
      this.turnOffEditmode();
      this.serverUri = this.presetServerUri;
      this.clientId = this.presetClientId;
      this.clientSecret = this.presetClientSecret;
      this.forceRerender();
    },
    triggerSave() {
      this.turnOffEditmode();
      this.$emit("saveOidcConfig", {
        issuerUri: (this.$refs as any).serverUri.mappedValue,
        clientId: (this.$refs as any).clientId.mappedValue,
        clientSecret: (this.$refs as any).clientSecret.mappedValue,
      });
    },
  },
});
</script>
