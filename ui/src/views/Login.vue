<template>
  <div>
    <div class="row">
      <div class="col">
        <div class="card mx-auto mt-3" style="max-width: 30rem">
          <div class="card-body position-relative">
            <div class="d-flex align-items-center justify-content-center mb-3">
              <img src="/armadillo-logo.png" alt="armadillo" width="100" />
            </div>
            <h5 class="card-title text-center">Welcome to Armadillo</h5>
            <hr />
            <p class="ms-5">Login with:</p>
            <div class="d-grid gap-2 ms-5 me-5">
              <button
                class="btn btn-primary"
                type="button"
                @click="redirect('/oauth2')"
              >
                <i class="bi bi-building"></i> Institute account (oauth2)
              </button>
              <button
                class="btn btn-secondary"
                type="button"
                @click="toggleShowLogin"
              >
                <i class="bi bi-person-fill"></i> Local account (basic-auth)
              </button>
              <div v-show="showLogin">
                <hr />
                <!-- Error messages will appear here -->
                <FeedbackMessage
                  :successMessage="successMessage"
                  :errorMessage="errorMessage"
                ></FeedbackMessage>
                <p class="fw-bold">Login using local account:</p>
                <form>
                  <div class="input-group mb-3">
                    <span class="input-group-text" id="basic-user-addon">
                      <i class="bi bi-person-fill"></i>
                    </span>
                    <input
                      v-model="username"
                      type="text"
                      class="form-control"
                      placeholder="Username"
                      aria-label="Username"
                      aria-describedby="basic-user-addon1"
                    />
                  </div>
                  <div class="input-group mb-3">
                    <span class="input-group-text" id="basic-pwd-addon">
                      <i class="bi bi-lock-fill"></i>
                    </span>
                    <input
                      v-model="password"
                      type="password"
                      class="form-control"
                      placeholder="Password"
                      aria-label="Password"
                      aria-describedby="basic-pwd-addon1"
                    />
                  </div>
                  <div class="d-grid gap-2">
                    <button
                      type="button"
                      class="btn btn-primary mb-4"
                      @click="login"
                    >
                      Log in
                      <i class="bi bi-box-arrow-right"></i>
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { authenticate } from "@/api/api";
import FeedbackMessage from "@/components/FeedbackMessage.vue";

export default defineComponent({
  name: "Login",
  components: { FeedbackMessage },
  emits: ["loginEvent"],
  data() {
    return {
      showLogin: false,
      username: "",
      password: "",
      successMessage: "",
      errorMessage: "",
    };
  },
  methods: {
    toggleShowLogin() {
      this.showLogin = !this.showLogin;
    },
    redirect(url: string) {
      window.location.href = url;
    },
    login() {
      authenticate({ user: this.username, pwd: this.password })
        .then(() => {
          this.$router.push("/");
          this.$emit("loginEvent");
        })
        .catch((error: Error) => {
          this.errorMessage = error.message;
        });
    },
  },
});
</script>
