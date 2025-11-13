import { shallowMount } from "@vue/test-utils";
import Navbar from "@/components/Navbar.vue";

describe("Navbar", () => {
  test("displays username in navbar", () => {
    const wrapper = shallowMount(Navbar, {
      props: {
        username: "Bofke",
        version: "10.4.0",
        menu: {},
        icons: []
      },
      global: {
        stubs: ["router-link"],
      },
    });
    // Assert the rendered text of the component
    expect(wrapper.text()).toContain("Bofke");
  });
});
