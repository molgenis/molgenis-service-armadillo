import { shallowMount, VueWrapper, flushPromises, mount } from "@vue/test-utils";
import Workspaces from "@/views/Workspaces.vue";
import {
    Workspaces as WorkspacesType
} from "@/types/types"
import { createRouter, createWebHistory } from "vue-router";
import { convertBytes } from "@/helpers/utils";
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

    const buildTestData = (): WorkspacesType => ({
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
    });

    beforeEach(async () => {
        jest.clearAllMocks();

        testData = buildTestData();

        api.getWorkspaceDetails.mockResolvedValue(testData);

        const mockRouter = {
            push: jest.fn(),
        };

        router.currentRoute.value.params = { projectId: "some-project" };

        wrapper = mount(Workspaces, {
            global: {
                plugins: [router],
                mocks: {
                    $router: mockRouter,
                },
            },
        });

        // let the initial loadWorkspaces() call resolve
        await flushPromises();
    });

    test("clearIsDeleteUserWorkspaceDirectoryTriggered", () => {
        wrapper.vm.isDeleteWorkspaceDirectoryTriggered = true;
        wrapper.vm.clearIsDeleteUserWorkspaceDirectoryTriggered()
        expect(wrapper.vm.isDeleteWorkspaceDirectoryTriggered).toBe(false);
    })

    describe("initial load", () => {
        test("fetches workspace details on mount", () => {
            expect(api.getWorkspaceDetails).toHaveBeenCalledTimes(1);
        });

        test("adds an 'All workspaces' entry derived from all users", () => {
            expect(wrapper.vm.workspaces["All workspaces"]).toBeDefined();
            const names = wrapper.vm.workspaces["All workspaces"].map(
                (ws: any) => ws.name
            );
            expect(names).toEqual(
                expect.arrayContaining([
                    "cohort_1:my-workspace",
                    "cohort_1:my-workspace1",
                    "armadillo:test_save4",
                ])
            );
        });

        test("sets errorMessage when loading workspaces fails", async () => {
            jest.clearAllMocks();
            api.getWorkspaceDetails.mockImplementationOnce(() =>
                Promise.reject("boom")
            );
            const failingWrapper = shallowMount(Workspaces, {
                global: {
                    plugins: [router],
                    mocks: { $router: { push: jest.fn() } },
                },
            });
            await flushPromises();
            expect(failingWrapper.vm.errorMessage).toBeTruthy();
        });
    });

    describe("setWorkspaces / index helpers", () => {
        test("setWorkspaces populates userWorkspaces with checked=false", () => {
            wrapper.vm.setWorkspaces("user-bofke.dijkstra__at__umcg.nl");
            expect(wrapper.vm.userWorkspaces).toHaveLength(3);
            wrapper.vm.userWorkspaces.forEach((ws: any) => {
                expect(ws.checked).toBe(false);
            });
        });

        test("getIndexOfWorkspace finds the correct index by name", () => {
            wrapper.vm.setWorkspaces("user-bofke.dijkstra__at__umcg.nl");
            const index = wrapper.vm.getIndexOfWorkspace("armadillo:test_save");
            expect(wrapper.vm.userWorkspaces[index].name).toBe(
                "armadillo:test_save"
            );
        });

        test("getIndexOfWorkspace returns -1 when workspace is not present", () => {
            wrapper.vm.setWorkspaces("user-bofke.dijkstra__at__umcg.nl");
            expect(wrapper.vm.getIndexOfWorkspace("does-not-exist")).toBe(-1);
        });

        test("getIndexOfAllWorkspaces matches on both user and name", () => {
            wrapper.vm.selectedUser = "All workspaces";
            wrapper.vm.setWorkspaces("All workspaces");
            const index = wrapper.vm.getIndexOfAllWorkspaces(
                "user-t.de.boer__at__umcg.nl",
                "armadillo:test_save4"
            );
            expect(index).toBeGreaterThanOrEqual(0);
            expect(wrapper.vm.userWorkspaces[index].user).toBe(
                "user-t.de.boer__at__umcg.nl"
            );
        });

        test("getUserNameFromWorkspace returns the owning user", () => {
            wrapper.vm.selectedUser = "All workspaces";
            wrapper.vm.setWorkspaces("All workspaces");
            expect(
                wrapper.vm.getUserNameFromWorkspace("armadillo:test_save4")
            ).toBe("user-t.de.boer__at__umcg.nl");
        });
    });

    describe("delete user workspace directory", () => {
        test("setDeleteUserWorkspaceDirectory sets the confirmation flag", () => {
            wrapper.vm.setDeleteUserWorkspaceDirectory();
            expect(wrapper.vm.isDeleteWorkspaceDirectoryTriggered).toBe(true);
        });

        test("on success, shows a success message, clears the selected user and reloads", async () => {
            api.deleteWorkspaceDirectory.mockResolvedValueOnce(undefined);
            wrapper.vm.selectedUser = "user-bofke.dijkstra__at__umcg.nl";
            wrapper.vm.isDeleteWorkspaceDirectoryTriggered = true;

            wrapper.vm.deleteUserWorkspaceDirectory();
            await flushPromises();

            expect(api.deleteWorkspaceDirectory).toHaveBeenCalledWith(
                "user-bofke.dijkstra__at__umcg.nl"
            );
            expect(wrapper.vm.successMessage).toContain(
                "user-bofke.dijkstra__at__umcg.nl"
            );
            expect(wrapper.vm.selectedUser).toBe("");
            expect(wrapper.vm.isDeleteWorkspaceDirectoryTriggered).toBe(false);
            // once on mount, once from the reload triggered by the delete
            expect(api.getWorkspaceDetails).toHaveBeenCalledTimes(2);
        });

        test("on failure, shows an error message and keeps the selected user", async () => {
            api.deleteWorkspaceDirectory.mockRejectedValueOnce("network error");
            wrapper.vm.selectedUser = "user-bofke.dijkstra__at__umcg.nl";

            wrapper.vm.deleteUserWorkspaceDirectory();
            await flushPromises();

            expect(wrapper.vm.errorMessage).toContain("network error");
            expect(wrapper.vm.selectedUser).toBe(
                "user-bofke.dijkstra__at__umcg.nl"
            );
        });
    });

    describe("single workspace deletion", () => {
        beforeEach(() => {
            wrapper.vm.selectedUser = "user-bofke.dijkstra__at__umcg.nl";
        });

        test("records a success message when deletion succeeds", async () => {
            api.deleteUserWorkspace.mockResolvedValueOnce(undefined);

            await wrapper.vm.deleteWorkspace("armadillo:test_save");

            expect(api.deleteUserWorkspace).toHaveBeenCalledWith(
                "bofke.dijkstra__at__umcg.nl",
                "armadillo:test_save"
            );
            expect(wrapper.vm.deleteSuccessMessages).toEqual([
                "[armadillo:test_save] for user [bofke.dijkstra__at__umcg.nl]",
            ]);
            expect(wrapper.vm.deleteErrorMessages).toEqual([]);
        });

        test("records an error message when deletion fails", async () => {
            api.deleteUserWorkspace.mockRejectedValueOnce("could not delete");

            await wrapper.vm.deleteWorkspace("armadillo:test_save");

            expect(wrapper.vm.deleteErrorMessages).toEqual([
                "[armadillo:test_save] for user [bofke.dijkstra__at__umcg.nl] because could not delete",
            ]);
            expect(wrapper.vm.deleteSuccessMessages).toEqual([]);
        });
    });

    describe("collectDeleteMessages", () => {
        test("builds a success-only message and clears buffers", () => {
            wrapper.vm.deleteSuccessMessages = ["[a] for user [b]"];
            wrapper.vm.deleteErrorMessages = [];

            wrapper.vm.collectDeleteMessages();

            expect(wrapper.vm.successMessage).toContain(
                "Successfully deleted workspace"
            );
            expect(wrapper.vm.deleteSuccessMessages).toEqual([]);
            expect(wrapper.vm.deleteErrorMessages).toEqual([]);
        });

        test("builds an error message (pluralised) when failures are present", () => {
            wrapper.vm.deleteSuccessMessages = ["[a] for user [b]"];
            wrapper.vm.deleteErrorMessages = ["[c] for user [b] because oops", "[d] for user [b] because oops"];

            wrapper.vm.collectDeleteMessages();

            expect(wrapper.vm.errorMessage).toContain("Could not delete workspaces");
            expect(wrapper.vm.deleteSuccessMessages).toEqual([]);
            expect(wrapper.vm.deleteErrorMessages).toEqual([]);
        });
    });

    describe("deleteSelectedWorkspaces", () => {
        test("deletes every checked workspace and refreshes the list", async () => {
            api.deleteUserWorkspace.mockResolvedValue(undefined);
            wrapper.vm.selectedUser = "user-bofke.dijkstra__at__umcg.nl";
            wrapper.vm.setWorkspaces("user-bofke.dijkstra__at__umcg.nl");
            wrapper.vm.userWorkspaces[0].checked = true;
            wrapper.vm.userWorkspaces[1].checked = true;

            await wrapper.vm.deleteSelectedWorkspaces();

            expect(api.deleteUserWorkspace).toHaveBeenCalledTimes(2);
            expect(wrapper.vm.successMessage).toContain("Successfully deleted workspaces");
            expect(wrapper.vm.deleteSuccessMessages).toEqual([]);
        });
    });

    describe("downloadSelectedWorkspaces", () => {
        beforeEach(() => {
            wrapper.vm.selectedUser = "user-bofke.dijkstra__at__umcg.nl";
            wrapper.vm.setWorkspaces("user-bofke.dijkstra__at__umcg.nl");
            wrapper.vm.userWorkspaces[0].checked = true;
        });

        test("tracks successful downloads", async () => {
            api.downloadWorkspace.mockResolvedValueOnce(undefined);

            await wrapper.vm.downloadSelectedWorkspaces();
            await flushPromises();

            expect(api.downloadWorkspace).toHaveBeenCalledWith(
                "cohort_1:my-workspace",
                "user-bofke.dijkstra__at__umcg.nl"
            );
            expect(wrapper.vm.downloadSuccesses).toEqual(["cohort_1:my-workspace"]);
            expect(wrapper.vm.successMessage).toContain("Succesfully downloaded");
        });

        test("tracks failed downloads", async () => {
            api.downloadWorkspace.mockRejectedValueOnce("fail");

            await wrapper.vm.downloadSelectedWorkspaces();
            await flushPromises();

            expect(wrapper.vm.downloadFailures).toEqual(["cohort_1:my-workspace"]);
            expect(wrapper.vm.errorMessage).toContain("Download failure");
        });
    });

    describe("upload handlers", () => {
        test("onSuccess sets a success message and reloads the workspace list", async () => {
            wrapper.vm.selectedUser = "user-bofke.dijkstra__at__umcg.nl";
            const callsBefore = api.getWorkspaceDetails.mock.calls.length;

            await wrapper.vm.onSuccess({ filename: "new-workspace.RData" });
            await flushPromises();

            expect(wrapper.vm.successMessage).toContain("new-workspace.RData");
            expect(wrapper.vm.successMessage).toContain(
                "bofke.dijkstra__at__umcg.nl"
            );
            expect(api.getWorkspaceDetails.mock.calls.length).toBeGreaterThan(
                callsBefore
            );
        });

        test("onError sets an error message", () => {
            wrapper.vm.selectedUser = "user-bofke.dijkstra__at__umcg.nl";

            wrapper.vm.onError("disk full");

            expect(wrapper.vm.errorMessage).toContain("disk full");
            expect(wrapper.vm.errorMessage).toContain(
                "bofke.dijkstra__at__umcg.nl"
            );
        });
    });

    describe("computed properties", () => {
        test("showAllWorkspaces is true only for the 'All workspaces' pseudo-user", () => {
            wrapper.vm.selectedUser = "All workspaces";
            expect(wrapper.vm.showAllWorkspaces).toBe(true);

            wrapper.vm.selectedUser = "user-bofke.dijkstra__at__umcg.nl";
            expect(wrapper.vm.showAllWorkspaces).toBe(false);
        });

        test("usernameFromFolder strips the 'user-' prefix", () => {
            wrapper.vm.selectedUser = "user-bofke.dijkstra__at__umcg.nl";
            expect(wrapper.vm.usernameFromFolder).toBe(
                "bofke.dijkstra__at__umcg.nl"
            );
        });

        test("selectedWorkspaces returns only the names of checked workspaces", () => {
            wrapper.vm.setWorkspaces("user-bofke.dijkstra__at__umcg.nl");
            wrapper.vm.userWorkspaces[0].checked = true;
            wrapper.vm.userWorkspaces[2].checked = true;

            expect(wrapper.vm.selectedWorkspaces).toEqual([
                "cohort_1:my-workspace",
                "armadillo:test_save",
            ]);
        });

        test("formattedWorkspaces converts sizes and dates and tags each entry with its user", () => {
            const formatted =
                wrapper.vm.formattedWorkspaces["user-bofke.dijkstra__at__umcg.nl"];
            const original = testData["user-bofke.dijkstra__at__umcg.nl"][0];
            const match = formatted.find((ws: any) => ws.name === original.name);

            expect(match.user).toBe("user-bofke.dijkstra__at__umcg.nl");
            expect(match.size).toBe(convertBytes(original.size));
            expect(match.lastModified.getTime()).toBe(
                new Date(original.lastModified).getTime()
            );
        });

        test("sortedWorkspaces orders user keys alphabetically", () => {
            const keys = Object.keys(wrapper.vm.sortedWorkspaces);
            const sorted = [...keys].sort((a, b) => a.localeCompare(b));
            expect(keys).toEqual(sorted);
        });

        test("filteredWorkspaces omits the 'user' field for a specific user", () => {
            wrapper.vm.selectedUser = "user-bofke.dijkstra__at__umcg.nl";
            const entries =
                wrapper.vm.filteredWorkspaces["user-bofke.dijkstra__at__umcg.nl"];
            entries.forEach((ws: any) => {
                expect(ws.user).toBeUndefined();
            });
        });

        test("filteredWorkspaces keeps the 'user' field for 'All workspaces'", () => {
            wrapper.vm.selectedUser = "All workspaces";
            const entries = wrapper.vm.filteredWorkspaces["All workspaces"];
            expect(entries.length).toBeGreaterThan(0);
            entries.forEach((ws: any) => {
                expect(ws.user).toBeDefined();
            });
        });

        test("filteredHeaders includes 'user' only for 'All workspaces'", () => {
            wrapper.vm.selectedUser = "All workspaces";
            expect(wrapper.vm.filteredHeaders).toEqual([
                "user",
                "name",
                "size",
                "lastModified",
            ]);

            wrapper.vm.selectedUser = "user-bofke.dijkstra__at__umcg.nl";
            expect(wrapper.vm.filteredHeaders).toEqual([
                "name",
                "size",
                "lastModified",
            ]);
        });
    });
});