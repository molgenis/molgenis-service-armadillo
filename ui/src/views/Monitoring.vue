<template>
  <div class="row">
    <div class="col">
      <!-- Error messages will appear here -->
      <FeedbackMessage
        :successMessage="successMessage"
        :errorMessage="errorMessage"
      ></FeedbackMessage>
      <!-- Loading spinner -->
      <LoadingSpinner v-if="loading"></LoadingSpinner>
    </div>
  </div>
  <div class="row">
    <div class="col-0 col-sm-12">Monitoring page</div>
  </div>
</template>

<script lang="ts">
import LoadingSpinner from "../components/LoadingSpinner.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import { defineComponent, onMounted } from "vue";
export default defineComponent({
  name: "Monitoring",
  components: {
    FeedbackMessage,
    LoadingSpinner,
  },
  setup() {
    // const monitoring: Ref<Monitoring[]> = ref([]);
    onMounted(() => {
      //loadMonitoring();
    });
    const loadProfiles = async () => {
      //monitoring.value = await getMonitoring();
    };
    return {
      //monitoring,
      loadProfiles,
    };
  },
  data() {
    return {
      errorMessage: "",
      loading: false,
      successMessage: "",
    };
  },
  computed: {},
  watch: {},
  methods: {},
});
</script>
