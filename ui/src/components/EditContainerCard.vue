<template>
  <div class="card">
    <h5 class="card-header">{{ container.name }}</h5>
    <div class="card-body">
      <form>
        <div class="mb-3" v-for="(value, key) in container">
          <input
            class="form-check-input"
            type="checkbox"
            value=""
            v-if="getValueType(value) === 'boolean'"
          />&nbsp;
          <label for="containerName" class="form-label">{{
            toCapitalizedWords(key)
          }}</label>

          <input
            v-if="getValueType(value) === 'string'"
            type="text"
            class="form-control"
            :value="value"
          />
          <span v-else-if="getValueType(value) === 'array'">
            <BadgeList :item-array="value" />
          </span>

          {{ getValueType(value) }}
          {{ value }}
        </div>
      </form>
      <button class="btn btn-danger"><i class="bi bi-x-lg"></i> Cancel</button>
      <button class="btn btn-primary">
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

export default defineComponent({
  name: "EditContainerCard",
  components: { BadgeList, Badge },
  methods: {
    toCapitalizedWords,
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
  data() {
    return {
      dontShow: ["imageSize", "installDate", "lastImageId"],
      icons: {
        port: "",
        image: "",
      },
    };
  },
  props: {
    container: { type: Object as PropType<String, any>, required: true },
  },
});
</script>
