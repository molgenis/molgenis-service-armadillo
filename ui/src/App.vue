<template>
  <div class="row">
    <div class="col">
      <Navbar :username="principal.name" />
      <div class="container">
        <div class="row mt-1">
          <div class="col">
            <Tabs
              :menu="tabs"
              :icons="tabIcons"
              :activeTab="activeTab"
              v-on:activeTabChange="setActiveTab"
            >
              <TabContent
                v-for="item in tabs"
                :menuItem="item"
                :menuIndex="tabs.indexOf(item)"
                :isActive="activeTab === tabs.indexOf(item)"
              >
                <div v-if="item === 'Users'">
                  <Users></Users>
                </div>
                <div v-else-if="item === 'Projects'">
                  <Projects></Projects>
                </div>
                <div v-else-if="item === 'Profiles'">
                  <Profiles></Profiles>
                </div>
                <div v-else-if="item === 'Monitoring'">
                  <Monitoring></Monitoring>
                </div>
              </TabContent>
            </Tabs>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Navbar from "./components/Navbar.vue";
import Tabs from "./components/Tabs.vue";
import TabContent from "./components/TabContent.vue";
import Projects from "./views/Projects.vue";
import Users from "./views/Users.vue";
import { onMounted, Ref, ref } from "vue";
import { getPrincipal } from "./api/api";
import { Principal } from "@/types/api";

export default {
  name: "ArmadilloPortal",
  components: {
    Navbar,
    Projects,
    Tabs,
    TabContent,
    Users,
  },
  setup() {
    const principal: Ref<Principal> = ref({
      authorities: [
        {
          authority: "",
        },
      ],
      details: null,
      authenticated: false,
      principal: null,
      credentials: null,
      name: "",
    } as Principal);
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
  data() {
    return {
      activeTab: 0,
      tabs: ["Users", "Projects", "Profiles", "Monitoring"],
      tabIcons: ["people-fill", "folder-fill", "grid", "clipboard-data-fill"],
    };
  },
  methods: {
    setActiveTab(index: number) {
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
