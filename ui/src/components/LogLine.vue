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
      {{ content.serverTime }}: <span class="fw-bold">{{ content.type }}</span>
      <span v-if="content.principal"> [{{ content.principal }}]</span>
      <button class="btn" @click="toggleCollapsed">
        <i class="bi bi-caret-down" v-if="collapsed"></i>
        <i class="bi bi-caret-up" v-else></i>
      </button>
    </div>
    <div class="card-body" :class="collapsed ? 'd-none' : 'd-inline'">
      <p class="m-0">
        <i class="bi bi-clock"></i> {{ content.serverTime }}
        <span v-if="content.principal">({{ content.timestamp }})</span>
      </p>
      <p class="m-0" v-if="content.principal">
        <i class="bi bi-person-fill"></i> {{ content.principal }}
      </p>
      <p class="m-0" v-if="content.logger">
        <i class="bi bi-file-text"></i> {{ content.logger }} [<span
          class="fst-italic"
          >{{ content.status }}</span
        >]
      </p>
      <p clas s="m-0" v-if="content.event">
        <i class="bi bi-calendar-event"></i> {{ content.event }}
      </p>
      <div class="m-0" v-if="content.data">
        <h5>Info</h5>
        <p v-for="(value, key) in content.data" class="m-0">
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
  name: "LogLine",
  props: {
    logLine: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      collapsed: true,
    };
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
      return this.content.type.includes("FAILURE")
        ? "failure"
        : this.content.type.includes("SUCCESS")
        ? "success"
        : "";
    },
    content() {
      const firstSplit = this.logLine.split(" [");
      const serverTime = firstSplit[0];
      const secondSplit = firstSplit[1].split("] ");
      const info = secondSplit[0];
      const thirdSplit = this.logLine.split("  ");
      const status = thirdSplit[0].split("]")[1].replace(" ", "");
      const fourthSplit = thirdSplit[1].split(" - ");
      try {
        const logger = fourthSplit[0];
        const fifthSplit = fourthSplit[1].split(" [");
        const event = fifthSplit[0];
        const sixthSplit = fifthSplit[1].split(", principal=");
        const timestamp = new Date(sixthSplit[0].replace("timestamp=", ""));
        const seventhSplit = sixthSplit[1].split(", type=");
        const principal = seventhSplit[0];
        const eighthSplit = seventhSplit[1].split(", data=");
        const type = eighthSplit[0];
        const data = fourthSplit[1].split(", data=")[1];
        return {
          serverTime: serverTime,
          info: info,
          status: status,
          logger: logger,
          event: event,
          timestamp: timestamp.toUTCString(),
          principal: principal,
          type: type,
          data: this.formatData(data),
        };
      } catch {
        const type = fourthSplit[0];
        const message = fourthSplit[1];
        return {
          serverTime: serverTime,
          info: info,
          status: status,
          message: message,
          type: type,
        };
      }
    },
  },
};
</script>
