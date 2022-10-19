<template>
  <div class="row">
    <div class="col">
      <Navbar :username="principal.name"/>
      <div class="container">
        <div class="row mt-2">
          <div class="col">
            <Tabs
                :menu="tabs"
                :icons="tabIcons"
                :activeTab="activeTab"
                v-on:activeTabChange="setActiveTab"
            >
              <!-- can't we use RouterView here? -->
              <TabContent
                  v-for="item in tabs"
                  :menuItem="item"
                  :menuIndex="tabs.indexOf(item)"
                  :isActive="activeTab === tabs.indexOf(item)"
              />
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
import {onMounted, Ref, ref} from "vue";
import {getPrincipal} from "./api/api";
import {Principal} from "@/types/api";

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
      tabs: ["Projects", "Users", "Profiles"],
      tabIcons: ["clipboard2-data", "people-fill", "shield-shaded"],
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
