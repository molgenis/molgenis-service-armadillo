import { mount, VueWrapper } from "@vue/test-utils";
import KeyValueInput from "@/components/KeyValueInput.vue";

describe("KeyValueInput", () => {
  let wrapper: VueWrapper<any>;
  beforeEach(function () {
    wrapper = mount(KeyValueInput, {
      props: { modelValue: { val_a: "a", val_b: "b", val_c: "c" } },
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
    const keyInput = wrapper.find("input.key-input");
    await keyInput.setValue("val_d");
    const valInput = wrapper.find("input.value-input");
    await valInput.setValue("d");
    const addButton = wrapper.find("button.add-new-value");
    addButton.trigger("click");
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted()).toHaveProperty("update");
    expect(wrapper.emitted("update")).toEqual([
      [{ val_a: "a", val_b: "b", val_c: "c", val_d: "d" }],
    ]);
  });

  test("can cancel adding value", async () => {
    wrapper.vm.showAdd = true;
    await wrapper.vm.$nextTick();
    const keyInput = wrapper.find("input.key-input");
    await keyInput.setValue("val_d");
    const valInput = wrapper.find("input.value-input");
    await valInput.setValue("d");
    const cancelButton = wrapper.find("button.cancel-new-value");
    cancelButton.trigger("click");
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.showAdd).toBe(false);
    expect(wrapper.vm.newValue).toBe("");
    expect(wrapper.vm.newKey).toBe("");
  });

  test("can remove value", async () => {
    const removeButtons = wrapper.findAll("button.remove-badge");
    removeButtons[0].trigger("click");
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted()).toHaveProperty("update");
    expect(wrapper.emitted("update")).toEqual([
      [{ val_b: "b", val_c: "c"}],
    ]);
  });
});
