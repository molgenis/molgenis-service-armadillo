import { shallowMount, VueWrapper } from "@vue/test-utils";
import ProjectsExplorer from "@/views/ProjectsExplorer.vue";
import { createRouter, createWebHistory } from "vue-router";
import * as _api from "@/api/api";
import { ProjectsExplorerData } from "@/types/types";

const api = _api as any;

jest.mock("@/api/api");

describe("ProjectsExplorer", () => {
    const mock_routes = [
        {
            path: "/",
            redirect: "/item_a"
        },
        {
            path: "/item_a",
            component: {
                template: "Welcome to item a",
            },
        },
        {
            path: "/item_b",
            component: {
                template: "Welcome to item b",
            },
        },
        {
            path: "/item_c",
            component: {
                template: "Welcome to item c",
            },
        },
    ];
    const router = createRouter({
        history: createWebHistory(),
        routes: mock_routes,
    });
    let wrapper: VueWrapper<any>;

    beforeEach(function() {
        const mockRouter = {
            push: jest.fn(),
        };

        wrapper = shallowMount(ProjectsExplorer, {
            global: {
                plugins: [router],
                mocks: {
                    $router: mockRouter,
                },
            },
        });

    });


})