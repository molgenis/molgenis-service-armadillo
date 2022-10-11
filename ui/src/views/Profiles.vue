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
    <div class="col-0 col-sm-12">Profiles page</div>
  </div>
</template>

<script lang="ts">
import LoadingSpinner from "../components/LoadingSpinner.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import { defineComponent, onMounted } from "vue";
export default defineComponent({
  name: "Profiles",
  components: {
    FeedbackMessage,
    LoadingSpinner,
  },
  setup() {
    // const profiles: Ref<Profiles[]> = ref([]);
    onMounted(() => {
      //loadProfiles();
    });
    const loadProfiles = async () => {
      //profiles.value = await getProfiles();
    };
    return {
      //profiles,
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
