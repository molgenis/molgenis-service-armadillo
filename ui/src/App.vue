<template>
  <div class="row">
    <div class="col">
      <Navbar
        :version="version"
        :username="username"
        @logout="logoutUser"
        :showLogin="false"
      />
      <div class="container">
        <div class="row mt-2">
          <div class="col" v-if="username">
            <Tabs v-if="username" :menu="tabs" :icons="tabIcons" />
          </div>
          <Login @loginEvent="reloadUser" v-else />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Navbar from "@/components/Navbar.vue";
import Tabs from "@/components/Tabs.vue";
import Login from "@/views/Login.vue";
import { defineComponent, onMounted, ref, Ref } from "vue";
import { getPrincipal, getVersion, logout } from "@/api/api";
import { useRouter } from "vue-router";
import { ApiError } from "@/helpers/errors";

export default defineComponent({
  name: "ArmadilloPortal",
  components: {
    Navbar,
    Tabs,
    Login,
  },
  setup() {
    const isAuthenticated: Ref<boolean> = ref(false);
    const username: Ref<string> = ref("");
    const version: Ref<string> = ref("");
    const router = useRouter();

    onMounted(() => {
      loadUser();
      loadVersion();
    });

    const loadUser = async () => {
      await getPrincipal()
        .then((principal) => {
          isAuthenticated.value = principal.authenticated;
          username.value =
            principal.principal &&
            principal.principal.attributes &&
            principal.principal.attributes.email
              ? principal.principal.attributes.email
              : principal.name;
        })
        .catch((error: ApiError) => {
          if (error.cause === 401) {
            router.push("/login");
          }
        });
    };

    const loadVersion = async () => {
      version.value = await getVersion();
    };

    return {
      username,
      isAuthenticated,
      version,
      loadUser,
      loadVersion,
    };
  },
  data() {
    return {
      loading: false,
      tabs: ["Projects", "Users", "Profiles", "Insight"],
      tabIcons: [
        "clipboard2-data",
        "people-fill",
        "shield-shaded",
        "brilliance",
      ],
    };
  },
  methods: {
    logoutUser() {
      logout().then(() => {
        this.username = "";
        this.reloadUser();
      });
    },
    reloadUser() {
      this.loadUser()
        .then(() => {
          if (!this.username) {
            this.$router.push("/login");
          }
        })
        .catch((error: ApiError) => {
          if (error.cause === 401) {
            this.$router.push("/login");
          }
        });
    },
  },
});
</script>
