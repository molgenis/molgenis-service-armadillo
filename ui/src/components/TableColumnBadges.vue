<template>
  <span>
    <span class="badge text-bg-secondary me-1" v-for="listItem in data">
      {{ listItem }}
    </span>
  </span>
</template>

<script>
export default {
  name: "TableColumnBadges",
  props: {
    data: Array,
  },
};
</script>
