import { mount, VueWrapper } from "@vue/test-utils";
import FeedbackMessage from "@/components/FeedbackMessage.vue";

describe("FeedbackMessage", () => {
  let wrapper: VueWrapper<any>;
  let projects = ["lifecycle", "hallo", "molgenis"];
  beforeEach(function () {
    wrapper = mount(FeedbackMessage, {
      props: {
        successMessage: "Hello world",
        errorMessage: "Destructing world in 3...2...1",
      },
    });
  });
  test("displays message", () => {
    // Assert the rendered text of the component
    expect(wrapper.text()).toContain("Hello world");
    expect(wrapper.text()).toContain("Destructing world in 3...2...1");
  });
  test("clears success message after 5 seconds", () => {
    wrapper.vm.successMsg = "Something else";
    setTimeout(() => {
      expect(wrapper.vm.successMsg).toBe("");
    }, 5500);
  });
});
