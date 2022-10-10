import { mount } from "@vue/test-utils";
import BadgeList from "@/components/BadgeList.vue";

describe("BadgeList", () => {
  test("shows badges", () => {
    const wrapper = mount(BadgeList, {
      props: {
        itemArray: ["item_a", "item_b", "item_c"],
        saveCallback: jest.fn(),
        row: { firstName: "James", lastName: "Doe" },
      },
    });

    // Assert the rendered text of the componentgg
    expect(wrapper.text()).toContain("item_a");
    expect(wrapper.text()).toContain("item_b");
    expect(wrapper.text()).toContain("item_c");
  });
});
