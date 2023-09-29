import { shallowMount, VueWrapper } from "@vue/test-utils";
import Profiles from "@/views/Profiles.vue";
import { createRouter, createWebHistory } from "vue-router";
import * as _api from "@/api/api";
import { Profile } from "@/types/api"
import { processErrorMessages } from "@/helpers/errorProcessing";

const api = _api as any;

jest.mock("@/api/api");

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

    beforeEach(function() {
        const mockRouter = {
            push: jest.fn(),
        };

        testData = [
            {
                name: "default",
                image: "datashield/armadillo-rserver",
                host: "localhost",
                port: 6311,
                packageWhitelist: [
                    "dsBase"
                ],
                functionBlacklist: [],
                datashieldSeed: "100000000",
                options: {
                    "datashield.seed": "100000000"
                },
                container: {
                    tags:  [
                        "datashield/armadillo-rserver:2.0.0",
                        "datashield/armadillo-rserver:latest"
                      ],
                    status: "NOT_RUNNING"
                }
            },
            {
                name: "profile1",
                image: "source/some_profile1",
                host: "localhost",
                port: 6312,
                packageWhitelist: [
                    "dsBase"
                ],
                functionBlacklist: [],
                datashieldSeed: "100000001",
                options: {
                    "datashield.seed": "100000001"
                },
                container: {
                    tags: ["source/some_profile1"],
                    status: "NOT_RUNNING"
                }
            }
        ]

        api.getProfiles.mockImplementationOnce(() => {
            return Promise.resolve(testData);
        });

        profileToAdd = {
            name: "profile2",
            image: "other_source/profile2",
            host: "localhost",
            port: 6313,
            packageWhitelist: [
                "dsBase"
            ],
            functionBlacklist: [],
            datashieldSeed: "100000002",
            options: {
                "datashield.seed": "100000002"
            },
            container: {
                tags: ["other_source/profile2"],
                status: "NOT_FOUND"
            }
        }

        wrapper = shallowMount(Profiles, {
            global: {
                plugins: [router],
                mocks: {
                    $router: mockRouter,
                },
            },
        });
    });
    test("clears updated profile index and name", () => {
        wrapper.vm.profileToEditIndex = 2;
        wrapper.vm.profileToEdit = "foobar"
        wrapper.vm.clearProfileToEdit();
        expect(wrapper.vm.profileToEditIndex).toBe(-1);
        expect(wrapper.vm.profileToEdit).toBe("");
    });

    test("clears user messages", () => {
        wrapper.vm.successMessage = "testSuccess";
        wrapper.vm.errorMessage = "testError";
        wrapper.vm.clearUserMessages();
        expect(wrapper.vm.successMessage).toBe("");
        expect(wrapper.vm.errorMessage).toBe("");
    });

    test("clears new profile", () => {
        wrapper.vm.addProfile = true;
        wrapper.vm.profiles.unshift(profileToAdd);
        wrapper.vm.profileToEditIndex = 0;
        wrapper.vm.clearProfileToEdit();
        expect(wrapper.vm.addProfile).toBe(false);
        expect(wrapper.vm.profileToEditIndex).toBe(-1);
        expect(wrapper.vm.profileToEdit).toBe("");
    });

    test("new datashield seed", () => {
        expect(wrapper.vm.firstFreeSeed).toBe("100000002");
    });

    test("new profile port", () => {
        expect(wrapper.vm.firstFreePort).toBe(6313);
    });

    test("edits profile", () => {
        wrapper.vm.profileToEdit = "";
        wrapper.vm.editProfile(profileToAdd);
        expect(wrapper.vm.profileToEdit).toBe(profileToAdd.name);
    });

    test("retrieve index of profile to edit", () => {
        wrapper.vm.profileToEdit = "profile1";
        const index = wrapper.vm.getEditIndex();
        expect(index).toBe(1);
    });

    test("reloads profiles", async () => {
        const testFunction = jest.fn()
        const updatedProfiles = testData.concat([profileToAdd])
        api.getProfiles.mockImplementation(() => {
            testFunction();
            return Promise.resolve(updatedProfiles);
        });
        wrapper.vm.reloadProfiles();
        expect(wrapper.vm.loading).toBe(true);
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.loading).toBe(false);
        expect(testFunction).toHaveBeenCalled();
    });

    test("fail to rename default profile", () => {
        wrapper.vm.profiles.unshift(profileToAdd);
        wrapper.vm.profileToEditIndex = 0;
        wrapper.vm.profileToEdit = "default"
        wrapper.vm.saveEditedProfile();
        expect(wrapper.vm.errorMessage).toBe("Save failed: cannot rename 'default' package.");
    });

    test("fail to save a unnamed profile", () => {
        wrapper.vm.addNewProfile();
        wrapper.vm.saveEditedProfile();
        expect(wrapper.vm.errorMessage).toBe("Cannot create profile with empty name.");
    });
});
