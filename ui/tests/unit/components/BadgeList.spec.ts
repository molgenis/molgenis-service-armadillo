import { mount, VueWrapper } from "@vue/test-utils";
import BadgeList from "@/components/BadgeList.vue";

describe("BadgeList", () => {
  let wrapper: VueWrapper<any>;
  let projects = ["lifecycle", "hallo", "molgenis"];
  beforeEach(function () {
    wrapper = mount(BadgeList, {
      props: {
        itemArray: projects,
        canEdit: true,
      },
    });
  });
  test("shows badges", () => {
    // Assert the rendered text of the component
    expect(wrapper.text()).toContain("lifecycle");
    expect(wrapper.text()).toContain("molgenis");
  });

  test("removes correct badge", () => {
    wrapper.vm.remove(1);
    expect(wrapper.emitted()).toHaveProperty("update");
    expect(wrapper.emitted("update")).toEqual([[["lifecycle", "molgenis"]]]);
  });
});

