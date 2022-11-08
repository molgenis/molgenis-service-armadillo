<template>
  <div>
    <div class="row">
      <div class="col">
        <div class="card mx-auto mt-3" style="max-width: 30rem">
          <div class="card-body">
            <h5 class="card-title">Login using:</h5>
            <div class="d-grid gap-2 ms-5 me-5">
              <button class="btn btn-primary" type="button" @click="redirect('/oauth2')">
                <i class="bi bi-building"></i> Institute account (oauth2)
              </button>
              <button
                class="btn btn-secondary"
                type="button"
                @click="getUrl('/basic-login')"
              >
                <i class="bi bi-person-fill"></i> Local account (basic-auth)
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import {get} from "@/api/api";

export default defineComponent({
  name: "Login",
  methods: {
    redirect(url: string) {
      window.location.href = url;
    },
    getUrl(url: string){
      get(url);
    }
  },
});
</script>
