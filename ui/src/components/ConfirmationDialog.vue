<template>
  <Alert type="warning" class="mt-1" @clear="$emit('cancel')">
    Are you sure you want to delete [{{ record }}]?
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
  },
};
</script>
