import { shallowMount, VueWrapper } from "@vue/test-utils";
import Projects from "@/views/Projects.vue";
import { createRouter, createWebHistory } from "vue-router";
import * as _api from "@/api/api";

const api = _api as any;

jest.mock("@/api/api");

describe("Projects", () => {
  const testData = [
    {
      name: "environmental",
      users: ["m.fiamma@ospedale.it"],
    },
    {
      name: "project3",
      users: ["stephen.chapman@glasgow.ac.uk", "a.anamarija@univerza.si"],
    },
    {
      name: "snow1",
      users: ["m.fiamma@ospedale.it"],
    },
    {
      name: "molgenis",
      users: ["a.victor@umcg.nl"],
    },
    {
      name: "blood",
      users: ["a.anamarija@univerza.si"],
    },
    {
      name: "bro",
      users: ["a.anamarija@univerza.si"],
    },
    {
      name: "research",
      users: [
        "j.doe@example.com",
        "l.knope@pawnee-uni.com",
        "a.anamarija@univerza.si",
        "a.ida@yliopisto.fi",
      ],
    },
  ];
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
    routes: mock_routes,
  });
  let wrapper: VueWrapper<any>;

  beforeEach(function () {
    const mockRouter = {
      push: jest.fn(),
    };
    api.getProjects.mockImplementationOnce(() => {
      return Promise.resolve(testData);
    });
    wrapper = shallowMount(Projects, {
      global: {
        plugins: [router],
        mocks: {
          $router: mockRouter,
        },
      },
    });
  });
  test("clears user messages", async () => {
    wrapper.vm.successMessage = "test";
    wrapper.vm.errorMessage = "test";
    wrapper.vm.clearUserMessages();
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.successMessage).toBe("");
    expect(wrapper.vm.errorMessage).toBe("");
  });

  test("clears project to edit", async () => {
    wrapper.vm.projectToEdit = "test";
    wrapper.vm.clearProjectToEdit();
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.projectToEdit).toBe("");
  });

  test("clears project to edit", async () => {
    wrapper.vm.projectToEdit = "test";
    wrapper.vm.clearProjectToEdit();
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.projectToEdit).toBe("");
  });

  test("sets project to edit", async () => {
    wrapper.vm.projectToEdit = "";
    wrapper.vm.editProject({ name: "molgenis", users: ["tommy", "mariska"] });
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.projectToEdit).toBe("molgenis");
  });

  test("retrieves index of project to edit", async () => {
    wrapper.vm.projectToEdit = "molgenis";
    const projects = wrapper.vm.projects;
    const index = wrapper.vm.getEditIndex();
    await wrapper.vm.$nextTick();
    expect(index).toBe(3);
  });

  test("should return old index if projectname is altered", async () => {
    wrapper.vm.projectToEditIndex = 2;
    wrapper.vm.projectToEdit = "molgenis_project";
    const index = wrapper.vm.getEditIndex();
    await wrapper.vm.$nextTick();
    expect(index).toBe(2);
  });

  test("returns -1 if name of project is empty (to be able to rename project)", async () => {
    wrapper.vm.projectToEditIndex = 3;
    wrapper.vm.projectToEdit = "";
    const index = wrapper.vm.getEditIndex();
    await wrapper.vm.$nextTick();
    expect(index).toBe(-1);
  });

  test("calls loadProjects and sets loading to false on success", async () => {
    api.getProjects.mockImplementation(() => {
      return Promise.resolve(testData);
    });
    wrapper.vm.reloadProjects();
    expect(wrapper.vm.loading).toBe(true);
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.loading).toBe(false);
  });

  test("calls loadProjects, sets loading to false and sets error message if reloading fails", async () => {
    const error = new Error("fail");
    api.getProjects.mockImplementation(() => {
      return Promise.reject(error);
    });
    wrapper.vm.reloadProjects();
    expect(wrapper.vm.loading).toBe(true);
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.errorMessage).toBe(
      "Could not load projects: Error: fail."
    );
  });

  test("filters and sorts projects alphabetically", async () => {
    wrapper.vm.searchString = "ro";
    const filteredAndSorted = wrapper.vm.getFilteredAndSortedProjects();
    await wrapper.vm.$nextTick();
    expect(filteredAndSorted.length).toBe(3);
    expect(filteredAndSorted).toEqual([
      {
        name: "bro",
        users: ["a.anamarija@univerza.si"],
      },
      {
        name: "environmental",
        users: ["m.fiamma@ospedale.it"],
      },
      {
        name: "project3",
        users: ["stephen.chapman@glasgow.ac.uk", "a.anamarija@univerza.si"],
      },
    ]);
  });

  test("removes project", async () => {
    api.deleteProject.mockImplementation(() => {
      return Promise.resolve({});
    });
    wrapper.vm.removeProject({
      name: "bro",
      users: ["a.anamarija@univerza.si"],
    });
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.successMessage).toBe("[bro] was successfully deleted.");
  });

  test("removes project fails", async () => {
    const error = new Error("fail");
    api.deleteProject.mockImplementation(() => {
      return Promise.reject(error);
    });
    wrapper.vm.removeProject({
      name: "doesntexist",
      users: [],
    });
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.errorMessage).toBe(
      "Could not delete [doesntexist]: Error: fail."
    );
  });

  test("edited project is saved", async () => {
    const reloadMock = jest.fn();
    const putMock = jest.fn();
    const deleteMock = jest.fn();
    api.getProjects.mockImplementation(() => {
      reloadMock();
      return Promise.resolve(testData);
    });
    api.putProject.mockImplementation(() => {
      putMock();
      return Promise.resolve({});
    });
    api.deleteProject.mockImplementation(() => {
      deleteMock();
      return Promise.resolve({});
    });
    wrapper.vm.projectToEdit = "molgenis";
    wrapper.vm.projects[3] = {
      name: "molgenisArmadillo",
      users: ["a.victor@umcg.nl"],
    };
    wrapper.vm.projectToEditIndex = 3;
    wrapper.vm.saveEditedProject();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    // called in clearProjectToEdit and saveEditedProject
    expect(reloadMock.mock.calls.length).toBe(2);
    // called in saveProject
    expect(putMock).toHaveBeenCalled();
    // called because we change the project name in removeProject
    expect(deleteMock).toHaveBeenCalled();
    // happens in clearProjectToEdit at the end of saveEditedProject
    expect(wrapper.vm.projectToEdit).toBe("");
  });
});
