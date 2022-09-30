<template>
  <div>
    <Alert
      v-show="this.errorMsg"
      type="danger"
      class="mt-1"
      :clearFunction="this.clearError"
    >
      <strong>Error: </strong>
      {{ this.errorMsg }}
    </Alert>
    <Alert
      v-show="this.successMsg"
      type="success"
      class="mt-1"
      :clearFunction="this.clearSuccess"
    >
      <strong>Success: </strong>{{ this.successMsg }}
    </Alert>
  </div>
</template>

<script lang="ts">
import Alert from "../components/Alert.vue";
import { defineComponent } from "vue";

export default defineComponent({
  name: "FeedbackMessage",
  components: {
    Alert,
  },
  props: {
    successMessage: String,
    errorMessage: String,
  },
  data() {
    return {
      successMsg: this.successMessage,
      errorMsg: this.errorMessage,
    };
  },
  methods: {
    clearSuccess() {
      this.successMsg = "";
    },
    clearError() {
      this.errorMsg = "";
    },
  },
  watch: {
    successMessage: function (newVal) {
      this.successMsg = newVal;
    },
    errorMessage: function (newVal) {
      this.errorMsg = newVal;
    },
  },
});
</script>
