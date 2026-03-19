<template>
  <div class="card m-1">
    <div class="card-header fw-bold">
      <i :class="'bi bi-' + icon"></i> {{ getCapitalisedValue(item) }}
    </div>
    <div class="card-body">
      <div
        v-for="(metric, path, index) in metrics"
        :key="index"
        :data="metric"
        :name="path.toString()"
      >
        <div
          v-if="metric.name.startsWith(item) && metric._display"
          class="mb-3"
        >
          <p class="fw-bold mb-0">{{ metric.name }}</p>
          <p class="fst-italic text-sm-start fs-6 m-0">
            {{ metric.description }}
          </p>
          <p
            v-for="measurement in metric.measurements"
            :key="measurement.value"
            class="m-0"
          >
            <i
              class="bi bi-stopwatch me-2"
              v-if="
                isTimeUnit(
                  measurement.statistic,
                  metric.measurements,
                  metric.baseUnit
                )
              "
            ></i>
            <i
              class="bi bi-memory me-2"
              v-else-if="isMemory(metric.name, metric.description)"
            ></i>
            <i
              class="bi bi-files me-2"
              v-else-if="isFiles(metric.baseUnit)"
            ></i>
            <i
              class="bi bi-folder me-2"
              v-else-if="isDirectory(metric.baseUnit)"
            ></i>
            <span v-if="isPreviewValue(measurement.statistic)" class="me-2">
              {{ getCapitalisedValue(measurement.statistic) }}:</span
            >
            <span v-if="isFileSizeUnit(metric.baseUnit)">{{
              convertBytes(measurement.value)
            }}</span>
            <span v-else-if="isPercentage(metric.description)">{{
              toPercentage(measurement.value)
            }}</span>
            <span v-else class="me-1">{{ measurement.value }}</span>
            <span
              v-if="
                !isFileSizeUnit(metric.baseUnit) &&
                (isTimeUnit(
                  measurement.statistic,
                  metric.measurements,
                  metric.baseUnit
                ) ||
                  isOnlyOneMeasurement(metric.measurements))
              "
            >
              {{ metric.baseUnit }}
            </span>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { convertBytes } from "@/helpers/utils";

export default defineComponent({
  name: "MetricsCard",
  props: {
    item: {
      type: String,
      required: true,
    },
    metrics: {
      type: Object,
      required: true,
    },
    icon: {
      type: String,
      required: true,
    },
  },
  methods: {
    convertBytes,
    isTimeUnit(
      statistic: String,
      measurements: Array<Object>,
      baseUnit: String
    ) {
      const lowerStatistic = statistic.toLowerCase();
      return (
        lowerStatistic.includes("time") ||
        lowerStatistic.includes("duration") ||
        lowerStatistic.includes("max") ||
        (this.isOnlyOneMeasurement(measurements) &&
          (baseUnit === "seconds" || baseUnit === "ms" || baseUnit === "ns"))
      );
    },
    isFileSizeUnit(baseUnit: String) {
      return baseUnit == "bytes";
    },
    isOnlyOneMeasurement(measurements: Array<Object>) {
      return measurements.length === 1;
    },
    isPreviewValue(value: String) {
      return value !== "VALUE";
    },
    isFiles(baseUnit: String) {
      return baseUnit === "files";
    },
    isDirectory(baseUnit: String) {
      return baseUnit === "directories";
    },
    getCapitalisedValue(value: String) {
      const firstCharacter = value.slice(0, 1).toUpperCase();
      const otherCharacters = value
        .slice(1, value.length)
        .toLowerCase()
        .replace("_", " ");
      return firstCharacter + otherCharacters;
    },
    isMemory(name: String, description: String) {
      return name.includes("memory") || description?.includes("memory");
    },
    isPercentage(description: String) {
      return description?.includes("percent");
    },
    toPercentage(value: number) {
      return value * 100.0 + "%";
    },
  },
});
</script>
