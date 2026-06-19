<template>
  <div class="card mt-2 mb-2">
    <h5 class="card-header"><i class="bi bi-database-fill"></i> Storage</h5>
    <div class="card-body">
      <div class="row">
        <div class="col">
          <DiskSpace
            v-if="freeDiskSpace && totalDiskSpace"
            :used="diskspace.used"
            :percentage="diskspace.percentage"
            :total="diskspace.total"
            class="m-2"
          />
        </div>
      </div>
      <div class="row">
        <div class="col-lg-6 mx-auto">
          Available downloads on server:
          <button
            class="btn btn-danger btn-sm float-end"
            :disabled="appList.length < 2"
            @click="$emit('triggerDelete', appToDelete)"
          >
            <i class="bi bi-trash-fill"></i>
          </button>
        </div>
      </div>
      <div class="row" style="width: 100%">
        <div class="col-lg-6 mx-auto">
          <div class="card bg-light mt-2 ms-2">
            <div class="card-body">
              <div class="form-check" v-for="app in filteredApps" :key="app">
                <input
                  class="form-check-input"
                  type="radio"
                  name="appRadio"
                  :value="app"
                  v-model="appToDelete"
                  :disabled="
                    getVersionFromJar(app) === currentVersion
                  "
                />
                <label class="form-check-label">
                  {{ app }}
                  <span
                    v-if="getVersionFromJar(app) === currentVersion"
                    class="badge text-bg-info"
                    ><i class="bi bi-patch-exclamation-fill"></i> Currently
                    running</span
                  >
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { StringArray } from "@/types/types";
import { defineComponent, PropType } from "vue";
import { convertBytes, getVersionFromJar } from "@/helpers/utils";
import { DiskSpaceType } from "@/types/api";
import DiskSpace from "./DiskSpace.vue";

export default defineComponent({
  name: "Storage",
  components: { DiskSpace },
  emits: ["triggerDelete"],
  props: {
    appList: { type: Array as PropType<StringArray>, required: true },
    freeDiskSpace: { type: Number, required: false },
    totalDiskSpace: { type: Number, required: false },
    currentVersion: { type: String, required: true },
  },
  data() {
    return {
      appToDelete: "",
    };
  },
  computed: {
    filteredApps(): StringArray {
      return this.appList.filter((app) => app !== 'armadillo.jar')
    },
    diskspace(): DiskSpaceType {
      if (this.totalDiskSpace && this.freeDiskSpace) {
        const usedSpace = this.totalDiskSpace - this.freeDiskSpace;
        return {
          total: convertBytes(this.totalDiskSpace),
          free: convertBytes(this.freeDiskSpace),
          used: convertBytes(usedSpace),
          percentage: (usedSpace * 100) / this.totalDiskSpace,
        };
      } else {
        return { total: "", free: "", used: "", percentage: 0 };
      }
    },
  },
  methods: {
    getVersionFromJar,
  },
});
</script>
