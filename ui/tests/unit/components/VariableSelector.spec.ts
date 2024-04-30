import { shallowMount, VueWrapper } from "@vue/test-utils";
import VariableSelector from "@/components/VariableSelector.vue";

describe("VariableSelector", () => {
  let wrapper: VueWrapper<any>;
  beforeEach(function () {
    wrapper = shallowMount(VariableSelector, {
      props: {
        variables: ["teststring", "test", "beststring", "a", "b"]
      },
    });
  });

  test("updateVariables updates selected variables", async () => {
    wrapper.vm.updateVariables("a");
    expect(wrapper.vm.selectedVariables).toEqual(["a"]);
    wrapper.vm.updateVariables("b");
    expect(wrapper.vm.selectedVariables).toEqual(["a", "b"]);
  });

  test("getFilteredVariables returns variables that match search terms", async () => {
    wrapper.vm.searchString = "test";
    expect(wrapper.vm.getFilteredVariables()).toEqual(["teststring", "test"]);
  });
});
