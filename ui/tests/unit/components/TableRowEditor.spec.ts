import { mount } from "@vue/test-utils";
import TableRowEditor from "@/components/TableRowEditor.vue";

describe("TableRowEditor", () => {
  const emptyFunction = function(){};
  const addMock = jest.fn();
  const saveMock = jest.fn();

  const wrapper = mount(TableRowEditor, {
    props: {
      rowToEdit: {
        firstName: "Bofke",
        lastName: "Molgenis",
        institution: "UMCG",
        admin: false,
        projects: ["project1", "project2", "project3"],
      },
      addArrayElementToRow: true,
      arrayColumn: "projects",
      saveCallback: emptyFunction,
      cancelCallback: emptyFunction,
      deleteArrayElementCallback: emptyFunction,
      addArrayElementCallback: addMock,
      saveArrayElementCallback: saveMock,
    },
  });

  test("Row is correctly loaded", () => {
    expect(wrapper.html()).toContain("Bofke");
    expect(wrapper.html()).toContain("Molgenis");
    expect(wrapper.html()).toContain("UMCG");
    expect(wrapper.html()).toContain("checkbox");
  });

  test("triggers save function on click of save button", async () => {
    const saveButton = wrapper.findAll("button.check-badge.text-light.bg-secondary");
    saveButton[0].trigger("click");
    expect(saveMock).toHaveBeenCalled();
  });

  test("triggers add function on click of add button", () => {
    const addButton = wrapper.findAll("button.btn.btn-primary.btn-sm.float-end");
    addButton[0].trigger("click");
    expect(addMock).toHaveBeenCalled();
  });
});
