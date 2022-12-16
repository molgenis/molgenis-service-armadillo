<template>
  <nav class="navbar navbar-dark bg-dark pt-1">
    <div class="container-fluid">
      <a class="navbar-brand align-middle" href="#">
        <img
            src="/armadillo-logo.png"
            alt="molgenis"
            width="35"
            class="d-inline-block me-2"
        />
        Armadillo portal (<a href="/swagger-ui/index.html">api</a>)
      </a>
      <form class="d-flex mt-1">
        <span class="navbar-text p-2" v-show="username"
        ><i class="bi bi-person-fill"></i> {{ username }}
        </span>
        <span>
          <button
              type="button"
              class="btn btn-primary"
              @click="$emit('logout')"
              v-if="username"
          >
            Log out
          </button>
          <router-link to="/login" v-else-if="showLogin">
            <button type="button" class="btn btn-primary">Log in</button>
          </router-link>
        </span>
      </form>
    </div>
  </nav>
</template>

<script lang="ts">
import {defineComponent} from "vue";

export default defineComponent({
  name: "Navbar",
  props: {
    username: String,
    showLogin: Boolean,
  },
  emits: ["logout"],
});
</script>
