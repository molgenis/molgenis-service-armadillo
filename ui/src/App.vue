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
            <Alert v-if="diskNearFull" type="warning" :dismissible="false">
              {{ diskSpaceMessage }}
            </Alert>
            <Alert v-if="isUnauthorised" type="warning" :dismissible="false">
              You are logged in, but you don't have permissions to access the Armadillo user interface. If you believe you should have these permisions, contact an administrator.
            </Alert>
            {{ errorMessage }}
            <Tabs v-if="username && !isUnauthorised" :menu="tabs" :icons="tabIcons" />
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
import Alert from "@/components/Alert.vue";
import { defineComponent, onMounted, ref, Ref } from "vue";
import { getPrincipal, getVersion, logout, getFreeDiskSpace, getPermissions } from "@/api/api";
import { useRouter } from "vue-router";
import { ApiError } from "@/helpers/errors";
import { diskSpaceBelowThreshold, convertBytes } from "@/helpers/utils";

export default defineComponent({
  name: "ArmadilloPortal",
  components: {
    Navbar,
    Tabs,
    Login,
    Alert,
  },
  setup() {
    const isAuthenticated: Ref<boolean> = ref(false);
    const isUnauthorised: Ref<boolean> = ref(false);
    const username: Ref<string> = ref("");
    const version: Ref<string> = ref("");
    const router = useRouter();
    const diskSpace: Ref<string> = ref("");

    onMounted(() => {
      loadUser();
      loadVersion();
      loadFreeDiskSpace();
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
          getPermissions().catch((error: ApiError) => {
            if (error.cause == 403) {
              isUnauthorised.value = true;
            }
          })
              
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
    const loadFreeDiskSpace = async () => {
      diskSpace.value = await getFreeDiskSpace();
    };
    return {
      username,
      isAuthenticated,
      isUnauthorised,
      version,
      loadUser,
      loadVersion,
      diskSpace,
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
  computed: {
    diskNearFull() {
      return diskSpaceBelowThreshold(this.diskSpace);
    },
    diskSpaceMessage() {
      return `Disk space low (${
        this.diskSpace === "" ? "" : convertBytes(this.diskSpace)
      } remaining). Saving workspaces may not be possible and users risk losing workspace data. Either allocate more space or remove saved workspaces.`;
    },
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
