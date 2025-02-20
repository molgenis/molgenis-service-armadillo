import { shallowMount, VueWrapper } from "@vue/test-utils";
import Workspaces from "@/views/Workspaces.vue";
import {
    Workspaces as WorkspacesType
} from "@/types/types"
import { createRouter, createWebHistory } from "vue-router";
import * as _api from "@/api/api";

const api = _api as any;

jest.mock("@/api/api");

describe("Workspaces", () => {
    let testData: WorkspacesType;

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

        router.currentRoute.value.params = { projectId: "some-project" };

        testData = {
            "user-bofke.dijkstra__at__umcg.nl": [
              {
                "lastModified": "2025-02-06T13:02:49.282+01:00",
                "name": "cohort_1:my-workspace",
                "size": 2148
              },
              {
                "lastModified": "2025-02-06T13:02:49.283+01:00",
                "name": "cohort_2:my-workspace",
                "size": 1205
              },
              {
                "lastModified": "2025-02-11T16:18:27.283+01:00",
                "name": "armadillo:test_save",
                "size": 1246
              }
            ],
            "user-2e786317-6e8b-4769-83d6-cf74f0e2636a": [
              {
                "lastModified": "2024-09-27T14:37:56.681+02:00",
                "name": "cohort_1:my-workspace1",
                "size": 2148
              },
              {
                "lastModified": "2024-09-27T14:37:56.796+02:00",
                "name": "cohort_2:my-workspace1",
                "size": 1205
              },
              {
                "lastModified": "2024-12-10T12:33:48.478+01:00",
                "name": "armadillo:test_save",
                "size": 1246
              }
            ],
            "user-t.de.boer__at__umcg.nl": [
              {
                "lastModified": "2025-02-14T10:27:15.937+01:00",
                "name": "armadillo:test_save4",
                "size": 468811
              },
              {
                "lastModified": "2024-12-10T12:33:44.263+01:00",
                "name": "cohort_1:my-workspace",
                "size": 2148
              },
              {
                "lastModified": "2024-12-10T12:33:44.264+01:00",
                "name": "cohort_2:my-workspace",
                "size": 1205
              },
              {
                "lastModified": "2024-12-10T12:33:44.301+01:00",
                "name": "armadillo:Untitled",
                "size": 24476988
              },
              {
                "lastModified": "2024-12-10T12:33:48.457+01:00",
                "name": "armadillo:test_save1",
                "size": 1476710570
              },
              {
                "lastModified": "2024-12-10T12:33:48.478+01:00",
                "name": "armadillo:test_save",
                "size": 1246
              },
              {
                "lastModified": "2024-12-10T12:33:50.948+01:00",
                "name": "armadillo:test_save3",
                "size": 148094264
              }
            ]
          };

        api.getWorkspaceDetails.mockImplementationOnce(() => {
            return Promise.resolve(testData);
        });

        wrapper = shallowMount(Workspaces, {
            global: {
                plugins: [router],
                mocks: {
                    $router: mockRouter,
                },
            },
        });

    });

    test("clearIsDeleteUserWorkspaceDirectoryTriggered", () => {
        wrapper.vm.isDeleteWorkspaceDirectoryTriggered = true;
        wrapper.vm.clearIsDeleteUserWorkspaceDirectoryTriggered()
        expect(wrapper.vm.isDeleteWorkspaceDirectoryTriggered).toBe(false);})

});
