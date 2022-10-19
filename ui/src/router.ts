import { createWebHistory, createRouter } from "vue-router";
import Projects from "@/views/Projects.vue";
import ProjectsExplorer from "@/views/ProjectsExplorer.vue";
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
    name: "users",
    component: Users,
  },
  {
    path: "/projects",
    name: "projects",
    component: Projects,
  },
  {
    path: "/projects-explorer/:projectId",
    name: "projects-explorer",
    component: ProjectsExplorer,
  },
  {
    path: "/profiles",
    name: "profiles",
    component: Profiles,
  },
  {
    path: "/monitoring",
    name: "monitoring",
    component: Monitoring,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;