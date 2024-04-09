import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";

import { modal } from "bootstrap/js/dist/modal.js";

import "bootstrap/scss/bootstrap.scss";
import "bootstrap-icons/font/bootstrap-icons.css";

createApp(App).use(modal).use(router).mount("#app");
