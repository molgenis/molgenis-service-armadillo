import { mount, shallowMount, VueWrapper } from "@vue/test-utils";
import StringArrayInput from "@/components/StringArrayInput.vue";

describe("StringArrayInput", () => {
  let wrapper: VueWrapper<any>;
  beforeEach(function () {
    wrapper = shallowMount(StringArrayInput, {
      props: { modelValue: ["a", "b", "c"] },
    });
  });

  test("can add value when add value button clicked", async () => {
    const button = wrapper.find("button.btn-add-value");
    button.trigger("click");
    expect(wrapper.vm.showAdd).toBe(true);
  });

  test("can save added value", async () => {
    wrapper.vm.showAdd = true;
    wrapper.vm.$nextTick().then(async () => {
      const input = wrapper.find("input.arrayElementInput");
      await input.setValue("value_d");
      const addButton = wrapper.find("button.add-new-value");
      addButton.trigger("click");
      expect(wrapper.emitted()).toHaveProperty("update");
      expect(wrapper.emitted("update")).toEqual([[["a", "b", "c", "value_d"]]]);
    });
  });


  test("can save cancel add value", async () => {
    wrapper.vm.showAdd = true;
    wrapper.vm.$nextTick().then(async () => {
      const input = wrapper.find("input.arrayElementInput");
      await input.setValue("value_d");
      const cancelButton = wrapper.find("button.cancel-new-value");
      cancelButton.trigger("click");
      expect(wrapper.vm.showAdd).toBe(false);
      expect(wrapper.vm.newValue).toBe("");
    });
  });
});
