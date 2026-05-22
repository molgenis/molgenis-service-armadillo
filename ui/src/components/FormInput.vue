<template>
  <div class="mb-3 row">
    <label class="col-sm-2 col-form-label">
      <i :class="`bi bi-${icon}`"></i> {{ label }}
    </label>
    <div class="col-sm-10">
      <div class="input-group">
        <input
          :type="type !== 'password' ? type : showSecret ? 'text' : 'password'"
          class="form-control"
          v-model="mappedValue"
          :placeholder="mappedValue"
          :disabled="!isEditMode"
        />
        <!-- toggle visibility in case of password here -->
        <button
          class="btn btn-info"
          v-if="type === 'password'"
          type="button"
          @click="toggleSecret"
        >
          <i class="bi bi-eye-slash-fill" v-if="showSecret"></i
          ><i class="bi bi-eye-fill" v-else></i>
        </button>
        <!-- copy here -->
        <button
          v-if="hasCopyButton"
          class="btn"
          :class="isCopied ? 'btn-success' : 'btn-secondary'"
          type="button"
          @click="copy"
        >
          <i class="bi bi-clipboard-check-fill" v-if="isCopied"></i
          ><i class="bi bi-clipboard-fill" v-else></i>
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "FormInput",
  components: {},
  props: {
    icon: String,
    label: { type: String, required: true },
    value: { type: String, required: true },
    hasCopyButton: Boolean,
    type: String,
    isEditMode: Boolean,
  },
  data(): { showSecret: boolean; isCopied: boolean; mappedValue: string } {
    return {
      showSecret: false,
      isCopied: false,
      mappedValue: this.value,
    };
  },
  methods: {
    copy() {
      navigator.clipboard
        .writeText(this.mappedValue)
        .then(() => {
          // otherwise this is the context of the then function we're in, rather than our data prop
          const self = this;
          this.isCopied = true;
          setTimeout(function () {
            self.isCopied = false;
          }, 1000);
        })
        .catch((err) => {
          console.error("Error copying to clipboard:", err);
        });
    },
    toggleSecret() {
      this.showSecret = !this.showSecret;
    },
  },
});
</script>
