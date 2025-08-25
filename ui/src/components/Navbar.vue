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
        Armadillo portal <small class="text-secondary">{{ version }}</small>
      </a>
      <form class="align-self-start mt-2">
        <span>
          <router-link :to="{ name: 'projects' }">
            <button class="btn btn-dark">
              <i class="bi bi-clipboard-data"></i> Projects
            </button>
          </router-link>
        </span>
        <span>
          <router-link :to="{ name: 'users' }">
            <button class="btn btn-dark">
              <i class="bi bi-people-fill"></i> Users
            </button>
          </router-link>
        </span>
        <span>
          <router-link :to="{ name: 'workspaces' }">
            <button class="btn btn-dark">
              <i class="bi bi-person-workspace"></i> Workspaces
            </button>
          </router-link>
        </span>
        <span>
          <router-link :to="{ name: 'profiles' }">
            <button class="btn btn-dark">
              <i class="bi bi-shield-shaded"></i> Profiles
            </button>
          </router-link>
        </span>
        <span>
          <router-link :to="{ name: 'insight' }">
            <button class="btn btn-dark">
              <i class="bi bi-brilliance"></i> Insight
            </button>
          </router-link>
        </span>
      </form>
      <form class="d-flex mt-1">
        <span>
          <a
            type="button"
            class="btn btn-dark"
            href="https://molgenis.github.io/molgenis-service-armadillo/"
            target="_blank"
            ><i class="bi bi-book"></i> Docs</a
          >
        </span>
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
import { defineComponent } from "vue";

export default defineComponent({
  name: "Navbar",
  props: {
    version: String,
    username: String,
    showLogin: Boolean,
  },
  emits: ["logout"],
});
</script>
