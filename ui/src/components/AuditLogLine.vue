<template>
  <div class="card m-1">
    <div class="card-body">
      <p class="m-0">
        <i class="bi bi-clock"></i> Timestamp: {{ content.timestamp }}
      </p>
      <p class="m-0">
        <i class="bi bi-person-fill"></i> {{ content.principal }}
      </p>
      <p class="m-0">
        <span class="fw-bold">action: </span> {{ content.type }}
      </p>
      <div class="m-0">
        <p v-for="(value, key) in content.data" :key="key" class="m-0">
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
  computed: {
    content() {
      const splitOnPrincipal = this.logLine.split("\nprincipal: ");
      const timestamp = new Date(
        splitOnPrincipal[0].replace("timestamp: ", "")
      ).toUTCString();
      const splitOnType = splitOnPrincipal[1].split("\ntype: ");
      const principal = splitOnType[0];
      const splitOnData = splitOnType[1].split("\ndata: ");
      const type = splitOnData[0];
      const data = JSON.parse(splitOnData[1]);
      return {
        principal: principal,
        timestamp: timestamp,
        type: type,
        data: data,
      };
    },
  },
};
</script>
