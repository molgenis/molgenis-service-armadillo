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
        <span
          v-for="(item, index) in menu"
          class="nav-item"
          :key="index"
          v-if="username && !showLogin"
        >
          <router-link :to="{ name: item.toLowerCase() }">
            <button class="btn btn-dark">
              <i
                class="bi bi-chevron-double-right"
                v-show="selectedPage == item.toLowerCase()"
              />
              <i :class="`bi bi-${icons[index]}`"></i>
              <span
                :class="
                  selectedPage == item.toLowerCase()
                    ? 'text-decoration-underline'
                    : ''
                "
                >{{ item }}</span
              >
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
import { StringArray } from "@/types/types";
import { defineComponent, PropType } from "vue";

export default defineComponent({
  name: "Navbar",
  props: {
    version: String,
    username: String,
    showLogin: Boolean,
    menu: { type: Array as PropType<StringArray>, required: true },
    icons: { type: Array as PropType<StringArray>, required: true },
  },
  emits: ["logout"],
  methods: {
    isSelectedPage(page: string) {
      return page.toLowerCase() === this.selectedPage;
    },
  },
  computed: {
    selectedPage() {
      return this.$route.fullPath.split("/")[1];
    },
  },
});
</script>
