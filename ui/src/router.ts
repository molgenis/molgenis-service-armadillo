import { createRouter, createWebHashHistory } from "vue-router";
import Projects from "@/views/Projects.vue";
import ProjectsExplorer from "@/views/ProjectsExplorer.vue";
import Users from "@/views/Users.vue";
import Profiles from "@/views/Profiles.vue";
import Login from "@/views/Login.vue";
import Insight from "./views/Insight.vue";
import Workspaces from "./views/Workspaces.vue";

const routes = [
  {
    path: "/",
    redirect: "/projects",
  },
  {
    path: "/users",
    name: "users",
    component: Users,
  },
  {
    path: "/workspaces",
    name: "workspaces",
    component: Workspaces,
  },
  {
    path: "/projects",
    name: "projects",
    component: Projects,
  },
  {
    path: "/login",
    name: "login",
    component: Login,
  },
  {
    path: "/projects-explorer/:projectId/:folderId?/:fileId?",
    name: "projects-explorer",
    component: ProjectsExplorer,
  },
  {
    path: "/profiles",
    name: "profiles",
    component: Profiles,
  },
  {
    path: "/insight",
    name: "insight",
    component: Insight,
  },
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

export default router;
