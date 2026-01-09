<template>
  <div class="card">
    <h5 class="card-header">{{ container.name }}</h5>
    <div class="card-body">
      <form>
        <div class="mb-3" v-for="(value, key) in container">
          <label for="containerName" class="form-label">{{
            toCapitalizedWords(key)
          }}</label>
          <input
            v-if="typeof value === 'string'"
            type="text"
            class="form-control"
            :value="value"
          />
          {{ typeof value }}
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

export default defineComponent({
  name: "EditContainerCard",
  methods: { toCapitalizedWords },
  data() {
    return {
      dontShow: ["imageSize", "installDate"],
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
