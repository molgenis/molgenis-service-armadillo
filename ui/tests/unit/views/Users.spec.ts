import { shallowMount, VueWrapper } from "@vue/test-utils";
import Users from "@/views/Users.vue";
import { createRouter, createWebHistory } from "vue-router";
import * as _api from "@/api/api";
import { User } from "@/types/api";

const api = _api as any;

jest.mock("@/api/api");

describe("Users", () => {
  let testData: User[];

  let userToAdd: User;
  let userToEdit: User;

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
    const projects = [
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
    testData = [
      {
        email: "j.doe@example.com",
        firstName: "John",
        lastName: "Doe",
        institution: "example",
        admin: false,
        projects: ["project2"],
      },
      {
        email: "b.dijkstra@hospital.nl",
        firstName: "Bofke",
        lastName: "Dijkstra",
        institution: "hospital",
        admin: false,
        projects: ["project1"],
      },
      {
        email: "j.doe@university.nl",
        firstName: "Jane",
        lastName: "Doe",
        institution: "university",
        admin: true,
        projects: ["project1", "project2", "project3"],
      },
    ];

    const mockRouter = {
      push: jest.fn(),
    };
    api.getUsers.mockImplementationOnce(() => {
      return Promise.resolve(testData);
    });
    api.getProjects.mockImplementationOnce(() => {
      return Promise.resolve(projects);
    });

    userToAdd = {
      email: "h.t.gump@psr.com",
      firstName: "Henk Tony",
      lastName: "Gump",
      institution: "Prof Snooby's Research Hospital",
      admin: false,
      projects: ["project1", "project3"],
    };

    userToEdit = {
      email: "j.doe@example.com",
      firstName: "John",
      lastName: "Doe",
      institution: "example",
      admin: false,
      projects: ["project2"],
    };

    wrapper = shallowMount(Users, {
      global: {
        plugins: [router],
        mocks: {
          $router: mockRouter,
        },
      },
    });
  });
  test("clears updated user index", () => {
    wrapper.vm.updatedUserIndex = 2;
    wrapper.vm.clearUpdatedUserIndex();
    expect(wrapper.vm.updatedUserIndex).toBe(-1);
  });

  test("clears user messages", () => {
    wrapper.vm.successMessage = "test";
    wrapper.vm.errorMessage = "test";
    wrapper.vm.clearUserMessages();
    expect(wrapper.vm.successMessage).toBe("");
    expect(wrapper.vm.errorMessage).toBe("");
  });

  test("clears user to edit", () => {
    wrapper.vm.userToEdit = "b.dijkstra@hospital.nl";
    wrapper.vm.clearUserToEdit();
    expect(wrapper.vm.userToEdit).toBe("");
  });

  test("clears new user", () => {
    wrapper.vm.addRow = true;
    wrapper.vm.addMode.newUser = userToAdd;
    wrapper.vm.userToEdit = "";
    wrapper.vm.clearNewUser();
    expect(wrapper.vm.addMode.newUser).toEqual({
      email: "",
      firstName: "",
      lastName: "",
      institution: "",
      admin: false,
      projects: [],
    });
    expect(wrapper.vm.addRow).toBe(false);
  });

  test("edits user", () => {
    wrapper.vm.userToEdit = "";
    wrapper.vm.addRow = true;
    wrapper.vm.editUser(userToEdit);
    expect(wrapper.vm.addRow).toBe(false);
    expect(wrapper.vm.userToEdit).toBe(userToEdit.email);
  });

  test("retrieves index of user to edit", () => {
    wrapper.vm.userToEdit = userToEdit.email;
    const index = wrapper.vm.getEditIndex();
    expect(index).toBe(1);
  });

  test("reloads users", async () => {
    const testFunction = jest.fn();
    const updatedUsers = testData.concat([userToAdd]);
    api.getUsers.mockImplementation(() => {
      testFunction();
      return Promise.resolve(updatedUsers);
    });
    wrapper.vm.reloadUsers();
    expect(wrapper.vm.loading).toBe(true);
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.loading).toBe(false);
    expect(testFunction).toHaveBeenCalled();
  });

  test("reloads users", async () => {
    const testFunction = jest.fn();
    const updatedUsers = testData.concat([userToAdd]);
    api.getUsers.mockImplementation(() => {
      testFunction();
      return Promise.resolve(updatedUsers);
    });
    wrapper.vm.reloadUsers();
    expect(wrapper.vm.loading).toBe(true);
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.loading).toBe(false);
    expect(testFunction).toHaveBeenCalled();
  });

  test("returns error when loading users fails", async () => {
    const error = new Error("fail");
    api.getUsers.mockImplementation(() => {
      return Promise.reject(error);
    });
    wrapper.vm.reloadUsers();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.errorMessage).toBe(`Could not load users: ${error}.`);
  });

  test("retrieves filtered and sorted users with searchterm", async () => {
    wrapper.vm.searchString = "Doe";
    const users = wrapper.vm.getFilteredAndSortedUsers();
    expect(users).toEqual([
      {
        email: "j.doe@example.com",
        firstName: "John",
        lastName: "Doe",
        institution: "example",
        admin: false,
        projects: ["project2"],
      },
      {
        email: "j.doe@university.nl",
        firstName: "Jane",
        lastName: "Doe",
        institution: "university",
        admin: true,
        projects: ["project1", "project2", "project3"],
      },
    ]);
  });
  test("will not sort when user to edit is set", async () => {
    const userData = [
      {
        email: "g.doe@example.com",
        firstName: "Gerald",
        lastName: "Doe",
        institution: "example",
        admin: false,
        projects: ["project2"],
      },
      {
        email: "b.dijkstra@hospital.nl",
        firstName: "Bofke",
        lastName: "Dijkstra",
        institution: "hospital",
        admin: false,
        projects: ["project1"],
      },
      {
        email: "a.doe@university.nl",
        firstName: "Abel",
        lastName: "Doe",
        institution: "university",
        admin: true,
        projects: ["project1", "project2", "project3"],
      },
    ];
    wrapper.vm.users = userData;
    wrapper.vm.userToEdit = "a.doe@university.nl";
    const users = wrapper.vm.getFilteredAndSortedUsers();
    expect(users).toEqual(userData);
  });

  test("calls deleteUser", async () => {
    const john = "j.doe@example.com";
    const testFunction = jest.fn();
    api.deleteUser.mockImplementation((user: string) => {
      testFunction(user);
      return Promise.resolve();
    });
    wrapper.vm.proceedDelete(john);
    expect(testFunction).toHaveBeenCalledWith(john);
  });

  test("deletes user with edited email, saves edited user and reloads", async () => {
    const john = {
      email: "john.doe@example.com",
      firstName: "John",
      lastName: "Doe",
      institution: "example",
      admin: false,
      projects: ["project2"],
    };
    const updatedUsers = [
      john,
      {
        email: "b.dijkstra@hospital.nl",
        firstName: "Bofke",
        lastName: "Dijkstra",
        institution: "hospital",
        admin: false,
        projects: ["project1"],
      },
      {
        email: "j.doe@university.nl",
        firstName: "Jane",
        lastName: "Doe",
        institution: "university",
        admin: true,
        projects: ["project1", "project2", "project3"],
      },
    ];
    wrapper.vm.users = updatedUsers;
    wrapper.vm.editMode.userToEditIndex = 0;
    wrapper.vm.userToEdit = "j.doe@example.com";
    const getMock = jest.fn();
    const saveMock = jest.fn();
    const deleteMock = jest.fn();
    api.putUser.mockImplementation((user: User) => {
      saveMock(user);
      return Promise.resolve();
    });
    api.deleteUser.mockImplementation((user: string) => {
      deleteMock(user);
      return Promise.resolve();
    });
    api.getUsers.mockImplementation(() => {
      getMock();
      return Promise.resolve(updatedUsers);
    });
    wrapper.vm.saveEditedUser();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(saveMock).toHaveBeenCalledWith(john);
    expect(deleteMock).toHaveBeenCalledWith("j.doe@example.com");
    expect(getMock).toHaveBeenCalled();
  });

  test("calls save api call with new user as argument", async () => {
    const saveMock = jest.fn();
    api.putUser.mockImplementation((user: User) => {
      saveMock(user);
      return Promise.resolve();
    });
    wrapper.vm.addMode.newUser = userToAdd;
    wrapper.vm.saveNewUser();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(saveMock).toHaveBeenCalledWith(userToAdd);
    expect(wrapper.vm.addMode.newUser).toEqual({
      email: "",
      firstName: "",
      lastName: "",
      institution: "",
      admin: false,
      projects: [],
    });
  });
  test("saves user then reloads and runs callback", async () => {
    const getMock = jest.fn();
    const saveMock = jest.fn();
    const callback = jest.fn();
    api.putUser.mockImplementation((user: User) => {
      saveMock(user);
      return Promise.resolve();
    });
    const updatedUsers = testData.concat([userToAdd]);
    api.getUsers.mockImplementation(() => {
      getMock();
      return Promise.resolve(updatedUsers);
    });
    wrapper.vm.saveUser(userToAdd, callback);
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(saveMock).toBeCalledWith(userToAdd);
    expect(wrapper.vm.errorMessage).toBe("");
    expect(wrapper.vm.successMessage).toBe(
      "[h.t.gump@psr.com] was successfully saved."
    );
    expect(wrapper.vm.successMessage).toBe(
      "[h.t.gump@psr.com] was successfully saved."
    );
    expect(getMock).toBeCalled();
    expect(callback).toBeCalled();
  });

  test("cannot create user with empty email", () => {
    userToAdd.email = "";
    wrapper.vm.saveUser(userToAdd, undefined);
    expect(wrapper.vm.errorMessage).toBe(
      "Cannot create user with empty email address."
    );
    expect(wrapper.vm.errorMessage).toBe(
      "Cannot create user with empty email address."
    );
  });

  test("cannot add user with existing email", () => {
    userToAdd.email = "j.doe@example.com";
    wrapper.vm.addMode.newUser = userToAdd;
    wrapper.vm.saveUser(userToAdd, undefined);
    expect(wrapper.vm.errorMessage).toBe(
      "User with email address [j.doe@example.com] already exists."
    );
    expect(wrapper.vm.errorMessage).toBe(
      "User with email address [j.doe@example.com] already exists."
    );
  });

  test("toggles addRow", () => {
    wrapper.vm.addRow = false;
    wrapper.vm.toggleAddRow();
    expect(wrapper.vm.addRow).toBe(true);
  });

  test("updates admin setting", () => {
    const adminToBe = {
      email: "j.doe@example.com",
      firstName: "John",
      lastName: "Doe",
      institution: "example",
      admin: false,
      projects: ["project2"],
    };
    const saveMock = jest.fn();
    api.putUser.mockImplementation((user: User) => {
      saveMock(user);
      return Promise.resolve();
    });
    wrapper.vm.updateAdmin(adminToBe, false);
    expect(saveMock).toBeCalledWith(adminToBe);
    expect(adminToBe.admin).toBe(true);
  });

  describe("isAddingDuplicateProjectToExistingUser", () => {
    test("returns true if duplicate project is added to existing user", () => {
      wrapper.vm.projectsOfUserToEdit = ["project3"];
      wrapper.vm.editMode.userToEdit = "user";
      const observed =
        wrapper.vm.isAddingDuplicateProjectToExistingUser("project3");
      expect(observed).toBe(true);
    });
    test("returns false if non duplicate user is added to existing project", () => {
      wrapper.vm.projectsOfUserToEdit = ["project2"];
      const observed =
        wrapper.vm.isAddingDuplicateProjectToExistingUser("project3");
      expect(observed).toBe(false);
    });
  });

  describe("isAddingNonExistingProject", () => {
    test("returns true if project not existing", () => {
      wrapper.vm.availableProjects = ["project1", "project3"];
      const observed = wrapper.vm.isAddingNonExistingProject("project2");
      expect(observed).toBe(true);
    });
    test("returns false if project exists", () => {
      wrapper.vm.availableProjects = ["project1", "project3"];
      const observed = wrapper.vm.isAddingNonExistingProject("project3");
      expect(observed).toBe(false);
    });
  });

  describe("isAddingDuplicateProjectToNewUser", () => {
    test("returns false if project not existing", () => {
      wrapper.vm.addMode.newUser.projects = ["project1", "project3"];
      const observed = wrapper.vm.isAddingDuplicateProjectToNewUser("project2");
      expect(observed).toBe(false);
    });
    test("returns true if project exists", () => {
      wrapper.vm.addMode.newUser.projects = ["project1", "project3"];
      const observed = wrapper.vm.isAddingDuplicateProjectToNewUser("project3");
      expect(observed).toBe(true);
    });
  });
});
