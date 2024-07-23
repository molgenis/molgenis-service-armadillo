import { shallowMount, VueWrapper } from "@vue/test-utils";
import ColumnNamesPreview from "@/components/ColumnNamesPreview.vue";

describe("ColumnNamesPreview", () => {
  let wrapper: VueWrapper<any>;
  beforeEach(function () {
    wrapper = shallowMount(ColumnNamesPreview, {
      props: {
        columnNames: ["col1", "col2"],
        buttonName: "+ 2 more columns",
      },
    });
  });

  test("Toggle column names", () => {
    expect(wrapper.vm.isCollapsed).toBe(false)
    wrapper.vm.toggleColumnNames()
    expect(wrapper.vm.isCollapsed).toBe(true)
  });
  test("Join column names with comma", () => {
    expect(wrapper.vm.columnNamesString).toBe("col1, col2")
  });
});
