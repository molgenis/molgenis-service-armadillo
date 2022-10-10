import { mount } from "@vue/test-utils";
import TabContent from "@/components/TabContent.vue";

describe("TabContent", () => {
  test("displays provided tabs and is active", () => {
    const wrapper = mount(TabContent, {
      props: {
        menuItem: "item1",
        menuIndex: 0,
        isActive: true,
      },
    });

    expect(wrapper.html()).toContain("item1");
    expect(wrapper.findAll(".active").length).toBe(1);
    expect(wrapper.findAll(".show").length).toBe(1);
  });

  test("is inactive", () => {
    const wrapper = mount(TabContent, {
      props: {
        menuItem: "item1",
        menuIndex: 0,
        isActive: false,
      },
    });
    expect(wrapper.findAll(".active").length).toBe(0);
    expect(wrapper.findAll(".show").length).toBe(0);
  });
});
