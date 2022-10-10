import { mount } from "@vue/test-utils";
import Navbar from "@/components/Navbar.vue";

describe("Navbar", () => {
  test("displays username in navbar", () => {
    const wrapper = mount(Navbar, {
      props: {
        username: "Bofke",
      },
    });

    // Assert the rendered text of the component
    expect(wrapper.text()).toContain("Bofke");
  });
});
