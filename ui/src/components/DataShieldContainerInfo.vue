<template>
  <div>
    <h5>DataSHIELD</h5>
    <div class="row">
      <div class="col mt-2">
        <b>Package whitelist</b>
        <div>
          <BadgeList :itemArray="packageWhitelist" />
          <span v-if="packageWhitelist.length === 0">-</span>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col mt-2">
        <b>Function blacklist</b>
        <div>
          <BadgeList :itemArray="functionBlacklist" />
          <span v-if="functionBlacklist.length === 0">-</span>
        </div>
      </div>
    </div>
    <table class="table">
      <thead>
        <tr>
          <th scope="col">Option</th>
          <th scope="col">Value</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <th scope="row">nfilter.tab</th>
          <td>{{ getOption("nfilter.tab") }}</td>
        </tr>
        <tr>
          <th scope="row">nfilter.subset</th>
          <td>{{ getOption("nfilter.subset") }}</td>
        </tr>
        <tr>
          <th scope="row">nfilter.glm</th>
          <td>{{ getOption("nfilter.glm") }}</td>
        </tr>
        <tr>
          <th scope="row">nfilter.string</th>
          <td>{{ getOption("nfilter.string") }}</td>
        </tr>
        <tr>
          <th scope="row">nfilter.stringShort</th>
          <td>{{ getOption("nfilter.stringShort") }}</td>
        </tr>
        <tr>
          <th scope="row">nfilter.kNN</th>
          <td>{{ getOption("nfilter.kNN") }}</td>
        </tr>
        <tr>
          <th scope="row">nfilter.levels.density</th>
          <td>{{ getOption("nfilter.levels.density") }}</td>
        </tr>
        <tr>
          <th scope="row">nfilter.levels.max</th>
          <td>{{ getOption("nfilter.levels.max") }}</td>
        </tr>
        <tr>
          <th scope="row">nfilter.noise</th>
          <td>{{ getOption("nfilter.noise") }}</td>
        </tr>
        <tr>
          <th scope="row">datashield.privacyControlLevel</th>
          <td>{{ getOption("datashield.privacyControlLevel") }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import BadgeList from "@/components/BadgeList.vue";

export default defineComponent({
  name: "DataShieldContainerInfo",
  components: { BadgeList },
  props: {
    packageWhitelist: { type: Array, required: true },
    functionBlacklist: { type: Array, required: true },
    options: { type: Object, required: true },
  },
  methods: {
    getOption(optionId) {
      return optionId in this.options ? this.options[optionId] : "";
    },
  },
});
</script>

<style scoped></style>
