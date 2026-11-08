import { mount } from "@vue/test-utils";
import DiskSpace from "@/components/DiskSpace.vue";

describe("DiskSpace", () => {
  test("shows available space", async () => {
    const wrapper = mount(DiskSpace, {props: {used: "341", total: "1033", percentage: 41.7}});
    expect(wrapper.html()).toContain("width: 41.7%");
    expect(wrapper.html()).toContain("Used: 341 / 1033");
  });
});
