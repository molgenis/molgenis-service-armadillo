import { createRouter, createWebHashHistory } from "vue-router";
import Projects from "@/views/Projects.vue";
import ProjectsExplorer from "@/views/ProjectsExplorer.vue";
import Users from "@/views/Users.vue";
import Login from "@/views/Login.vue";
import Actuator from "@/views/Actuator.vue";
import Workspaces from "@/views/Workspaces.vue";
import RemoteFiles from "@/views/RemoteFiles.vue";
import Containers from "@/views/Containers.vue";

const routes = [
  {
    path: "/",
    redirect: "/ui",
  },
  {
    path: "/ui",
    redirect: "/ui/projects",
  },
  {
    path: "/ui/users",
    name: "users",
    component: Users,
  },
  {
    path: "/ui/workspaces",
    name: "workspaces",
    component: Workspaces,
  },
  {
    path: "/ui/projects",
    name: "projects",
    component: Projects,
  },
  {
    path: "/ui/login",
    name: "login",
    component: Login,
  },
  {
    path: "/ui/projects-explorer/:projectId/:folderId?/:fileId?",
    name: "projects-explorer",
    component: ProjectsExplorer,
  },
  // {
  //   path: "/profiles",
  //   name: "profiles",
  //   component: Profiles,
  // },
  {
    path: "/ui/containers",
    name: "containers",
    component: Containers,
  },
  {
    path: "/ui/metrics",
    name: "metrics",
    component: Actuator,
  },
  {
    path: "/ui/logs",
    name: "logs",
    component: RemoteFiles,
  },
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

export default router;
