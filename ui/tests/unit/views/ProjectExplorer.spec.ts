import { shallowMount, VueWrapper } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import ProjectsExplorer from "@/views/ProjectsExplorer.vue";

describe("ProjectExplorer", () => {
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

  router.currentRoute.value.params = { projectId: "my-project", folderId: "myFolder"}

  let wrapper: VueWrapper<any>;

  beforeEach(function () {
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
  test("clears filePreview", () => {
    wrapper.vm.filePreview = ["something"];
    wrapper.vm.clearFilePreview();
    expect(wrapper.vm.filePreview).toEqual([{}]);
  });
});
