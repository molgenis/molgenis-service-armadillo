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
        <label for="inputServer" class="col-sm-2 col-form-label">
          <i class="bi bi-database-fill"></i> Server URL
        </label>
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
        <label for="inputClient" class="col-sm-2 col-form-label">
          <i class="bi bi-fingerprint"></i> Client ID
        </label>
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
        <label for="inputSecret" class="col-sm-2 col-form-label">
          <i class="bi bi-incognito"></i> Client Secret
        </label>
        <div class="col-sm-10">
          <div class="input-group">
            <input
              :type="showSecret ? 'text' : 'password'"
              class="form-control"
              id="inputSecret"
              v-model="clientSecret"
              :placeholder="clientSecret"
              :disabled="!isEditMode"
            />
            <!-- toggle visibility here -->
            <button
              class="btn btn-info"
              type="button"
              @click="toggleVisibilityClientSecret"
            >
              <i class="bi bi-eye-slash-fill" v-if="showSecret"></i
              ><i class="bi bi-eye-fill" v-else></i>
            </button>
            <!-- copy here -->
            <button
              class="btn"
              :class="isCopied['secret'] ? 'btn-success' : 'btn-secondary'"
              type="button"
              @click="copy(clientSecret, 'secret')"
            >
              <i
                class="bi bi-clipboard-check-fill"
                v-if="isCopied['secret']"
              ></i
              ><i class="bi bi-clipboard-fill" v-else></i>
            </button>
          </div>
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
  data() {
    return {
      showSecret: false,
      isCopied: {
        secret: false,
        id: false,
        uri: false,
      },
      serverUri: this.presetServerUri,
      clientId: this.presetClientId,
      clientSecret: this.presetClientSecret,
      isEditMode: false,
    };
  },
  emits: ["saveOidcConfig"],
  methods: {
    toggleVisibilityClientSecret() {
      this.showSecret = !this.showSecret;
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
    },
    copy(variableToCopy: string, typeToCopy: "id" | "secret" | "uri") {
      const textBlob = new Blob([variableToCopy], { type: "text/plain" });
      const clipboardItemData: {
        [mimeType: string]: Blob | string | Promise<Blob | string>;
      } = {
        "text/plain": textBlob,
      };

      const clipboardItem = new ClipboardItem(clipboardItemData);

      navigator.clipboard
        .write([clipboardItem])
        .then(() => {
          // otherwise this is the context of the then function we're in, rather than our data prop
          const self = this;
          this.isCopied[typeToCopy] = true;
          setTimeout(function () {
            self.isCopied[typeToCopy] = false;
          }, 1000);
        })
        .catch((err) => {
          console.error("Error copying to clipboard:", err);
        });
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
});
</script>
