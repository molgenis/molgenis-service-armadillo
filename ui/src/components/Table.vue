<template>
  <table class="table">
    <thead>
      <tr>
        <th scope="col" v-for="(value, key) in data[0]">{{ key }}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="item in data">
        <!-- <th scope="row">1</th> -->
        <td v-for="value in item">
          {{ value }}
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
};
</script>
