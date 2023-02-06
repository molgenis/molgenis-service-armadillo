import { shallowMount, VueWrapper } from "@vue/test-utils";
import Projects from "@/views/Projects.vue";
import { createRouter, createWebHistory } from "vue-router";
import * as _api from "@/api/api";

const api = _api as any;

jest.mock("@/api/api");

describe("Projects", () => {
  const userData = [
    { email: "stephen.chapman@glasgow.ac.uk", projects: ["project3"] },
    {
      email: "a.anamarija@univerza.si",
      projects: ["project3", "blood", "bro", "research"],
    },
    { email: "m.fiamma@ospedale.it", projects: ["snow1", "environmental"] },
    { email: "a.victor@umcg.nl", projects: ["molgenis"] },
    { email: "l.knope@pawnee-uni.com", projects: ["research"] },
    { email: "j.doe@example.com", projects: ["research"] },
    { email: "a.ida@yliopisto.fi", projects: ["research"] },
  ];
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
    api.getUsers.mockImplementationOnce(() => {
      return Promise.resolve(userData);
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
  test("clears user messages", () => {
    wrapper.vm.successMessage = "test";
    wrapper.vm.errorMessage = "test";
    wrapper.vm.clearUserMessages();
    expect(wrapper.vm.successMessage).toBe("");
    expect(wrapper.vm.errorMessage).toBe("");
  });

  test("clears project to edit", () => {
    wrapper.vm.projectToEdit = { name: "test", users: ["henk", "pietje"] };
    wrapper.vm.clearProjectToEdit();
    expect(wrapper.vm.projectToEdit).toEqual({ name: "", users: [] });
  });

  test("retrieves project index", () => {
    const projectIndex = wrapper.vm.getProjectIndex("project3");
    expect(projectIndex).toBe(4);
  });

  test("sets project to edit", () => {
    wrapper.vm.projectToEdit = { name: "", users: [] };
    wrapper.vm.editProject({ name: "molgenis", users: ["tommy", "mariska"] });
    expect(wrapper.vm.projectToEdit).toEqual({
      name: "molgenis",
      users: ["tommy", "mariska"],
    });
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

  test("filters and sorts projects alphabetically", () => {
    wrapper.vm.searchString = "ro";
    const filteredAndSorted = wrapper.vm.getFilteredAndSortedProjects();
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
    wrapper.vm.recordToDelete = "bro";
    wrapper.vm.proceedDelete("bro");
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.successMessage).toBe("[bro] was successfully deleted.");
    expect(wrapper.vm.recordToDelete).toBe("");
  });

  test("remove project fails", async () => {
    const error = new Error("fail");
    api.deleteProject.mockImplementation(() => {
      return Promise.reject(error);
    });
    wrapper.vm.proceedDelete("doesntexist");
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.errorMessage).toBe(
      "Could not delete [doesntexist]: Error: fail."
    );
    expect(wrapper.vm.recordToDelete).toBe("");
  });

  test("edited project is saved", async () => {
    const reloadMock = jest.fn();
    const putMock = jest.fn();
    api.getProjects.mockImplementation(() => {
      reloadMock();
      return Promise.resolve(testData);
    });
    api.putProject.mockImplementation(() => {
      putMock();
      return Promise.resolve({});
    });
    wrapper.vm.projectToEdit = {
      name: "molgenis",
      users: ["a.victor@umcg.nl", "anotheruser@umcg.nl"],
    };
    wrapper.vm.projects[3] = {
      name: "molgenis",
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
    // happens in clearProjectToEdit at the end of saveEditedProject
    expect(wrapper.vm.projectToEdit).toEqual({ name: "", users: [] });
  });

  test("presents error message if project name is empty", () => {
    wrapper.vm.projectToEdit = {
      name: "",
      users: ["a.victor@umcg.nl"],
    };
    wrapper.vm.projects[3] = {
      name: "",
      users: ["a.victor@umcg.nl"],
    };
    wrapper.vm.projectToEditIndex = 3;
    wrapper.vm.saveEditedProject();
    expect(wrapper.vm.errorMessage).toBe(
      "Cannot create project with empty name."
    );
  });

  test("adds project", async () => {
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
    wrapper.vm.addRow = true;
    wrapper.vm.newProject = {
      name: "testproject",
      users: [],
    };
    wrapper.vm.saveNewProject();
    await wrapper.vm.$nextTick();
    expect(putMock).toBeCalled();
    expect(reloadMock).toBeCalled();
    expect(deleteMock).not.toBeCalled();
  });

  describe("isEditingProject", () => {
    test("returns true if editing project", () => {
      wrapper.vm.projectToEditIndex = 1;
      expect(wrapper.vm.isEditingProject).toBe(true);
    });
    test("returns false if not editing project", () => {
      expect(wrapper.vm.isEditingProject).toBe(false);
    });
  });
  describe("addingDuplicateUserToExistingProject", () => {
    test("returns true if duplicate user is added to existing project", () => {
      wrapper.vm.projectToEditIndex = 1;
      wrapper.vm.usersOfProjectToEdit = ["user3"];
      const observed = wrapper.vm.addingDuplicateUserToExistingProject("user3");
      expect(observed).toBe(true);
    });
    test("returns false if non duplicate user is added to existing project", () => {
      wrapper.vm.projectToEditIndex = 1;
      wrapper.vm.usersOfProjectToEdit = ["user2"];
      const observed = wrapper.vm.addingDuplicateUserToExistingProject("user3");
      expect(observed).toBe(false);
    });
  });

  describe("addingNonExistingUser", () => {
    test("returns true if user not existing", () => {
      wrapper.vm.availableUsers = ["user1", "user3"];
      const observed = wrapper.vm.addingNonExistingUser("user2");
      expect(observed).toBe(true);
    });
    test("returns false if user exists", () => {
      wrapper.vm.availableUsers = ["user1", "user3"];
      const observed = wrapper.vm.addingNonExistingUser("user3");
      expect(observed).toBe(false);
    });
  });

  describe("addingDuplicateUserToNewProject", () => {
    test("returns false if user not existing", () => {
      wrapper.vm.newProject.users = ["user1", "user3"];
      const observed = wrapper.vm.addingDuplicateUserToNewProject("user2");
      expect(observed).toBe(false);
    });
    test("returns true if user exists", () => {
      wrapper.vm.newProject.users = ["user1", "user3"];
      const observed = wrapper.vm.addingDuplicateUserToNewProject("user3");
      expect(observed).toBe(true);
    });
  });
});
