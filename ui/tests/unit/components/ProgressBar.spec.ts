import { mount } from "@vue/test-utils";
import ProgressBar from "@/components/ProgressBar.vue";

describe("ProgressBar", () => {
  test("shows percentage", async () => {
    const percentage = 32;
    const wrapper = mount(ProgressBar, {props: {percentage: percentage}});
    expect(wrapper.html()).toContain("32");
    expect(wrapper.html()).toContain("progress-bar-animated progress-bar-striped");
  });
  test("is green when 100%", async () => {
    const percentage = 100;
    const wrapper = mount(ProgressBar, {props: {percentage: percentage}});
    expect(wrapper.html()).toContain("bg-success");
  });
});
