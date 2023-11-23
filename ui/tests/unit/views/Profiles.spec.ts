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
    let singleTestData: Profile[];

    let profileToAdd: Profile;
    let default_profile_running: Profile;
    let default_profile_not_running: Profile;

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

        default_profile_not_running = {
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
        }

        default_profile_running = JSON.parse(JSON.stringify(default_profile_not_running))
        default_profile_running.container.status = "RUNNING"

        singleTestData = [
            {
                name: "profile-one",
                image: "source/some_profile-one",
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
                    tags: ["source/some_profile-two"],
                    status: "NOT_RUNNING"
                }
            }
        ];

        testData = [default_profile_not_running].concat(singleTestData);

        api.getProfiles.mockImplementationOnce(() => {
            return Promise.resolve(testData);
        });
        api.putProfile.mockImplementationOnce((profileJson: Profile) => {
            return Promise.resolve(profileJson)
        });

        profileToAdd = {
            name: "profile-two",
            image: "other_source/profile-two",
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
                tags: ["other_source/profile-two"],
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
        wrapper.vm.profileToEdit = "profile-one";
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

    test("returns error when loading profiles fails", async () => {
        const error = new Error("fail");
        api.getProfiles.mockImplementation(() => {
            return Promise.reject(error);
        });
        wrapper.vm.reloadProfiles();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.errorMessage).toBe(`Could not load profiles: ${error}.`);
    });

    test("fail to rename default profile", () => {
        wrapper.vm.profiles.unshift(profileToAdd);
        wrapper.vm.profileToEditIndex = 0;
        wrapper.vm.profileToEdit = "default"
        wrapper.vm.saveEditedProfile();
        expect(wrapper.vm.errorMessage).toBe("Save failed: cannot rename 'default' package.");
    });

    test("fail to use same port for profile", () => {
        wrapper.vm.profiles.unshift(profileToAdd);
        let p = {... profileToAdd};
        p.port = 6313;
        wrapper.vm.profiles.unshift(p);
        wrapper.vm.profileToEditIndex = 0;
        wrapper.vm.saveEditedProfile();
        expect(wrapper.vm.errorMessage).toBe("Save failed: [localhost:6313] already used.");
    });

    test("fail to save a unnamed profile", () => {
        wrapper.vm.addNewProfile();
        wrapper.vm.saveEditedProfile();
        expect(wrapper.vm.errorMessage).toBe("Cannot create profile with empty name.");
    });

    test("starting default profile", async () => {
        api.startProfile.mockImplementationOnce(() => {
            return Promise.resolve(default_profile_running)
        });
        api.getProfiles.mockImplementation(() => {
            return Promise.resolve([default_profile_running].concat(singleTestData))
        });
        wrapper.vm.startProfile("default");
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.successMessage).toBe("[default] was successfully started.");
        expect(wrapper.vm.errorMessage).toBe("");
        wrapper.vm.profileToEdit = "default";
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.profiles[wrapper.vm.getEditIndex()].name).toBe("default");
        expect(wrapper.vm.profiles[wrapper.vm.getEditIndex()].container.status).toBe("RUNNING");
    });

    test("stopping default profile", async () => {
        api.startProfile.mockImplementationOnce(() => {
            return Promise.resolve(default_profile_running)
        })
        api.stopProfile.mockImplementationOnce(() => {
            return Promise.resolve(default_profile_not_running)
        });
        api.getProfiles.mockImplementation(() => {
            return Promise.resolve([default_profile_running].concat(singleTestData))
        });
        wrapper.vm.startProfile("default");
        await wrapper.vm.$nextTick()
        expect(wrapper.vm.successMessage).toBe("[default] was successfully started.");
        expect(wrapper.vm.errorMessage).toBe("");
        wrapper.vm.profileToEdit = "default";
        wrapper.vm.reloadProfiles();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.profiles[wrapper.vm.getEditIndex()].name).toBe("default");
        expect(wrapper.vm.profiles[wrapper.vm.getEditIndex()].container.status).toBe("RUNNING");
        api.getProfiles.mockImplementation(() => {
            return Promise.resolve([default_profile_not_running].concat(singleTestData))
        });
        wrapper.vm.stopProfile("default");
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.successMessage).toBe("[default] was successfully stopped.");
        expect(wrapper.vm.errorMessage).toBe("");
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.profiles[wrapper.vm.getEditIndex()].container.status).toBe("NOT_RUNNING");
    });

    test("creating a profile", async () => {
        wrapper.vm.addNewProfile();
        wrapper.vm.profiles[0] = profileToAdd;
        wrapper.vm.saveEditedProfile();
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.successMessage).toBe("[profile-two] was successfully saved.");
        expect(wrapper.vm.errorMessage).toBe("");
        expect(wrapper.vm.profiles.includes(profileToAdd)).toBe(true);
    });
});
