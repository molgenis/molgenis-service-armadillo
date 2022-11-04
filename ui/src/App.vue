<template>
  <div class="row">
    <div class="col">
      <Navbar :username="username" @logout="logoutUser" />
      <div class="container">
        <div class="row mt-2">
          <div class="col">
            <Tabs v-if="username !== ''" :menu="tabs" :icons="tabIcons" />
            <router-view v-else />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Navbar from "@/components/Navbar.vue";
import Tabs from "@/components/Tabs.vue";
import Projects from "@/views/Projects.vue";
import Users from "@/views/Users.vue";
import { onMounted, Ref, ref, defineComponent } from "vue";
import { getPrincipal, logout } from "@/api/api";
import { Principal } from "@/types/api";

export default defineComponent({
  name: "ArmadilloPortal",
  components: {
    Navbar,
    Projects,
    Tabs,
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
      loading: false,
      tabs: ["Projects", "Users", "Profiles"],
      tabIcons: ["clipboard2-data", "people-fill", "shield-shaded"],
    };
  },
  computed: {
    authenticated() {
      return this.principal.authenticated;
    },
    isOauthUser() {
      return (
        this.principal.principal &&
        this.principal.principal.attributes &&
        this.principal.principal.attributes.email
      );
    },
    username() {
      // disabled ts here bc only way to fix error is by copy pasting code of isOauthUser (which is called)
      return this.isOauthUser
        ? // @ts-ignore
          this.principal.principal.attributes.email
        : this.principal.name;
    },
  },
  watch: {
    authenticated(newValue) {
      if (!newValue) {
        this.$router.push("/login");
      }
    },
  },
  methods: {
    logoutUser() {
      logout().then(() => {
        this.reloadUser();
      });
    },
    reloadUser() {
      this.loadPrincipal()
        .then(() => {
          if (!this.username) {
            this.$router.push("/login");
          }
        })
        .catch((error) => {
          console.error(`Could not load projects: ${error}.`);
        });
    },
  },
});
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
