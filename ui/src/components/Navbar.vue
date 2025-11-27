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
          v-for="(value, key, index) in menu"
          class="nav-item"
          :key="index"
          v-if="username && !showLogin"
        >
          <span v-if="typeof value === 'object'">
            <div class="dropdown nav-item dropdown-menu-dark">
              <button
                class="btn btn-dark dropdown-toggle"
                type="button"
                data-bs-toggle="dropdown"
                aria-expanded="false"
                @click="toggleDropdown(key)"
              >
                <i
                  class="bi bi-chevron-right"
                  v-show="
                    menu[key]
                      .map((item: string) => item.toLowerCase())
                      .includes(selectedPage)
                  "
                />&nbsp; <i :class="`bi bi-${icons[index]}`" />&nbsp;
                {{ key }}
              </button>
              <ul class="dropdown-menu" :id="'dropdown-' + key.toLowerCase()">
                <li v-for="dropdownItem in value">
                  <router-link
                    :to="{ name: dropdownItem.toLowerCase() }"
                    @click="toggleDropdown(key)"
                    class="text-decoration-none"
                  >
                    <a class="dropdown-item" href="#">
                      <i
                        class="bi bi-chevron-double-right"
                        v-show="selectedPage == dropdownItem.toLowerCase()"
                      />&nbsp;
                      {{ dropdownItem }}
                    </a>
                  </router-link>
                </li>
              </ul>
            </div>
          </span>
          <router-link
            :to="{ name: key.toLowerCase() }"
            @click="closeDropdowns"
            v-else
          >
            <button class="btn btn-dark">
              <i
                class="bi bi-chevron-double-right"
                v-show="selectedPage == key.toLowerCase()"
              />&nbsp; <i :class="`bi bi-${icons[index]}`" />&nbsp;
              <span
                :class="
                  selectedPage == key.toLowerCase()
                    ? 'text-decoration-underline'
                    : ''
                "
                >{{ key }}</span
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
          <router-link to="/ui/login" v-else-if="showLogin">
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
    menu: { type: Object, required: true },
    icons: { type: Array as PropType<StringArray>, required: true },
  },
  emits: ["logout"],
  methods: {
    isSelectedPage(page: string) {
      return page.toLowerCase() === this.selectedPage;
    },
    toggleDropdown(id: string) {
      const element = document.getElementById("dropdown-" + id.toLowerCase());
      element?.classList.contains("show")
        ? element?.classList.remove("show")
        : element?.classList.add("show");
    },
    closeDropdowns() {
      for (let element of document.getElementsByClassName("dropdown-menu")) {
        element.classList.remove("show");
      }
    },
  },
  computed: {
    selectedPage() {
      return this.$route.fullPath.split("/")[2];
    },
  },
});
</script>

<style scoped>
.dropdown {
  display: inline;
}
.dropdown-menu {
  left: 0;
}
</style>
