<template>
  <div class="row p-0">
    <div class="col-4"><i class="bi bi-funnel-fill"></i> Show all</div>
    <div class="col-8">
      <div class="form-check form-switch ps-4">
        <input
          class="form-check-input"
          type="checkbox"
          role="switch"
          @change="$emit('switched', showOnlyErrors)"
          v-model="showOnlyErrors"
        />
        <label class="form-check-label">Only show errors</label>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "ShowSwitch",
  emits: ["switched"],
  data() {
    return {
      showOnlyErrors: {
        type: Boolean,
        default: false,
      },
    };
  },
});
</script>
