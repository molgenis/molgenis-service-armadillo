<template>
  <div>
    <div class="row">
      <div class="col">
        <!-- Error messages will appear here -->
        <!--<FeedbackMessage
          :successMessage="successMessage"
          :errorMessage="errorMessage"
        ></FeedbackMessage>
        <ConfirmationDialog
          v-if="fileToDelete != ''"
          :record="`${folderToDeleteFrom}/${fileToDelete}`"
          action="delete"
          recordType="file"
          @proceed="proceedDelete"
          @cancel="clearRecordToDelete"
        ></ConfirmationDialog>-->
      </div>
    </div>
    <div class="row">
      <div class="col-12">
        <h2 class="mt-3">Workspaces</h2>
        {{ selectedUser }}
        <button
          type="button"
          class="btn btn-danger bg-danger"
          @click="deleteUserWorkspace"
        >
          <i class="bi bi-trash-fill"></i>
        </button>
        <div class="row mt-1 border border-1">
          <!-- Loading spinner -->
          <LoadingSpinner v-if="loading" class="pt-3 mt-3"></LoadingSpinner>
          <div class="col-6" v-else>
            <ListGroup
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
            <!--<DataPreviewTable
                :data="filePreview"
                :maxWidth="previewContainerWidth"
                :n-rows="fileInfo.dataSizeRows"
              ></DataPreviewTable>-->
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
import {} from "@/helpers/utils";
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
    onMounted(() => {
      console.log(workspaceComponent);
      watch(
        () => workspaceComponent.value?.selectedItem,
        (newVal) => {
          console.log(newVal);
          if (newVal != undefined) {
            emit("selectUser", newVal);
            selectedUser.value = newVal;
          }
        }
      );
    });
    return {
      route,
      router,
      errorMessage,
      previewParam,
      selectedUser,
    };
  },
  data() {
    return {
      workspaces: {
        "user-1bd26c22-4a9a-480a-80e7-6acbed34bc05": [
          {
            name: "cohort_2:test_workspace_2",
            size: 172681,
            lastModified: "2024-12-05T12:27:49.107+01:00",
          },
          {
            name: "cohort_1:test_workspace_2",
            size: 174014,
            lastModified: "2024-12-05T12:27:48.877+01:00",
          },
          {
            name: "cohort_2:test_workspace_1",
            size: 172681,
            lastModified: "2024-12-05T12:27:37.2+01:00",
          },
          {
            name: "cohort_2:test_workspace_3",
            size: 172681,
            lastModified: "2024-12-05T12:27:52.65+01:00",
          },
          {
            name: "cohort_1:test_workspace_3",
            size: 174014,
            lastModified: "2024-12-05T12:27:52.418+01:00",
          },
          {
            name: "cohort_1:test_workspace_1",
            size: 174014,
            lastModified: "2024-12-05T12:27:36.963+01:00",
          },
        ],
        "user-e6de84fa-d08d-43e4-9558-bb9fba1528b9": [
          {
            name: "cohort_2:test_workspace_2",
            size: 172681,
            lastModified: "2024-12-05T12:27:49.107+01:00",
          },
          {
            name: "cohort_1:test_workspace_2",
            size: 174014,
            lastModified: "2024-12-05T12:27:48.877+01:00",
          },
          {
            name: "cohort_2:test_workspace_1",
            size: 172681,
            lastModified: "2024-12-05T12:27:37.2+01:00",
          },
          {
            name: "cohort_2:test_workspace_3",
            size: 172681,
            lastModified: "2024-12-05T12:27:52.65+01:00",
          },
          {
            name: "cohort_1:test_workspace_3",
            size: 174014,
            lastModified: "2024-12-05T12:27:52.418+01:00",
          },
          {
            name: "cohort_1:test_workspace_1",
            size: 174014,
            lastModified: "2024-12-05T12:27:36.963+01:00",
          },
        ],
      },
      loading: false,
    };
  },
  methods: {
    deleteUserWorkspace() {},
    showSelectedUser() {},
  },
});
</script>
