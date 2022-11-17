import { mount, VueWrapper } from "@vue/test-utils";
import BadgeList from "@/components/BadgeList.vue";

describe("BadgeList", () => {
  let wrapper: VueWrapper<any>;
  beforeEach(function () {
    wrapper = mount(BadgeList, {
      props: {
        itemArray: ["item_a", "item_b", "item_c"],
        saveCallback: jest.fn(),
        row: { firstName: "James", lastName: "Doe" },
      },
    });
  });
  test("shows badges", () => {
    // Assert the rendered text of the componentgg
    expect(wrapper.text()).toContain("item_a");
    expect(wrapper.text()).toContain("item_b");
    expect(wrapper.text()).toContain("item_c");
  });
  
  test("removes badges", () => {
  wrapper.vm.remove(1);
    // We cannot check if the property is really removed, since the watcher will reset it to the "props"
    // The parent component will be updated when the update event is emitted, causing the item to be removed.
    expect(wrapper.emitted()).toHaveProperty("update");
  });
});
