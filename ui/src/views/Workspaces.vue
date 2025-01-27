<template>
  <div>
    <div class="row">
      <div class="col">
        <!-- Error messages will appear here -->
        <!--<FeedbackMessage
          :successMessage="successMessage"
          :errorMessage="errorMessage"
        ></FeedbackMessage>-->
        <ConfirmationDialog
          v-if="isDeleteTriggered"
          :record="`${workspacesToDelete.join(', ')}`"
          action="delete"
          recordType="workspaces"
          :additionalMessage="`for user [${selectedUser}]`"
          @proceed="() => {}"
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
                  <!-- Add buttons for editing/deleting users -->
                  <th scope="row">
                    <input
                      class="form-check-input"
                      @click="toggleWorkspacesToDelete(columnProps.item)"
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
import { FormattedWorkspaces, Workspace, Workspaces } from "@/types/types";

export default defineComponent({
  name: "WorkspaceExplorer",
  emits: ["selectUser"],
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
            emit("selectUser", newVal);
            selectedUser.value = newVal;
          }
        }
      );
      loadWorkspaces();
    });
    const loadWorkspaces = async () => {
      workspaces.value = await getWorkspaceDetails().catch((error: string) => {
        errorMessage.value = processErrorMessages(error, "workspaces", router);
        return {};
      });
    };
    return {
      route,
      router,
      errorMessage,
      previewParam,
      selectedUser,
      workspaces,
    };
  },
  data() {
    return {
      loading: false,
      isDeleteTriggered: false,
      userWorkspaces: [] as Workspace[],
    };
  },
  methods: {
    getIndexOfWorkspace(selectedWorkspaceName: string) {
      return this.userWorkspaces.findIndex((workspace) => {
        return workspace.name === selectedWorkspaceName;
      });
    },
    setWorkspaces(user: string) {
      this.resetWorkspacesToDelete();
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
    resetWorkspacesToDelete() {
      this.workspacesToDelete = [];
    },
    toggleWorkspacesToDelete(selectedWS: Workspace) {
      const selectedWorkspaceName = selectedWS.name;
      const index = this.getIndexOfWorkspace(selectedWorkspaceName);
      this.userWorkspaces[index]["checked"] = true;
    },
    deleteWorkspace(workspaceName: string) {
      deleteUserWorkspace(
        this.selectedUser.replace("user-", ""),
        workspaceName
      ).catch((error) => {
        this.errorMessage = error;
      });
    },
    deleteAllWorkspaces() {
      const userWorkspaces = this.workspaces[this.selectedUser];
      userWorkspaces.forEach((workspace) => {
        this.deleteWorkspace(workspace.name);
      });
    },
    showSelectedUser() {},
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
