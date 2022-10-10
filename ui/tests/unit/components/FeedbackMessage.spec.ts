import { mount } from "@vue/test-utils";
import FeedbackMessage from "@/components/FeedbackMessage.vue";

test("displays message", () => {
  const wrapper = mount(FeedbackMessage, {
    props: {
      successMessage: "Hello world",
      errorMessage: "Destructing world in 3...2...1",
    },
  });

  // Assert the rendered text of the component
  expect(wrapper.text()).toContain("Hello world");
  expect(wrapper.text()).toContain("Destructing world in 3...2...1");
});
