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
        <ConfirmationDialog
          v-if="isDeleteWorkspaceDirectoryTriggered"
          :record="`${selectedUser}`"
          action="delete"
          recordType="user workspace directory:"
          @proceed="deleteUserWorkspaceDirectory"
          @cancel="clearIsDeleteUserWorkspaceDirectoryTriggered"
        ></ConfirmationDialog>
      </div>
    </div>
    <div class="row">
      <div class="col-12">
        <button
          class="btn btn-danger"
          :disabled="!selectedUser || selectedUser === 'All workspaces'"
          @click="setDeleteUserWorkspaceDirectory"
        >
          <i class="bi bi-trash-fill"></i> Delete directory
        </button>
        <div class="row mt-1 border border-1">
          <!-- Loading spinner -->
          <LoadingSpinner v-if="loading" class="pt-3 mt-3"></LoadingSpinner>
          <div class="col-6" v-else>
            <ListGroup
              v-on:selectItem="setWorkspaces($event)"
              ref="workspaceComponent"
              :listContent="Object.keys(sortedWorkspaces)"
              rowIcon="person-fill"
              rowIconAlt="person-fill"
              :altIconCondition="showSelectedUser"
              :preselectedItem="selectedUser"
              :selectionColor="selectedUser ? 'secondary' : 'primary'"
            ></ListGroup>
          </div>
          <div class="col-6" :style="{}" ref="workspaceDetails">
            <div v-if="selectedUser">
              <DataPreviewTable
                :data="filteredWorkspaces[selectedUser]"
                :sortedHeaders="filteredHeaders"
                :nRows="2"
                :sortColumns="['user', 'name', 'size', 'lastModified']"
              >
                <template #extraHeader>
                  <th>
                    <button
                      type="button"
                      class="btn btn-danger btn-sm bg-danger"
                      @click.prevent="setIsDeleteTriggered"
                    >
                      <i class="bi bi-trash-fill"></i>
                    </button>
                  </th>
                </template>
                <template #extraColumn="columnProps">
                  <!-- Add buttons for editing/deleting users  -->
                  <th scope="row">
                    <input
                      v-if="selectedUser === 'All workspaces'"
                      class="form-check-input"
                      v-model="
                        userWorkspaces[
                          getIndexOfAllWorkspaces(
                            columnProps.item['user'],
                            columnProps.item['name']
                          )
                        ].checked
                      "
                      type="checkbox"
                    />
                    <input
                      v-else
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
import Alert from "@/components/Alert.vue";
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
import {
  getWorkspaceDetails,
  deleteUserWorkspace,
  deleteWorkspaceDirectory,
} from "@/api/api";
import { processErrorMessages } from "@/helpers/errorProcessing";
import { FormattedWorkspaces, StringArray, Workspaces } from "@/types/types";
import { Workspace } from "@/types/api";

export default defineComponent({
  name: "Workspaces",
  components: {
    ConfirmationDialog,
    FeedbackMessage,
    ListGroup,
    LoadingSpinner,
    DataPreviewTable,
    Alert,
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
    const getAllWorkspaces = () => {
      return Object.entries(workspaces.value).flatMap(
        ([userKey, workspaceArray]: [string, any]) =>
          workspaceArray.map((workspace: any) => ({
            user: userKey,
            name: workspace.name,
            size: workspace.size,
            lastModified: workspace.lastModified,
          }))
      );
    };
    const loadWorkspaces = async () => {
      workspaces.value = await getWorkspaceDetails().catch((error: string) => {
        errorMessage.value = processErrorMessages(error, "workspaces", router);
        return {};
      });
      workspaces.value["All workspaces"] = getAllWorkspaces();
    };
    return {
      route,
      router,
      errorMessage,
      previewParam,
      selectedUser,
      workspaces,
      loadWorkspaces,
      getAllWorkspaces,
    };
  },
  data() {
    return {
      loading: false,
      isDeleteTriggered: false,
      isDeleteWorkspaceDirectoryTriggered: false,
      userWorkspaces: [] as Workspace[],
      successMessage: "",
      deleteErrorMessages: [] as StringArray,
      deleteSuccessMessages: [] as StringArray,
    };
  },
  methods: {
    clearIsDeleteUserWorkspaceDirectoryTriggered() {
      this.isDeleteWorkspaceDirectoryTriggered = false;
    },
    setDeleteUserWorkspaceDirectory() {
      this.isDeleteWorkspaceDirectoryTriggered = true;
    },
    deleteUserWorkspaceDirectory() {
      deleteWorkspaceDirectory(this.selectedUser)
        .then(() => {
          this.successMessage = `Succesfully deleted workspace for user: [${this.selectedUser}]`;
          this.loadWorkspaces();
          this.selectedUser = "";
        })
        .catch((error) => {
          this.errorMessage = `Failed to delete workspace for user :[${this.selectedUser}], because: ${error}`;
        })
        .finally(() => {
          this.clearIsDeleteUserWorkspaceDirectoryTriggered();
        });
    },
    changeUser(user: string) {
      this.selectedUser = user;
      this.setWorkspaces(user);
    },
    getUserNameFromFolder() {
      return this.selectedUser.replace("user-", "");
    },
    getIndexOfWorkspace(selectedWorkspaceName: string) {
      return this.userWorkspaces.findIndex((workspace) => {
        return workspace.name === selectedWorkspaceName;
      });
    },
    getIndexOfAllWorkspaces(user: string, selectedWorkspaceName: string) {
      return this.userWorkspaces.findIndex((workspace) => {
        return (
          workspace.name === selectedWorkspaceName && workspace.user === user
        );
      });
    },
    setWorkspaces(user: string) {
      this.userWorkspaces = this.workspaces[user].map((ws: Workspace) => {
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
      await deleteUserWorkspace(this.getUserNameFromFolder(), workspaceName)
        .then(() => {
          const successMessage = `[${workspaceName}] for user [${this.selectedUser}]`;
          this.deleteSuccessMessages.push(successMessage);
        })
        .catch((error) => {
          const errorMessage = `[${workspaceName}] for user [${this.selectedUser}] because ${error}`;
          this.deleteErrorMessages.push(errorMessage);
        });
    },
    async deleteSelectedWorkspaces() {
      for (const workspace of this.workspacesToDelete) {
        await this.deleteWorkspace(workspace);
      }
      this.collectDeleteMessages();
      this.loadWorkspaces();
      this.setWorkspaces(this.selectedUser);
    },
    showSelectedUser() {},
    collectDeleteMessages() {
      var errorCollection = "";
      if (this.deleteSuccessMessages.length > 0) {
        const workspaceLabel =
          this.deleteSuccessMessages.length > 1 ? "workspaces" : "workspace";
        errorCollection += `Successfully deleted ${workspaceLabel} `;
        errorCollection += this.deleteSuccessMessages.join("; ");
      }
      if (this.deleteErrorMessages.length > 0) {
        const workspaceLabel =
          this.deleteErrorMessages.length > 1 ? "workspaces" : "workspace";
        errorCollection += `Could not delete ${workspaceLabel} `;
        errorCollection += this.deleteErrorMessages.join("; ");
        this.errorMessage = errorCollection;
      } else {
        this.successMessage = errorCollection;
      }
      this.deleteSuccessMessages = [];
      this.deleteErrorMessages = [];
    },
    addUserAsColumn() {
      const workspacesWithUser = this.workspaces;
      for (const user in workspacesWithUser) {
        workspacesWithUser[user] = workspacesWithUser[user].map(
          (item: Workspace) => ({
            user: user,
            ...item,
          })
        );
      }
      return workspacesWithUser;
    },
  },
  computed: {
    workspacesToDelete() {
      return this.userWorkspaces
        .filter((ws) => ws.checked)
        .map((ws) => ws.name);
    },
    formattedWorkspaces() {
      const workspacesWithUser = this.addUserAsColumn();
      return Object.entries(workspacesWithUser).reduce(
        (result: FormattedWorkspaces, [userId, workspaces]) => {
          result[userId] = (workspaces as Workspace[]).map(
            (workspace: Workspace) => ({
              user: workspace.user,
              name: workspace.name,
              size: convertBytes(workspace.size),
              lastModified: new Date(workspace.lastModified),
            })
          );
          return result;
        },
        {}
      );
    },
    sortedWorkspaces() {
      return Object.fromEntries(
        Object.entries(this.formattedWorkspaces).sort(([keyA], [keyB]) =>
          keyA.localeCompare(keyB)
        )
      );
    },
    filteredWorkspaces() {
      if (this.selectedUser === "All workspaces") {
        return this.formattedWorkspaces;
      } else {
        return Object.fromEntries(
          Object.entries(this.formattedWorkspaces).map(
            ([userId, workspaces]) => [
              userId,
              workspaces.map(({ user, ...rest }) => rest), // Exclude `user`
            ]
          )
        );
      }
    },
    filteredHeaders() {
      if (this.selectedUser === "All workspaces") {
        return ["user", "name", "size", "lastModified"];
      } else {
        return ["name", "size", "lastModified"];
      }
    },
  },
});
</script>
