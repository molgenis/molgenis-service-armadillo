import { shallowMount, VueWrapper } from "@vue/test-utils";
import ViewEditor from "@/components/ViewEditor.vue";

describe("ViewEditor", () => {
  let wrapper: VueWrapper<any>;
  const testFunction = jest.fn()
  beforeEach(function () {
    wrapper = shallowMount(ViewEditor, {
      props: {
        onSave: testFunction
      },
    });
  });

  test("linkedObject", () => {
      wrapper.vm.vwFolder = "my-folder";
      wrapper.vm.vwTable = "my-table";
      expect(wrapper.vm.linkedObject).toBe("my-folder/my-table");
  });
  test("sourceObject", () => {
    wrapper.vm.srcFolder = "my-folder";
    wrapper.vm.srcTable = "my-table.parquet";
    expect(wrapper.vm.sourceObject).toBe("my-folder/my-table");
  });
  
  test("saveIfValid doesnt save if file info data not set", () => {
    wrapper.vm.saveIfValid(false, ["a", "b", "c"]);
    expect(wrapper.vm.formValidated).toBe(true);
    expect(testFunction).not.toHaveBeenCalled();
  })

  test("saveIfValid doesnt save if variables not set", () => {
    wrapper.vm.saveIfValid(true, []);
    expect(wrapper.vm.formValidated).toBe(true);
    expect(testFunction).not.toHaveBeenCalled();
  })

  test("saveIfValid does save if all view data set", () => {
    const srcProject = "project";
    const srcFolder = "folder";
    const srcTable = "table";
    const vwProject = "link-project";
    const vwFolder = "link-folder";
    const vwTable = "filename";
    const variables = ["a", "b", "c"];
    wrapper.vm.srcProject = srcProject;
    wrapper.vm.srcFolder = srcFolder;
    wrapper.vm.srcTable = srcTable;
    wrapper.vm.vwProject = vwProject;
    wrapper.vm.vwFolder = vwFolder;
    wrapper.vm.vwTable = vwTable;
    wrapper.vm.saveIfValid(true, variables);
    expect(testFunction).toHaveBeenCalledWith(srcProject, srcFolder+"/"+ srcTable, vwProject,vwFolder+"/"+ vwTable, variables);
  })

  test("allFileInformationProvided true when all projects, folders and tables set", () => {
    const srcProject = "project";
    const srcFolder = "folder";
    const srcTable = "table";
    const vwProject = "link-project";
    const vwFolder = "link-folder";
    const vwTable = "filename";
    wrapper.vm.srcProject = srcProject;
    wrapper.vm.srcFolder = srcFolder;
    wrapper.vm.srcTable = srcTable;
    wrapper.vm.vwProject = vwProject;
    wrapper.vm.vwFolder = vwFolder;
    wrapper.vm.vwTable = vwTable;
    expect(wrapper.vm.allFileInformationProvided).toBe(true);
  })

  test("allFileInformationProvided false when all srcProject not set", () => {
    const srcFolder = "folder";
    const srcTable = "table";
    const vwProject = "link-project";
    const vwFolder = "link-folder";
    const vwTable = "filename";
    wrapper.vm.srcFolder = srcFolder;
    wrapper.vm.srcTable = srcTable;
    wrapper.vm.vwProject = vwProject;
    wrapper.vm.vwFolder = vwFolder;
    wrapper.vm.vwTable = vwTable;
    expect(wrapper.vm.allFileInformationProvided).toBe(false);
  })

  test("allFileInformationProvided false when all srcFolder not set", () => {
    const srcProject = "project";
    const srcTable = "table";
    const vwProject = "link-project";
    const vwFolder = "link-folder";
    const vwTable = "filename";
    wrapper.vm.srcProject = srcProject;
    wrapper.vm.srcTable = srcTable;
    wrapper.vm.vwProject = vwProject;
    wrapper.vm.vwFolder = vwFolder;
    wrapper.vm.vwTable = vwTable;
    expect(wrapper.vm.allFileInformationProvided).toBe(false);
  })

  test("allFileInformationProvided false when all srcTable not set", () => {
    const srcProject = "project";
    const srcFolder = "folder";
    const vwProject = "link-project";
    const vwFolder = "link-folder";
    const vwTable = "filename";
    wrapper.vm.srcProject = srcProject;
    wrapper.vm.srcFolder = srcFolder;
    wrapper.vm.vwProject = vwProject;
    wrapper.vm.vwFolder = vwFolder;
    wrapper.vm.vwTable = vwTable;
    expect(wrapper.vm.allFileInformationProvided).toBe(false);
  })

  test("allFileInformationProvided false when all vwProject not set", () => {
    const srcProject = "project";
    const srcFolder = "folder";
    const srcTable = "table";
    const vwFolder = "link-folder";
    const vwTable = "filename";
    wrapper.vm.srcProject = srcProject;
    wrapper.vm.srcFolder = srcFolder;
    wrapper.vm.srcTable = srcTable;
    wrapper.vm.vwFolder = vwFolder;
    wrapper.vm.vwTable = vwTable;
    expect(wrapper.vm.allFileInformationProvided).toBe(false);
  })

  test("allFileInformationProvided false when all vwFolder not set", () => {
    const srcProject = "project";
    const srcFolder = "folder";
    const srcTable = "table";
    const vwProject = "link-project";
    const vwTable = "filename";
    wrapper.vm.srcProject = srcProject;
    wrapper.vm.srcFolder = srcFolder;
    wrapper.vm.srcTable = srcTable;
    wrapper.vm.vwProject = vwProject;
    wrapper.vm.vwTable = vwTable;
    expect(wrapper.vm.allFileInformationProvided).toBe(false);
  })

  test("allFileInformationProvided false when all vwTable not set", () => {
    const srcProject = "project";
    const srcFolder = "folder";
    const srcTable = "table";
    const vwProject = "link-project";
    const vwFolder = "link-folder";
    wrapper.vm.srcProject = srcProject;
    wrapper.vm.srcFolder = srcFolder;
    wrapper.vm.srcTable = srcTable;
    wrapper.vm.vwProject = vwProject;
    wrapper.vm.vwFolder = vwFolder;
    expect(wrapper.vm.allFileInformationProvided).toBe(false);
  })
});
 