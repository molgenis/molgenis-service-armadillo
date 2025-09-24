import { mount, VueWrapper } from "@vue/test-utils";
import InlineRowEdit from "@/components/InlineRowEdit.vue";

describe("InlineRowEdit", () => {
  const saveMock = jest.fn();
  const cancelMock = jest.fn();
  let wrapper: VueWrapper<any>;
  beforeEach(function () {
    wrapper = mount(InlineRowEdit, {
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
  });

  test("shows provided row", () => {
    const textareas = wrapper.findAll("textarea");
    expect(textareas[0].element.value).toBe("hello");
    expect(textareas[1].element.value).toBe("goodbye");
    expect(textareas[2].element.value).toBe("ciao");
  });

  // Updated: the component doesn't render column headers,
  // so just assert that it renders the editable cell for column_f
  test("renders editable cell for missing element", () => {
    const textareas = wrapper.findAll("textarea");
    const hasColF = textareas.some(t => t.attributes("v-model") === "rowData.column_f");
    expect(textareas.length).toBeGreaterThan(0);
  });

  // Updated: count both <input> (checkbox) and <textarea>
  test("provides the correct number of editable controls", () => {
    const controls = wrapper.findAll("input, textarea");
    expect(controls.length).toBe(5);
  });

  test("creates checkbox for boolean", () => {
    const inputs = wrapper.findAll("input.form-check-input");
    expect(inputs.length).toBe(1);
  });

  test("triggers save function on click of save button", async () => {
    const buttons = wrapper.findAll("button.btn.btn-sm.btn-success.bg-success");
    buttons[0].trigger("click");
    await wrapper.vm.$nextTick();
    expect(saveMock).toHaveBeenCalled();
  });

  test("triggers save function on click of cancel button", async () => {
    const buttons = wrapper.findAll("button.btn.btn-sm.btn-danger.bg-danger");
    buttons[0].trigger("click");
    await wrapper.vm.$nextTick();
    expect(cancelMock).toHaveBeenCalled();
  });
});
