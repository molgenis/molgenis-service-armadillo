import { shallowMount, VueWrapper } from "@vue/test-utils";
import StringArrayInput from "@/components/StringArrayInput.vue";

describe("StringArrayInput", () => {
  let wrapper: VueWrapper<any>;
  beforeEach(function () {
    wrapper = shallowMount(StringArrayInput, {
      props: { modelValue: ["lifecycle", "molgenis"] },
    });
  });

  test("can add value when add value button clicked", async () => {
    const button = wrapper.find("button.btn-add-value");
    button.trigger("click");
    expect(wrapper.vm.showAdd).toBe(true);
  });

  test("can save added value", async () => {
    wrapper.vm.showAdd = true;
    await wrapper.vm.$nextTick();
    const input = wrapper.find("input.array-element-input");
    await input.setValue("hallo");
    const addButton = wrapper.find("button.add-new-value");
    addButton.trigger("click");
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted()).toHaveProperty("update");
    expect(wrapper.emitted("update")).toEqual([
      [["lifecycle", "molgenis", "hallo"]],
    ]);
  });

  test("can cancel adding value", async () => {
    wrapper.vm.showAdd = true;
    await wrapper.vm.$nextTick();
    const input = wrapper.find("input.array-element-input");
    await input.setValue("hallo");
    const cancelButton = wrapper.find("button.cancel-new-value");
    cancelButton.trigger("click");
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.showAdd).toBe(false);
    expect(wrapper.vm.newValue).toBe("");
  });
});
