<template>
  <div id="aaaapp">
    <button @click="showOverlay = true">Show Settings</button>
    <div id="overlay" v-if="showOverlay" @click="showOverlay = false">
      <div class="container">
        <div class="row">
          <div class="col">
            <slot></slot>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  data() {
    return {
      showOverlay: false,
    };
  },
};
</script>
<style scoped>
#overlay {
  width: 100%;
  height: 100%;
  position: fixed;
  overflow-y: scroll;
  z-index: 1;
  top: 0;
  left: 0;
  background-color: rgba(0, 0, 0, 0.9);
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
