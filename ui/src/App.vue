<template>
  <div class="row">
    <div class="col">
      <Navbar :username="this.principal.name"/>
      <div class="container">
        <div class="row mt-1">
          <div class="col">
            <Tabs
              :menu="this.tabs"
              :icons="this.tabIcons"
              :activeTab="this.activeTab"
              v-on:activeTabChange="setActiveTab"
            >
              <TabContent
                v-for="item in this.tabs"
                :menuItem="item"
                :menuIndex="this.tabs.indexOf(item)"
                :isActive="this.activeTab == this.tabs.indexOf(item)"
              >
                <div v-if="item == 'Users'">
                  <Users></Users>
                </div>
                <div v-else>To do: {{ item }}</div>
              </TabContent>
            </Tabs>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Navbar from "./components/Navbar.vue";
import Tabs from "./components/Tabs.vue";
import TabContent from "./components/TabContent.vue";
import Users from "./views/Users.vue";
import { onMounted, ref } from "vue";
import { getPrincipal } from "./api/api";

export default {
  name: "ArmadilloPortal",
  components: {
    Navbar,
    Tabs,
    TabContent,
    Users,
  },
  setup() {
    const principal = ref([]);
    onMounted(() => {
      loadPrincipal();
    });
    const loadPrincipal = async () => {
      principal.value = await getPrincipal();
    };
    return {
      principal,
      loadPrincipal,
    };
  },
  mounted(){
    console.log(this.principal);
  },
  data() {
    return {
      activeTab: 0,
      tabs: ["Users", "Projects", "Profiles", "Monitoring"],
      tabIcons: ["people-fill", "folder-fill", "grid", "clipboard-data-fill"],
    };
  },
  methods: {
    setActiveTab(index) {
      console.log(this.principal.name)
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
