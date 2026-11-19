<template>
  <div>
    <div class="row">
      <div class="col">
        <FeedbackMessage
          :successMessage="successMessage"
          :errorMessage="errorMessage"
          :doClearSuccess="false"
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
    <h2>Request</h2>
    <div class="row">
      <div class="col-12" v-if="isValidData">
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
              <span v-else
                >{{ table.variables.split(",").length }} variables</span
              >
            </td>
          </tr>
        </table>
        <button
          class="btn btn-success"
          @click="askIfSure"
          :disabled="!isValidData"
        >
          <i class="bi bi-check-lg"></i> Approve
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { tableExists, approveRequest, getMetaData } from "@/api/api";
import ConfirmationDialog from "@/components/ConfirmationDialog.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import { StringArray, StringObject } from "@/types/types";
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
    const user: Ref<string> = ref("");
    const requestData: Ref<string> = ref("");
    const apiVersion: Ref<string> = ref("");

    onMounted(() => {
      user.value = route.params.user as string;
      requestData.value = route.params.requestData as string;
      apiVersion.value = route.params.apiVersion as string;
    });
    return {
      user,
      requestData,
      apiVersion,
    };
  },
  data(): {
    uncollapsedVariables: number[];
    approved: boolean;
    successMessage: string;
    errorMessage: string;
    allowedApiVersions: StringArray;
  } {
    return {
      uncollapsedVariables: [],
      approved: false,
      successMessage: "",
      errorMessage: "",
      allowedApiVersions: ["v1"],
    };
  },
  computed: {
    isValidData() {
      if (!this.apiVersion || !this.validateVersion()) {
        this.errorMessage = `Provided invalid api version: ${this.apiVersion}, please make sure it is one of the following: [${this.allowedApiVersions}] and your URL path is structured like: /#/r/{apiVersion}/{user}/{base64encoded requestdata}`;
        return false;
      } else if (!this.user) {
        this.errorMessage =
          "Request does not have a valid user, please make sure the URL path is structured like: /#/r/{apiVersion}/{user}/{base64-encoded requestdata}";
        return false;
      } else if (!this.validateUser()) {
        this.errorMessage = `Request invalid: user [${this.user}] is not a valid email address`;
        return false;
      } else if (!this.requestData) {
        this.errorMessage =
          "Request does not have a valid request data, please make sure the URL path is structured like: /#/r/{apiVersion}/{user}/{requestdata} and request data should be the base64 encoding of the variables in the following structure: project/folder/table1|var1,var2;project/folder/table2|var1";
        return false;
      } else if (!this.isBase64Decodable()) {
        this.errorMessage = "Cannot base64 decode provided requestData.";
        return false;
      } else {
        this.errorMessage = "";
        this.validateRequestData();
        return true;
      }
    },
    decodedRequestData() {
      let decoded: string;
      try {
        decoded = atob(this.requestData);
      } catch (error) {
        return [] as StringObject[];
      }
      const tables = decoded.split(";");
      const data = [] as StringObject[];
      if (tables[0] !== "") {
        tables.forEach((table) => {
          const tableName = table.split("|")[0];
          const variables = table.split("|")[1];
          data.push({ table: tableName, variables: variables });
        });
      }
      return data;
    },
  },
  methods: {
    tableExists,
    getMetaData,
    isBase64Decodable() {
      try {
        atob(this.requestData);
        return true;
      } catch (error) {
        return false;
      }
    },
    validateRequestData() {
      this.decodedRequestData.forEach((tb) => {
        const tbLink = tb["table"];
        const project = tbLink.split("/")[0];
        const object = tbLink.replace(project + "/", "") + ".parquet";
        const selectedVariables = tb["variables"].split(",");

        // Only look up the metadata once we know the table exists
        this.tableExists(project, object).then(
          () => {
            this.getMetaData(project, object)
              .then((metaData) => {
                const variables = Object.keys(metaData);
                selectedVariables.forEach((variable) => {
                  if (!variables.includes(variable)) {
                    this.errorMessage = `Request has invalid variable(s) for table [${tbLink}]: [${variable}] does not exist`;
                  }
                });
              })
              .catch(() => {
                this.errorMessage = `Request invalid: could not retrieve metadata for table [${tbLink}]`;
              });
          },
          () => {
            this.errorMessage = `Request invalid: table [${tbLink}] doesn't exist`;
          }
        );
      });
    },
    validateUser() {
      if (!this.user.includes("@")) {
        return false;
      }
      return true;
    },
    validateVersion() {
      // right now we only have version 1, if we have more versions in the future, we can address migration here
      return this.allowedApiVersions.includes(this.apiVersion);
    },
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
      approveRequest(this.user, this.decodedRequestData)
        .then((response) => {
          response.text().then((requestId) => {
            this.successMessage = `Successfully approved request [${requestId}] for user [${this.user}]. Requested variables are now available in project [${requestId}]`;
          });
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
