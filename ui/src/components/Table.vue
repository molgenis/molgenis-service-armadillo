<template>
  <table class="table">
    <thead>
      <tr>
        <th scope="col" v-for="(value, key) in data[0]">{{ toCapitalizedWords(key) }}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="item in data">
        <!-- <th scope="row">1</th> -->
        <td v-for="value in item">
          <span v-if="Array.isArray(value)">
            <slot name="arrayType" :data="value">
              {{ value }}
            </slot>
          </span>
          <span v-else-if="typeof value == 'boolean'">
            <slot name="boolType" :data="value">
              {{ value }}
            </slot>
          </span>
          <span v-else>{{ value }}</span>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script>
export default {
  name: "Table",
  props: {
    data: { String: Array },
  },
  methods: {
    toCapitalizedWords(name) {
      var words = name.match(/[A-Za-z][a-z]*/g) || [];

      return words.map(this.capitalize).join(" ");
    },

    capitalize(word) {
      return word.charAt(0).toUpperCase() + word.substring(1);
    },
  },
};
</script>
