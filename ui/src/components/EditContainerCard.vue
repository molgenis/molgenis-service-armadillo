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
          <div v-if="key === 'updateSchedule'">
            <span v-if="container['autoUpdate'] === true">
              {{ value }}
            </span>
          </div>
          <div v-else-if="!dontShow.includes(key)">
            <span v-if="getValueType(value) === 'boolean'">
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
                getValueType(value) === 'string' ||
                getValueType(value) === 'number'
              "
              :type="getValueType(value) === 'number' ? 'number' : 'text'"
              class="form-control container-input"
              :value="value"
              v-model="containerToEdit[key]"
            />
            <div v-else-if="getValueType(value) === 'array'">
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
                    @click.prevent="toggleItemToEdit(key)"
                  >
                    <i class="bi bi-plus-circle"></i>
                  </button>
                </div>
                <div class="col-lg-4" v-if="itemToEdit === key">
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
                      <i class="bi bi-x-lg" @click="resetAddValue"></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div v-else>
              <<
              {{ key }} | {{ getValueType(value) }} |
              {{ value }}
              >>
            </div>
          </div>
        </div>
      </form>
      <button class="btn btn-danger" @click="$emit('cancel-edit')">
        <i class="bi bi-x-lg"></i> Cancel
      </button>
      <button class="btn btn-primary" @click="$emit('save-changes')">
        <i class="bi bi-floppy-fill"></i> Save
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { toCapitalizedWords } from "@/helpers/utils";
import Badge from "@/components/Badge.vue";
import BadgeList from "@/components/BadgeList.vue";
import { Container, ContainerStartStatus } from "@/types/api";
import OnlineStatus from "@/components/OnlineStatus.vue";
import ContainerTypeLogo from "@/components/ContainerTypeLogo.vue";
import containers from "@/views/Containers.vue";

export default defineComponent({
  name: "EditContainerCard",
  props: {
    container: { type: Object as PropType<String, any>, required: true },
    template: { type: String, required: true },
    status: { type: Object as PropType<ContainerStartStatus>, required: true },
  },
  components: { ContainerTypeLogo, OnlineStatus, BadgeList, Badge },
  emits: ["cancel-edit", "save-changes"],
  methods: {
    toCapitalizedWords,
    toggleItemToEdit(key: string) {
      if (this.itemToEdit === "") {
        this.itemToEdit = key;
      } else if (this.itemToEdit === key) {
        this.itemToEdit = "";
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
    getValueType(value) {
      let type = typeof value;
      if (
        type === "object" &&
        Object.prototype.toString.call(value) === "[object Array]"
      ) {
        return "array";
      } else {
        return type;
      }
    },
  },
  data(): {
    valueToAdd: string;
    dontShow: Array<string>;
    frozen: Array<string>;
    itemsToEdit: string;
    icons: Object<string, string>;
    containerToEdit: Container;
  } {
    return {
      valueToAdd: "",
      dontShow: [
        "imageSize",
        "installDate",
        "lastImageId",
        "container",
        "creationDate",
      ],
      frozen: ["name"],
      itemToEdit: "",
      icons: {
        port: "",
        image: "",
      },
      containerToEdit: this.container,
    };
  },
});
</script>

<style :scoped>
.container-input {
  width: 50%;
}
</style>
