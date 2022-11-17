import { shallowMount } from "@vue/test-utils";
import SearchBar from "@/components/SearchBar.vue";

describe("SearchBar", () => {
  test("searchString should be updated on input", async () => {
    const searchValue = "somesearchvalue";
    const wrapper = shallowMount(SearchBar);
    const input = wrapper.find("input");

    await input.setValue(searchValue);

    expect(input.element.value).toBe(searchValue);
    expect(wrapper.emitted()).toHaveProperty('update:modelValue');
  });
});
