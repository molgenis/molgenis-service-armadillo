<template>
  <span>
    <ErrorLogLine
      v-if="fileId === 'LOG_FILE'"
      :logLine="logLine"
      :class="{ 'text-primary': isMatchedLine }"
    />
    <AuditLogLine
      v-else
      :logLine="logLine"
      :class="{ 'text-primary': isMatchedLine }"
    />
  </span>
</template>
<script lang="ts">
import AuditLogLine from "./AuditLogLine.vue";
import ErrorLogLine from "./ErrorLogLine.vue";

export default {
  name: "LogLine",
  components: { ErrorLogLine, AuditLogLine },
  props: {
    fileId: {
      type: String,
      required: true,
    },
    logLine: {
      type: String,
      required: true,
    },
    isMatchedLine: {
      type: Boolean,
      required: true,
    },
  },
};
</script>
