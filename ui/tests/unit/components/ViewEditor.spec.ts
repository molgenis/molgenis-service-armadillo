import { shallowMount, VueWrapper } from "@vue/test-utils";
import ViewEditor from "@/components/ViewEditor.vue";

describe("ViewEditor", () => {
  let wrapper: VueWrapper<any>;
  beforeEach(function () {
    wrapper = shallowMount(ViewEditor, {
      props: {

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
  
});
