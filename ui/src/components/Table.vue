<template>
  <table class="table">
    <thead>
      <tr>
        <slot name="extraHeader"></slot>
        <th scope="col" v-for="(value, key) in data[0]">
          {{ toCapitalizedWords(key) }}
        </th>
      </tr>
    </thead>
    <tbody>
      <slot name="extraRow"></slot>
      <template v-for="(item, index) in data">
        <tr v-if="index != this.indexToEdit">
          <slot name="extraColumn" :item="item"></slot>
          <td v-for="value in item">
            <span v-if="Array.isArray(value)">
              <slot name="arrayType" :data="value" :row="item">
                {{ value }}
              </slot>
            </span>
            <span v-else-if="typeof value == 'boolean'">
              <slot name="boolType" :data="value" :row="item">
                {{ value }}
              </slot>
            </span>
            <span v-else>{{ value }}</span>
          </td>
        </tr>
        <slot name="editRow" :row="item" v-else></slot>
      </template>
    </tbody>
  </table>
</template>

<script>
export default {
  name: "Table",
  props: {
    data: Array,
    idCol: String,
    indexToEdit: Number,
  },
  methods: {
    toCapitalizedWords(name) {
      const words = name.match(/[A-Za-z][a-z]*/g) || [];
      return words.map(this.capitalize).join(" ");
    },
    capitalize(word) {
      return word.charAt(0).toUpperCase() + word.substring(1);
    },
  },
};
</script>
