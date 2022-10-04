<template>
  <div>
    <Alert
      v-show="errorMsg"
      type="danger"
      class="mt-1"
      @clear="clearError"
    >
      <strong>Error: </strong>
      {{ errorMsg }}
    </Alert>
    <Alert
      v-show="successMsg"
      type="success"
      class="mt-1"
      @clear="clearSuccess"
    >
      <strong>Success: </strong>{{ successMsg }}
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
