<template>
  <div class="card m-1">
    <div class="card-body">
      <p class="m-0"><i class="bi bi-clock"></i> {{ timestamp }}</p>
      <p class="m-0"><i class="bi bi-person-fill"></i> {{ principal }}</p>
      <p class="m-0"><span class="fw-bold">action: </span> {{ type }}</p>
      <div class="m-0">
        <p v-for="(value, key) in data" :key="key" class="m-0">
          <span class="fw-bold m-0">{{ key }}: </span> {{ value }}
        </p>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
export default {
  name: "AuditLogLine",
  props: {
    logLine: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      principal: "",
      timestamp: "",
      type: "",
      data: "",
    };
  },
  mounted() {
    const splitOnPrincipal = this.logLine.split("\nprincipal: ");
    if (splitOnPrincipal?.length > 1) {
      this.timestamp = new Date(
        splitOnPrincipal[0].replace("timestamp: ", "")
      ).toUTCString();
      const splitOnType = splitOnPrincipal[1].split("\ntype: ");
      if (splitOnType?.length > 1) {
        this.principal = splitOnType[0];
        const splitOnData = splitOnType[1].split("\ndata: ");
        if (splitOnData?.length > 1) {
          this.type = splitOnData[0];
          this.data = JSON.parse(splitOnData[1]);
        }
      }
    }
  },
};
</script>
