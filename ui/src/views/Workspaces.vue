<template>
  <div>
    <div class="row">
      <div class="col">
        <!-- Error messages will appear here -->
        <FeedbackMessage
          :successMessage="successMessage"
          :errorMessage="errorMessage"
        ></FeedbackMessage>
        <ConfirmationDialog
          v-if="isDeleteTriggered"
          :record="`${workspacesToDelete.join(', ')}`"
          action="delete"
          recordType="workspaces"
          :additionalMessage="`for user [${selectedUser}]`"
          @proceed="deleteSelectedWorkspaces"
          @cancel="clearIsDeleteTriggered"
        ></ConfirmationDialog>
      </div>
    </div>
    <div class="row">
      <div class="col-12">
        <h2 class="mt-3">Workspaces</h2>
        {{ selectedUser }}
        <button
          type="button"
          class="btn btn-danger bg-danger"
          @click.prevent="deleteAllWorkspaces"
        >
          <i class="bi bi-trash-fill"></i>
        </button>
        <div class="row mt-1 border border-1">
          <!-- Loading spinner -->
          <LoadingSpinner v-if="loading" class="pt-3 mt-3"></LoadingSpinner>
          <div class="col-6" v-else>
            <ListGroup
              v-on:selectItem="setWorkspaces($event)"
              ref="workspaceComponent"
              :listContent="Object.keys(workspaces)"
              rowIcon="person-fill"
              rowIconAlt="person-fill"
              :altIconCondition="showSelectedUser"
              :preselectedItem="selectedUser"
              :selectionColor="selectedUser ? 'secondary' : 'primary'"
            ></ListGroup>
          </div>
          <div class="col-6" :style="{}" ref="workspaceDetails">
            <div v-if="selectedUser">
              <button
                type="button"
                class="btn btn-danger bg-danger"
                @click.prevent="setIsDeleteTriggered"
              >
                <i class="bi bi-trash-fill"></i>
              </button>
              <DataPreviewTable
                :data="formattedWorkspaces[selectedUser]"
                :sortedHeaders="['name', 'size', 'lastModified']"
                :nRows="2"
              >
                <template #extraHeader>
                  <th></th>
                </template>
                <template #extraColumn="columnProps">
                  <!-- Add buttons for editing/deleting users  -->
                  <th scope="row">
                    <input
                      class="form-check-input"
                      v-model="
                        userWorkspaces[
                          getIndexOfWorkspace(columnProps.item['name'])
                        ].checked
                      "
                      type="checkbox"
                    />
                  </th>
                </template>
              </DataPreviewTable>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import ConfirmationDialog from "@/components/ConfirmationDialog.vue";
import ListGroup from "@/components/ListGroup.vue";
import LoadingSpinner from "@/components/LoadingSpinner.vue";
import FeedbackMessage from "@/components/FeedbackMessage.vue";
import {} from "@/api/api";
import { convertBytes } from "@/helpers/utils";
import {
  defineComponent,
  onMounted,
  Ref,
  ref,
  watch,
  useTemplateRef,
} from "vue";
import { useRoute, useRouter } from "vue-router";
import DataPreviewTable from "@/components/DataPreviewTable.vue";
import { getWorkspaceDetails, deleteUserWorkspace } from "@/api/api";
import { processErrorMessages } from "@/helpers/errorProcessing";
import { FormattedWorkspaces, StringArray, Workspace, Workspaces } from "@/types/types";

export default defineComponent({
  name: "WorkspaceExplorer",
  components: {
    ConfirmationDialog,
    FeedbackMessage,
    ListGroup,
    LoadingSpinner,
    DataPreviewTable,
  },
  setup(_props, { emit }) {
    const selectedUser: Ref = ref("");
    const workspaceComponent = useTemplateRef("workspaceComponent");
    const errorMessage: Ref<string> = ref("");
    const router = useRouter();
    const route = useRoute();
    const previewParam = ref();
    const workspaces: Ref<Workspaces> = ref({});
    onMounted(() => {
      watch(
        () => workspaceComponent.value?.selectedItem,
        (newVal) => {
          if (newVal != undefined && newVal !== "") {
            selectedUser.value = newVal;
          }
        }
      );
      loadWorkspaces();
    });
    const loadWorkspaces = async () => {
      workspaces.value = await getWorkspaceDetails().catch((error: string) => {
        errorMessage.value = processErrorMessages(error, "workspaces", router);
        console.log(getWorkspaceDetails)
        return {};
      });
      const combinedWorkspaces = Object.entries(workspaces.value)
    .flatMap(([userKey, workspaceArray]: [string, any]) =>
      workspaceArray.map((workspace: any) => ({
        user: userKey,
        name: workspace.name,
        size: workspace.size,
        lastModified: workspace.lastModified,
      }))
    );
    workspaces.value["All workspaces"] = combinedWorkspaces;
    };
    return {
      route,
      router,
      errorMessage,
      previewParam,
      selectedUser,
      workspaces,
      loadWorkspaces,
    };
  },
  data() {
    return {
      loading: false,
      isDeleteTriggered: false,
      userWorkspaces: [] as Workspace[],
      successMessage: "",
      deleteErrorMessages: [] as StringArray,
      deleteSuccessMessages: [] as StringArray,
    };
  },
  methods: {
    getIndexOfWorkspace(selectedWorkspaceName: string) {
      return this.userWorkspaces.findIndex((workspace) => {
        return workspace.name === selectedWorkspaceName;
      });
    },
    setWorkspaces(user: string) {
      this.userWorkspaces = this.workspaces[user].map((ws) => {
        ws["checked"] = false;
        return ws;
      });
    },
    setIsDeleteTriggered() {
      this.isDeleteTriggered = true;
    },
    clearIsDeleteTriggered() {
      this.isDeleteTriggered = false;
    },
    async deleteWorkspace(workspaceName: string) {
      await deleteUserWorkspace(
        this.selectedUser.replace("user-", ""),
        workspaceName
      ).then(() => {
        const successMessage =  `[${workspaceName}] for user [${this.selectedUser}]`
        this.deleteSuccessMessages.push(successMessage);
        })
        .catch((error) => {
        const errorMessage = `[${workspaceName}] for user [${this.selectedUser}] because ${error}`
        this.deleteErrorMessages.push(errorMessage);
      });
    },
    deleteAllWorkspaces() {
      const userWorkspaces = this.workspaces[this.selectedUser];
      userWorkspaces.forEach((workspace) => {
        this.deleteWorkspace(workspace.name);
      });
    },
    async deleteSelectedWorkspaces() {
      for (const workspace of this.workspacesToDelete) {
        await this.deleteWorkspace(workspace);
      }      
      this.collectDeleteMessages()
      this.loadWorkspaces()
      this.setWorkspaces(this.selectedUser)
    },
    showSelectedUser() {},
    collectDeleteMessages() {
      var errorCollection = ""
      if(this.deleteSuccessMessages.length > 0) {
        console.log("success if", this.deleteSuccessMessages)
        const workspaceLabel = this.deleteSuccessMessages.length > 1 ? "workspaces" : "workspace";
        errorCollection += `Successfully deleted ${workspaceLabel} ` 
        errorCollection += this.deleteSuccessMessages.join("; ")
      }
      if(this.deleteErrorMessages.length > 0) {
        const workspaceLabel = this.deleteErrorMessages.length > 1 ? "workspaces" : "workspace";
        errorCollection += `Could not delete ${workspaceLabel} ` 
        errorCollection += this.deleteErrorMessages.join("; ")
        this.errorMessage = errorCollection
      } else {
        this.successMessage = errorCollection
      }
      this.deleteSuccessMessages = []
      this.deleteErrorMessages = []
    }
  },
  computed: {
    workspacesToDelete() {
      return this.userWorkspaces
        .filter((ws) => ws.checked)
        .map((ws) => ws.name);
    },
    formattedWorkspaces() {
      return Object.entries(this.workspaces).reduce(
        (result: FormattedWorkspaces, [userId, workspaces]) => {
          result[userId] = workspaces.map((workspace: Workspace) => ({
            name: workspace.name,
            size: convertBytes(workspace.size),
            lastModified: new Date(workspace.lastModified),
          }));
          return result;
        },
        {}
      );
    },
  },
});
</script>
