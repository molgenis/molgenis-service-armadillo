import { mount } from "@vue/test-utils";
import InlineRowEdit from "@/components/InlineRowEdit.vue";

const saveMock = jest.fn();
const cancelMock = jest.fn();
const wrapper = mount(InlineRowEdit, {
  props: {
    save: saveMock,
    cancel: cancelMock,
    dataStructure: {
      column_a: "string",
      column_b: "string",
      column_c: "string",
      column_d: "boolean",
      column_e: "array",
      column_f: "string",
    },
    row: {
      column_a: "hello",
      column_b: "goodbye",
      column_c: "ciao",
      column_d: true,
      column_e: ["a", "b", "c"],
    },
  },
});

describe("InlineRowEdit", () => {
  test("shows provided row", () => {
    expect(wrapper.html()).toContain("hello");
    expect(wrapper.html()).toContain("goodbye");
    expect(wrapper.html()).toContain("ciao");
  });

  test("shows header of missing element", () => {
    expect(wrapper.html()).toContain("column_f");
  });

  test("provides the correct number of input boxes", () => {
    const inputs = wrapper.findAll("input");
    expect(inputs.length).toBe(5);
  });

  test("creates checkbox for boolean", () => {
    const inputs = wrapper.findAll("input.form-check-input");
    expect(inputs.length).toBe(1);
  });

  test("triggers save function on click of save button", () => {
    const buttons = wrapper.findAll("button.btn.btn-sm.btn-success.bg-success");
    buttons[0].trigger("click");
    expect(saveMock).toHaveBeenCalled();
  });

  test("triggers save function on click of save button", () => {
    const buttons = wrapper.findAll("button.btn.btn-sm.btn-danger.bg-danger");
    buttons[0].trigger("click");
    expect(cancelMock).toHaveBeenCalled();
  });
});
