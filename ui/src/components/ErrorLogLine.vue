<template>
  <div class="card">
    <div class="card-header">
      <i
        class="bi bi-check-circle-fill text-success"
        v-if="messageType === 'success'"
      ></i>
      <i
        class="bi bi-x-circle-fill text-danger"
        v-else-if="messageType === 'failure'"
      ></i>
      <i class="bi bi-info-circle-fill text-info" v-else></i>
      {{ serverTime }}: <span class="fw-bold">{{ type }}</span>
      <span v-if="principal"> [{{ principal }}]</span>
      <button class="btn" @click="toggleCollapsed">
        <i class="bi bi-caret-down" v-if="collapsed"></i>
        <i class="bi bi-caret-up" v-else></i>
      </button>
    </div>
    <div class="card-body" :class="collapsed ? 'd-none' : 'd-inline'">
      <p class="m-0">
        <i class="bi bi-clock"></i> {{ serverTime }}
        <span v-if="principal">({{ timestamp }})</span>
      </p>
      <p class="m-0" v-if="principal">
        <i class="bi bi-person-fill"></i> {{ principal }}
      </p>
      <p class="m-0" v-if="logger">
        <i class="bi bi-file-text"></i> {{ logger }} [<span
          class="fst-italic"
          >{{ status }}</span
        >]
      </p>
      <p clas s="m-0" v-if="event">
        <i class="bi bi-calendar-event"></i> {{ event }}
      </p>
      <div class="m-0" v-if="data">
        <h5>Info</h5>
        <p v-for="(value, key) in data" class="m-0">
          <span class="fw-bold">{{ key }}:</span> {{ value }}
        </p>
      </div>
      <h5 class="mt-3">Raw log line</h5>
      <p class="font-monospace bg-light p-2 m-2 text-dark border rounded">
        {{ logLine }}
      </p>
    </div>
  </div>
</template>
<script lang="ts">
import { StringObject } from "@/types/types";

export default {
  name: "ErrorLogLine",
  props: {
    logLine: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      collapsed: true,
      serverTime: "",
      info: "",
      status: "",
      logger: "",
      event: "",
      timestamp: "",
      principal: "",
      type: "",
      message: "",
      data: {},
    };
  },
  mounted() {
    const firstSplit = this.logLine.split(" [");
    this.serverTime = firstSplit[0];
    if (firstSplit.length > 1) {
      const secondSplit = firstSplit[1].split("] ");
      this.info = secondSplit[0];
      const thirdSplit = this.logLine.split("  ");
      if (thirdSplit.length > 1) {
        this.status = thirdSplit[0].split("]")[1].replace(" ", "");
        const fourthSplit = thirdSplit[1].split(" - ");
        try {
          this.logger = fourthSplit[0];
          const fifthSplit = fourthSplit[1].split(" [");
          this.event = fifthSplit[0];
          const sixthSplit = fifthSplit[1].split(", principal=");
          this.timestamp = new Date(
            sixthSplit[0].replace("timestamp=", "")
          ).toUTCString();
          const seventhSplit = sixthSplit[1].split(", type=");
          this.principal = seventhSplit[0];
          const eighthSplit = seventhSplit[1].split(", data=");
          this.type = eighthSplit[0];
          this.data = this.formatData(fourthSplit[1].split(", data=")[1]);
        } catch {
          this.type = fourthSplit[0];
          this.message = fourthSplit[1];
        }
      }
    }
  },
  methods: {
    toggleCollapsed() {
      this.collapsed = !this.collapsed;
    },
    formatData(data: String) {
      const trimmedData = data.substring(1, data.length - 2);
      const result: StringObject = {};
      let index = 0;
      const length = trimmedData.length;

      while (index < length) {
        let eqIndex = trimmedData.indexOf("=", index);
        if (eqIndex === -1) break;

        const key = trimmedData.substring(index, eqIndex).trim();
        index = eqIndex + 1;

        // Determine value bounds
        let value = "";
        if (trimmedData[index] === "[") {
          // Handle bracketed value like [A, B]
          let bracketCount = 1;
          let end = index + 1;
          while (end < length && bracketCount > 0) {
            if (trimmedData[end] === "[") bracketCount++;
            if (trimmedData[end] === "]") bracketCount--;
            end++;
          }
          value = trimmedData.substring(index, end);
          index = end;
        } else {
          // Handle non-bracketed value, but may contain commas within descriptive text
          let end = index;
          let inText = false;
          while (end < length) {
            if (trimmedData[end] === "," && !inText) break;
            if (trimmedData[end] === "[") inText = true;
            if (trimmedData[end] === "]") inText = false;
            end++;
          }
          value = trimmedData.substring(index, end).trim();
          index = end;
        }

        result[key] = value;

        // Skip comma and whitespace
        if (trimmedData[index] === ",") index++;
        while (trimmedData[index] === " ") index++;
      }

      return result;
    },
  },
  computed: {
    messageType() {
      return this.type.includes("FAILURE")
        ? "failure"
        : this.type.includes("SUCCESS")
        ? "success"
        : "";
    },
  },
};
</script>
