import { createWebHistory, createRouter } from "vue-router";
import Projects from "@/views/Projects.vue";
import Users from "@/views/Users.vue";
import Profiles from "@/views/Profiles.vue";
import Monitoring from "@/views/Monitoring.vue";

const routes = [
  {
    path: "/",
    redirect: "/users"
  },
  {
    path: "/users",
    name: "Users",
    component: Users,
  },
  {
    path: "/projects",
    name: "Projects",
    component: Projects,
  },
  {
    path: "/profiles",
    name: "Profiles",
    component: Profiles,
  },
  {
    path: "/monitoring",
    name: "Monitoring",
    component: Monitoring,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;