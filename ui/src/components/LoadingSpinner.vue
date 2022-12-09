<template>
  <div class="loading-spinner p-0 m-0">
    <img
      class="molgenis p-0 mx-auto d-block"
      :style="molgenisStyle"
      src="/molgenis-logo.png"
    />
    <img
      class="p-0 mx-auto d-block spinner-logo"
      :style="armadilloStyle"
      src="/armadillo-logo-loading.png"
    />
  </div>
</template>

<script lang="ts">
export default {
  name: "LoadingSpinner",
  props: {
    imageWidth: {
      type: String,
      default: "20",
    },
  },
  computed: {
    width() {
      return Number(this.imageWidth);
    },
    armadilloStyle() {
      return {
        width: `${this.width + this.width / 5}px`,
      };
    },
    molgenisStyle() {
      return {
        width: `${this.width - this.width * 0.2}px`,
        "margin-bottom": `-${this.width}px`,
      };
    },
  },
};
</script>

<style lang="css" scoped>
.molgenis {
  opacity: 0.7;
}
.spinner-logo {
  opacity: 0.7;
  animation: rotation 2s infinite linear;
}
.rotate {
  animation: rotation 2s;
}
@keyframes rotation {
  from {
    transform: rotate(359deg);
  }
  to {
    transform: rotate(0deg);
  }
}
.linear {
  animation-timing-function: linear;
}
.infinite {
  animation-iteration-count: infinite;
}
</style>