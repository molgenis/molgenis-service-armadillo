import { shallowMount, VueWrapper } from "@vue/test-utils";
import MetaDataPreview from "@/components/MetaDataPreview.vue";

describe("MetaDataPreview", () => {
  let wrapper: VueWrapper<any>;

  beforeEach(() => {
    wrapper = shallowMount(MetaDataPreview, {
      props: {
        metadata: {
          col1: { type: "STRING", missing: "2/10", levels: [] },
          col2: { type: "BINARY", missing: "1/5", levels: ["Yes", "No"] },
        },
      },
    });
  });

  test("Toggle missing filter operator", () => {
    expect(wrapper.vm.missingFilter).toBe(">");
    wrapper.vm.toggleMissingFilter();
    expect(wrapper.vm.missingFilter).toBe("<");
    wrapper.vm.toggleMissingFilter();
    expect(wrapper.vm.missingFilter).toBe(">");
  });

  test("Get percentage of missing correctly", () => {
    expect(wrapper.vm.getPercentageOfMissing("2/10")).toBe(20);
    expect(wrapper.vm.getPercentageOfMissing("0/5")).toBe(0);
    expect(wrapper.vm.getPercentageOfMissing("1/4")).toBe(25);
  });

  test("Get emx data type of the parquet equivalent", () => {
    expect(wrapper.vm.getEmxDataType("BINARY", undefined)).toBe("String");
    expect(wrapper.vm.getEmxDataType("BINARY", ["a", "b", "c"])).toBe("Categorical");
    expect(wrapper.vm.getEmxDataType("INT32", undefined)).toBe("Integer");
    expect(wrapper.vm.getEmxDataType("DOUBLE", undefined)).toBe("Decimal");
    expect(wrapper.vm.getEmxDataType("BOOLEAN", undefined)).toBe("Boolean");
    // edge case, should never happen, but in case it does, just present what it got
    expect(wrapper.vm.getEmxDataType("NOT CONVERTIBLE", undefined)).toBe("NOT CONVERTIBLE");
  });

  describe("showLine method", () => {
    test("Filter = none => always true", () => {
      wrapper.setData({ filterColumn: "none" });
      expect(wrapper.vm.showLine("col1", "STRING", "2/10")).toBe(true);
    });

    test("Filter = column => matches column name", () => {
      wrapper.setData({ filterColumn: "column", columnFilter: "col1" });
      expect(wrapper.vm.showLine("col1", "STRING", "2/10")).toBe(true);
      expect(wrapper.vm.showLine("col2", "BINARY", "1/5")).toBe(false);
    });

    test("Filter = datatype => matches selected type", () => {
      wrapper.setData({ filterColumn: "datatype", selectedType: "BINARY" });
      expect(wrapper.vm.showLine("col2", "BINARY", "1/5")).toBe(true);
      expect(wrapper.vm.showLine("col1", "STRING", "2/10")).toBe(false);
    });

    test("Filter = missing => percentage meets filter criteria (>)", () => {
      wrapper.setData({ filterColumn: "missing", missingFilter: ">", missingFilterValue: 15 });
      expect(wrapper.vm.showLine("col1", "STRING", "2/10")).toBe(true); // 20%
      expect(wrapper.vm.showLine("col2", "BINARY", "1/5")).toBe(true); // 20%
    });

    test("Filter = missing => percentage meets filter criteria (<)", () => {
      wrapper.setData({ filterColumn: "missing", missingFilter: "<", missingFilterValue: 25 });
      expect(wrapper.vm.showLine("col1", "STRING", "2/10")).toBe(true); // 20%
      expect(wrapper.vm.showLine("col2", "BINARY", "1/5")).toBe(true); // 20%
    });

    test("Filter = missing => missingFilterValue is null => always true", () => {
      wrapper.setData({ filterColumn: "missing", missingFilterValue: null });
      expect(wrapper.vm.showLine("col1", "STRING", "2/10")).toBe(true);
    });
  });
});
