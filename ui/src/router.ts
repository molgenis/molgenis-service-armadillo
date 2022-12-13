import {createRouter, createWebHistory} from "vue-router";
import Projects from "@/views/Projects.vue";
import ProjectsExplorer from "@/views/ProjectsExplorer.vue";
import Users from "@/views/Users.vue";
import Profiles from "@/views/Profiles.vue";
import Login from "@/views/Login.vue";

const routes = [
    {
        path: "/",
        redirect: "/projects"
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
        path: "/login",
        name: "login",
        component: Login,
    }
];

const router = createRouter({
    history: createWebHistory("/ui/"),
    routes,
});

export default router;