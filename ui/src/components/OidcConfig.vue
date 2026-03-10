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
      <div class="mb-3 row">
        <label for="inputServer" class="col-sm-2 col-form-label"
          ><i class="bi bi-database-fill"></i> Server URL</label
        >
        <div class="col-sm-10">
          <input
            type="text"
            class="form-control"
            id="inputServer"
            v-model="serverUri"
            :disabled="!isEditMode"
            :placeholder="serverUri"
          />
        </div>
      </div>
      <div class="mb-3 row">
        <label for="inputClient" class="col-sm-2 col-form-label"
          ><i class="bi bi-fingerprint"></i> Client ID</label
        >
        <div class="col-sm-10">
          <input
            type="text"
            class="form-control"
            id="inputClient"
            v-model="clientId"
            :disabled="!isEditMode"
            :placeholder="clientId"
          />
        </div>
      </div>
      <div class="mb-3 row">
        <label for="inputSecret" class="col-sm-2 col-form-label"
          ><i class="bi bi-incognito"></i> Client Secret</label
        >
        <div class="col-sm-10">
          <input
            type="text"
            class="form-control"
            id="inputSecret"
            v-model="clientSecret"
            :placeholder="clientSecret"
            :disabled="!isEditMode"
          />
        </div>
      </div>
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
  emits: ["saveOidcConfig"],
  methods: {
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
    },
    triggerSave() {
      this.turnOffEditmode();
      this.$emit("saveOidcConfig", {
        issuerUri: this.serverUri,
        clientId: this.clientId,
        clientSecret: this.clientSecret,
      });
    },
  },
  data() {
    return {
      serverUri: this.presetServerUri,
      clientId: this.presetClientId,
      clientSecret: this.presetClientSecret,
      isEditMode: false,
    };
  },
});
</script>
