<template>
  <div>
    <h1>Request: {{requestId}}</h1>
    <ul>
      <li>User: [{{ userId }}]</li>
      <li>Table: [{{ projectId }}/{{ folderId }}/{{ tableId }}]</li>
      <li>Variables: [{{ variables  }}]</li>
    </ul>
    <button class="btn btn-success">Agree</button>
  </div>
</template>
<script lang="ts">
import { useRoute } from "vue-router";
import { defineComponent, onMounted, Ref, ref } from "vue";

export default defineComponent({
  name: "Request",
  components: {  },
  setup() {
    // const route = useRoute();
    // const requestId: Ref<string> = ref("");
    // const projectId: Ref<string> = ref("");
    // const folderId: Ref<string> = ref("");
    // const tableId: Ref<string> = ref("");
    // const variables: Ref<string> = ref("");

    // onMounted(() => {
    //   requestId.value = route.params.requestId as string;
    //   projectId.value = route.params.projectId as string;
    //   folderId.value = route.params.folderId as string;
    //   tableId.value = route.params.fileId as string;
    //   variables.value = route.params.variables as string;
    // });
    // return {
    //   requestId,
    //   projectId,
    //   folderId,
    //   tableId,
    //   variables
    // };
  },
  data() {
    return {
      requestId: "request1",
      userId: "t.j.cadman@umcg.nl",
      projectId: "uncan-cohort1",
      folderId: "core",
      tableId: "cohort_1_core",
      variables: "patient_id,age_group,weight_kg,height_cm"
    }
  }
});
</script>
