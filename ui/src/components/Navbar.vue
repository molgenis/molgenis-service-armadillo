<template>
  <nav class="navbar navbar-dark bg-dark">
    <div class="container-fluid">
      <a class="navbar-brand" href="#">
        <img
          src="/logo_molgenis.png"
          alt="molgenis"
          width="24"
          class="d-inline-block align-text-top"
        />
        Armadillo portal
      </a>
      <form class="d-flex">
        <span class="navbar-text p-2"
          ><i class="bi bi-person-fill"></i> {{ username }}
        </span>
        <span class="">
          <button type="button" class="btn btn-primary">Log out</button>
        </span>
      </form>
    </div>
  </nav>
</template>

<script lang="ts">
export default {
  name: "Navbar",
  props: {
    username: String,
  },
};
</script>
