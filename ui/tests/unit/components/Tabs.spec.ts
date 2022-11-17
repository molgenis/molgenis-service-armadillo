import { mount, VueWrapper } from "@vue/test-utils";
import Tabs from "@/components/Tabs.vue";
import { createRouter, createWebHistory } from "vue-router";

describe("Tabs", () => {
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

  const mockRoute = {
    fullPath: "/item_b",
  };
  const mockRouter = {
    push: jest.fn(),
  };

  let wrapper: VueWrapper<any>;
  beforeEach(function () {
    wrapper = mount(Tabs, {
      props: {
        menu: ["item_a", "item_b", "item_c"],
        icons: ["monkey", "horse", "zebra"],
      },
      global: {
        plugins: [router],
        mocks: {
          $route: mockRoute,
          $router: mockRouter,
        },
      },
    });
  });
  test("shows tabs with icons", () => {
    // Assert the rendered text of the component
    expect(wrapper.html()).toContain("item_a");
    expect(wrapper.html()).toContain("item_b");
    expect(wrapper.html()).toContain("item_c");
    expect(wrapper.html()).toContain("bi-monkey");
    expect(wrapper.html()).toContain("bi-horse");
    expect(wrapper.html()).toContain("bi-zebra");
  });

  test("expect item_b to be active", () => {
    const activeTab = wrapper.find("button.nav-link.active");
    expect(activeTab.html()).toContain("item_b");
  });
});
