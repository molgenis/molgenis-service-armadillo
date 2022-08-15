<script setup>
// This starter template is using Vue 3 <script setup> SFCs
// Check out https://vuejs.org/api/sfc-script-setup.html#script-setup
import Navbar from "./components/Navbar.vue";
import Tabs from "./components/Tabs.vue";
import TabContent from "./components/TabContent.vue";
</script>

<template>
  <div class="row">
    <div class="col">
      <Navbar />
      <div class="container">
        <div class="row mt-1">
          <div class="col">
            <Tabs
              :menu="this.tabs"
              :activeTab="this.activeTab"
              v-on:activeTabChange="setActiveTab"
            >
              <TabContent
                v-for="item in this.tabs"
                :menuItem="item"
                :menuIndex="this.tabs.indexOf(item)"
                :isActive="this.activeTab == this.tabs.indexOf(item)"
              >
                {{ item }}
              </TabContent>
            </Tabs>
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
      activeTab: 0,
      tabs: ["Users", "Projects", "Profiles", "Monitoring"],
    };
  },
  methods: {
    setActiveTab(index) {
      this.activeTab = index;
    },
  },
};
</script>

<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
}

.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}

.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}
</style>
