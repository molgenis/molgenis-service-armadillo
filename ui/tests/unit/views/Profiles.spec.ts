import { shallowMount, VueWrapper } from "@vue/test-utils";
import Profiles from "@/views/Profiles.vue";
import { createRouter, createWebHistory } from "vue-router";
import * as _api from "@/api/api";
import { Profile } from "@/types/api"

const api = _api as any;

jest.mock("@/api/api")

describe("Profiles", () => {
    let testData: Profile[];

    let profileToAdd: Profile;
    let profileToEdit: Profile;

    const mock_routes = [
        {
            path: "/",
            redirect: "/item_a",
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
        routes: mock_routes
    })
    let wrapper: VueWrapper<any>;

    
}
)
