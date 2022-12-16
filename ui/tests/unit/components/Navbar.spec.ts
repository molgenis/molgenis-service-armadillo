import { shallowMount } from "@vue/test-utils";
import Navbar from "@/components/Navbar.vue";

describe("Navbar", () => {
  test("displays username in navbar", () => {
    const wrapper = shallowMount(Navbar, {
      props: {
        username: "Bofke",
      },
      global: {
        stubs: ["router-link"],
      },
    });
    // Assert the rendered text of the component
    expect(wrapper.text()).toContain("Bofke");
  });
});
