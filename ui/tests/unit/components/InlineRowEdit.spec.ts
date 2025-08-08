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
    const values = wrapper.findAll("textarea")
      .map(w => (w.element as HTMLTextAreaElement).value)
    expect(values).toEqual(expect.arrayContaining(["hello", "goodbye", "ciao"]))
  })

  test("shows control for missing element (column_f)", () => {
    const tds = wrapper.findAll("td")
    // index 0=a, 1=b, 2=c, 3=d(bool), 4=e(array), 5=f(string)
    const tdF = tds[5]
    expect(tdF.find("textarea").exists()).toBe(true)
  })
  
  test("provides the correct number of input boxes", () => {
    const textareas = wrapper.findAll("textarea")      // a,b,c,f => 4
    const checkboxes = wrapper.findAll("input.form-check-input") // d => 1
    expect(textareas.length).toBe(4)
    expect(checkboxes.length).toBe(1)
  }) 

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

  test("triggers save function on click of save button", async () => {
    const buttons = wrapper.findAll("button.btn.btn-sm.btn-danger.bg-danger");
    buttons[0].trigger("click");
    await wrapper.vm.$nextTick();
    expect(cancelMock).toHaveBeenCalled();
  });
});
