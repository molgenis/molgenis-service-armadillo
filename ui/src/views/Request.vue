<template>
  <div>
    <div class="row">
      <div class="col">
        <FeedbackMessage
          :successMessage="successMessage"
          :errorMessage="errorMessage"
        ></FeedbackMessage>
        <ConfirmationDialog
          v-if="approved == true"
          :record="user"
          action="approve"
          recordType="data request of user"
          @proceed="proceedApprove"
          @cancel="cancelApprove"
        ></ConfirmationDialog>
      </div>
    </div>
    <h2>Request: {{ requestId }}</h2>
    <div class="row">
      <div class="col-12">
        <div class="mb-3"><i class="bi bi-person-fill"></i> {{ user }}</div>
        <h3>Overview of requested data</h3>
        <table class="table">
          <tr v-for="(table, index) in decodedRequestData" :key="table.table">
            <th class="align-text-top" style="width: 20rem">
              {{ table.table }}
            </th>
            <td class="align-text-top">
              <button
                class="btn btn-sm btn-primary p-1 ps-2 pe-2"
                v-if="uncollapsedVariables.includes(index)"
                @click="collapseVariable(index)"
              >
                <i class="bi bi-chevron-up"></i>
              </button>
              <button
                class="btn btn-sm btn-primary p-1 ps-2 pe-2"
                v-else
                @click="uncollapseVariable(index)"
              >
                <i class="bi bi-chevron-down"></i>
              </button>
            </td>
            <td>
              <ul
                :class="`variables-${index}`"
                v-if="uncollapsedVariables.includes(index)"
              >
                <li
                  v-for="variable in table.variables.split(',')"
                  :key="variable"
                >
                  {{ variable }}
                </li>
              </ul>
              <span v-else></span>
            </td>
          </tr>
        </table>
        <button class="btn btn-success" @click="askIfSure">
          <i class="bi bi-check-lg"></i> Approve
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { approveRequest } from "@/api/api";
import ConfirmationDialog from "@/components/ConfirmationDialog.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import { defineComponent, onMounted, Ref, ref } from "vue";
import { useRoute } from "vue-router";

export default defineComponent({
  name: "Request",
  components: {
    ConfirmationDialog,
    FeedbackMessage,
  },
  setup() {
    const route = useRoute();
    const requestId: Ref<string> = ref("");
    const user: Ref<string> = ref("");
    const requestData: Ref<string> = ref("");

    onMounted(() => {
      requestId.value = route.params.requestId as string;
      user.value = route.params.user as string;
      requestData.value = route.params.requestData as string;
    });
    return {
      requestId,
      user,
      requestData,
    };
  },
  data(): {
    uncollapsedVariables: number[];
    approved: boolean;
    successMessage: string;
    errorMessage: string;
  } {
    return {
      uncollapsedVariables: [],
      approved: false,
      successMessage: "",
      errorMessage: "",
    };
  },
  computed: {
    decodedRequestData() {
      const tables = atob(this.requestData).split(";");
      let data = [];
      let variableCount = 0;
      if (tables[0] !== "") {
        tables.forEach((table) => {
          const tableName = table.split("|")[0];
          const variables = table.split("|")[1];
          variableCount += variables.split(",").length;
          data.push({ table: tableName, variables: variables });
        });
      }
      return data;
    },
  },
  methods: {
    uncollapseVariable(index: number) {
      this.uncollapsedVariables.push(index);
    },
    collapseVariable(index: number) {
      var i = this.uncollapsedVariables.indexOf(index);
      if (i !== -1) {
        this.uncollapsedVariables.splice(i, 1);
      }
    },
    cancelApprove() {
      this.approved = false;
    },
    proceedApprove() {
      approveRequest(this.user, this.requestId, this.decodedRequestData)
        .then((response) => {
          this.successMessage = `Successfully approved request [${this.requestId}] for user [${this.user}]. Requested variables are now availble in project [${this.requestId}]`;
        })
        .catch((error) => {
          this.errorMessage = `Approval failed, because: ${error}`;
        });
    },
    askIfSure() {
      this.approved = true;
    },
  },
});
</script>
