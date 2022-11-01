import { mount } from "@vue/test-utils";
import Tabs from "@/components/Tabs.vue";

describe("Tabs", () => {
  const mockRoute = {
    fullPath: "/item_b",
  };
  const wrapper = mount(Tabs, {
    props: {
      menu: ["item_a", "item_b", "item_c"],
      icons: ["monkey", "horse", "zebra"],
    },
    global: {
      mocks: {
        $route: mockRoute,
      },
    },
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
