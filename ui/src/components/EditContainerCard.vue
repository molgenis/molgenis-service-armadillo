<template>
  <div class="card">
    <h5 class="card-header">
      <span class="row">
        <span class="col-2">
          {{ container.name }}
        </span>
        <span class="col-8">
          <ContainerTypeLogo :name="container.name" :template="template" />
        </span>
        <span class="col">
          <OnlineStatus class="float-end" :status="status" />
        </span>
      </span>
    </h5>
    <div class="card-body">
      <form>
        <div class="mb-3" v-for="(value, key) in containerToEdit">
          <div v-if="key === 'updateSchedule'" class="ms-4 row">
            <div v-if="container['autoUpdate'] === true" class="col-lg-4">
              <label class="fw-bold">Frequency</label>
              <div class="form-check">
                <input
                  class="form-check-input"
                  type="radio"
                  name="frequencyRadio"
                  value="daily"
                  v-model="updateFrequency"
                  checked
                />
                <label class="form-check-label" for="frequencyRadio">
                  Daily
                </label>
              </div>
              <div class="form-check">
                <input
                  class="form-check-input"
                  type="radio"
                  name="frequencyRadio"
                  v-model="updateFrequency"
                  value="weekly"
                />
                <label class="form-check-label" for="frequencyRadio">
                  Weekly
                </label>
              </div>
              <div v-if="updateFrequency === 'weekly'">
                <label class="fw-bold">Day</label>
                <Dropdown :options="daysOfTheWeek" @update="updateUpdateDay" />
              </div>
              <div>
                <label class="fw-bold">Time</label>
                <div>
                  <input
                    class="time-picker p-1"
                    type="time"
                    v-model="updateTime"
                  />
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="!dontShow.includes(key)">
            <span v-if="getDataType(value) === 'boolean'">
              <input
                class="form-check-input"
                type="checkbox"
                value=""
                v-model="containerToEdit[key]"
              />&nbsp;
            </span>
            <label for="containerName" class="form-label fw-bold">{{
              toCapitalizedWords(key)
            }}</label>
            <div v-if="frozen.includes(key)">{{ value }}</div>
            <input
              v-else-if="
                getDataType(value) === 'string' ||
                getDataType(value) === 'number'
              "
              :type="getDataType(value) === 'number' ? 'number' : 'text'"
              class="form-control container-input"
              :value="value"
              v-model="containerToEdit[key]"
            />
            <div v-else-if="getDataType(value) === 'array'">
              <BadgeList
                v-if="value.length > 0"
                :item-array="value"
                :can-edit="true"
              />
              <span v-else>-</span>
              <div class="row mt-1">
                <div class="col-1">
                  <button
                    class="btn btn-link"
                    @click.prevent="openItemToEdit(key)"
                  >
                    <i class="bi bi-plus-circle"></i>
                  </button>
                </div>
                <div
                  class="col-lg-4"
                  v-if="isArrayEditOpen && itemToEdit === key"
                >
                  <div class="input-group">
                    <input
                      type="text"
                      class="form-control"
                      v-model="valueToAdd"
                    />
                    <button class="btn btn-success" type="button">
                      <i
                        class="bi bi-check-lg"
                        @click="addValueToArray(key, valueToAdd)"
                      ></i>
                    </button>
                    <button class="btn btn-danger" type="button">
                      <i
                        class="bi bi-x-lg"
                        @click="isArrayEditOpen = false"
                      ></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div v-else-if="key === 'options'">
              <!-- Add (non-template-specific) options to docker container -->

              <span>
                <input
                  type="text"
                  class="form-control container-input font-monospace"
                  v-model="rawOptions"
                />
              </span>
              <div class="form-text mb-1">
                Docker container options that are not type-specific (unlike
                datashield options). Specify in JSON format.
              </div>
              <DataShieldOptions
                v-show="template === 'ds'"
                class="mt-4"
                :options="value"
              />
            </div>
          </div>
        </div>
      </form>
      <button class="btn btn-danger" @click="$emit('cancel-edit')">
        <i class="bi bi-x-lg"></i> Cancel
      </button>
      <button class="btn btn-primary" @click.prevent="onSave">
        <i class="bi bi-floppy-fill"></i> Save
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { getDataType, toCapitalizedWords } from "@/helpers/utils";
import { getDsDescriptions } from "@/helpers/dsInfo";
import Badge from "@/components/Badge.vue";
import BadgeList from "@/components/BadgeList.vue";
import { Container, ContainerStartStatus } from "@/types/api";
import OnlineStatus from "@/components/OnlineStatus.vue";
import ContainerTypeLogo from "@/components/ContainerTypeLogo.vue";
import Dropdown from "@/components/Dropdown.vue";
import DataShieldOptions from "@/components/DataShieldOptions.vue";

export default defineComponent({
  name: "EditContainerCard",
  props: {
    container: { type: Object as PropType<String, any>, required: true },
    template: { type: String, required: true },
    status: { type: Object as PropType<ContainerStartStatus>, required: true },
  },
  components: {
    DataShieldOptions,
    Dropdown,
    ContainerTypeLogo,
    OnlineStatus,
    BadgeList,
    Badge,
  },
  emits: ["cancel-edit", "save-changes", "throw-error"],
  methods: {
    getDataType,
    toCapitalizedWords,
    getFilteredOptions() {
      let filtered = {};
      const descriptions = getDsDescriptions();
      Object.keys(descriptions).forEach((option) => {
        if (!Object.keys(descriptions).includes(option)) {
          filtered[option] = options[option];
        }
      });
      return filtered;
    },
    updateUpdateDay(event: Event) {
      this.updateDay = event.toString();
    },
    openItemToEdit(key: string) {
      this.isArrayEditOpen = true;
      if (this.itemToEdit === "") {
        this.itemToEdit = key;
      } else {
        this.resetAddValue();
        this.itemToEdit = key;
      }
    },
    addValueToArray(array) {
      this.containerToEdit[array].push(this.valueToAdd);
      this.resetAddValue();
    },
    resetAddValue() {
      this.valueToAdd = "";
      this.itemToEdit = "";
    },
    onSave() {
      try {
        const raw = JSON.parse(this.rawOptions);
        this.containerToEdit["options"] = Object.assign(
          this.containerToEdit.options,
          raw
        );
        this.$emit("save-changes", this.containerToEdit);
      } catch (e) {
        this.$emit(
          "throw-error",
          `${e}. Options object [ ${this.rawOptions} ] doesn't adhere to json format required for container options.`
        );
      }
    },
  },
  watch: {
    updateFrequency() {
      this.containerToEdit.updateSchedule.frequency = this.updateFrequency;
    },
    updateDay() {
      this.containerToEdit.updateSchedule.day = this.updateDay;
      console.log(this.updateDay, this.containerToEdit.updateSchedule);
    },
    updateTime() {
      this.containerToEdit.updateSchedule.time = this.updateTime;
    },
  },
  data(): {
    valueToAdd: string;
    dontShow: Array<string>;
    frozen: Array<string>;
    itemsToEdit: string;
    icons: Object<string, string>;
    containerToEdit: Container;
    isArrayEditOpen: Boolean;
    updateDay: string;
    updateFrequency: string;
    daysOfTheWeek: Array<string>;
  } {
    return {
      valueToAdd: "",
      dontShow: [
        "imageSize",
        "installDate",
        "lastImageId",
        "container",
        "creationDate",
        "versionId",
      ],
      frozen: ["name"],
      itemToEdit: "",
      icons: {
        port: "",
        image: "",
      },
      containerToEdit: this.container,
      isArrayEditOpen: false,
      updateFrequency: this.container.updateSchedule.frequency,
      updateDay: this.container.updateSchedule.day,
      updateTime: this.container.updateSchedule.time,
      rawOptions: JSON.stringify(this.getFilteredOptions()),
      daysOfTheWeek: [
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday",
      ],
    };
  },
});
</script>

<style :scoped>
.container-input {
  width: 50%;
}

.time-picker {
  border-radius: 6px;
  border-width: 1px;
  border-color: #dee2e6;
}

.time-picker:focus {
  border-color: #86b7fe;
  outline: 0;
  box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.25);
}
</style>
