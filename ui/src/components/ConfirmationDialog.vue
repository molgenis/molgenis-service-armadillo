<template>
  <div class="confirm-container" v-on:click="$emit('cancel')">
    <Alert
      type="warning"
      class="confirm-dialog position-absolute top-50 start-50 translate-middle"
      @clear="$emit('cancel')"
    >
      <div>
        Are you sure you want to {{ action }} {{ recordType }} [{{ record }}]?
      </div>
      <div class="mb-1">
        <small>{{ extraInfo }}</small>
      </div>
      <button
        type="button"
        class="btn btn-sm btn-success"
        v-on:click="$emit('proceed', record)"
      >
        Yes
      </button>
      <button
        type="button"
        class="btn btn-sm btn-danger"
        v-on:click="$emit('cancel')"
      >
        No
      </button>
    </Alert>
  </div>
</template>

<script lang="ts">
import Alert from "@/components/Alert.vue";

export default {
  name: "ConfirmationDialog",
  components: {
    Alert,
  },
  emits: ["proceed", "cancel"],
  props: {
    record: String,
    action: String,
    recordType: String,
    extraInfo: {
      type: String,
      default: "",
    },
  },
};
</script>

<style scoped>
.confirm-dialog {
  position: fixed;
  z-index: 1100;
  opacity: 1 !important;
  pointer-events: all;
  display: block;
}
.confirm-container {
  position: fixed;
  top: 0em;
  left: 0em;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1099;
  width: 100%;
  height: 100%;
  display: block;
}
</style>
