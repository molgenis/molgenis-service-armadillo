import { mount } from "@vue/test-utils";
import ButtonGroup from "@/components/ButtonGroup.vue";

const mockFunction = jest.fn();
const wrapper = mount(ButtonGroup, {
  props: {
    buttonIcons: ["pencil-fill", "trash-fill", "1-circle"],
    buttonColors: ["danger", "info", "light"],
    clickCallbacks: [mockFunction, mockFunction, mockFunction],
    callbackArguments: ["hello", undefined, undefined],
  },
});

describe("ButtonGroup", () => {
  test("triggers function on click of button", () => {
    const buttons = wrapper.findAll("button");
    buttons[0].trigger("click");
    expect(mockFunction).toHaveBeenCalledWith("hello");
  });

  test("creates buttons with correct text and background colors", () => {
    const buttons = wrapper.findAll("button");
    expect(buttons.length).toBe(3);
    expect(buttons[0].attributes("class")).toContain("bg-danger");
    expect(buttons[0].attributes("class")).toContain("btn-danger");
    expect(buttons[1].attributes("class")).toContain("bg-info");
    expect(buttons[1].attributes("class")).toContain("btn-primary");
    expect(buttons[2].attributes("class")).toContain("bg-light");
    expect(buttons[2].attributes("class")).toContain("btn-light");
  });

  test("creates buttons with correct icons", () => {
    const icons = wrapper.findAll("i");
    expect(icons.length).toBe(3);
    expect(icons[0].attributes("class")).toContain("bi-pencil-fill");
    expect(icons[1].attributes("class")).toContain("bi-trash-fill");
    expect(icons[2].attributes("class")).toContain("bi-1-circle");
  });
});
