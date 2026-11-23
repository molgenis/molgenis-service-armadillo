import { mount } from "@vue/test-utils";
import { defineComponent } from "vue";
import Storage from "@/components/Storage.vue";

// Mock child component to isolate Storage unit tests
jest.mock("@/components/DiskSpace.vue", () => ({
  __esModule: true,
  default: defineComponent({
    name: "DiskSpace",
    props: ["used", "percentage", "total"],
    template: '<div data-testid="disk-space" />',
  }),
}));

// Mock helpers
jest.mock("@/helpers/utils", () => ({
  convertBytes: (bytes: number) => `${bytes}B`,
  getVersionFromJar: (jar: string) => jar.replace("armadillo-", "").replace(".jar", ""),
}));

const DEFAULT_PROPS = {
  appList: ["armadillo-1.0.0.jar", "armadillo-1.1.0.jar", "armadillo.jar"],
  currentVersion: "1.1.0",
  freeDiskSpace: 500_000_000,
  totalDiskSpace: 1_000_000_000,
};

function mountStorage(props = {}) {
  return mount(Storage, {
    props: { ...DEFAULT_PROPS, ...props },
  });
}

describe("Storage.vue", () => {
  describe("rendering", () => {
    it("renders the card header with Storage title", () => {
      const wrapper = mountStorage();
      expect(wrapper.find(".card-header").text()).toContain("Storage");
    });

    it("renders the DiskSpace component when freeDiskSpace and totalDiskSpace are provided", () => {
      const wrapper = mountStorage();
      expect(wrapper.find('[data-testid="disk-space"]').exists()).toBe(true);
    });

    it("does not render DiskSpace when freeDiskSpace is absent", () => {
      const wrapper = mountStorage({ freeDiskSpace: undefined });
      expect(wrapper.find('[data-testid="disk-space"]').exists()).toBe(false);
    });

    it("does not render DiskSpace when totalDiskSpace is absent", () => {
      const wrapper = mountStorage({ totalDiskSpace: undefined });
      expect(wrapper.find('[data-testid="disk-space"]').exists()).toBe(false);
    });
  });

  describe("filteredApps", () => {
    it("excludes 'armadillo.jar' from the app list", () => {
      const wrapper = mountStorage();
      const labels = wrapper.findAll(".form-check-label").map((el) => el.text());
      expect(labels.some((l) => l.includes("armadillo.jar"))).toBe(false);
    });

    it("renders a radio button for each filtered app", () => {
      const wrapper = mountStorage();
      // appList has 3 items; armadillo.jar is filtered out → 2 radios
      expect(wrapper.findAll('input[type="radio"]')).toHaveLength(2);
    });

    it("renders all apps when none are named 'armadillo.jar'", () => {
      const wrapper = mountStorage({
        appList: ["armadillo-1.0.0.jar", "armadillo-2.0.0.jar"],
      });
      expect(wrapper.findAll('input[type="radio"]')).toHaveLength(2);
    });
  });

  describe("currently running badge", () => {
    it("shows 'Currently running' badge for the current version", () => {
      const wrapper = mountStorage();
      const badges = wrapper.findAll(".badge");
      expect(badges).toHaveLength(1);
      expect(badges[0].text()).toContain("Currently running");
    });

    it("disables the radio input for the currently running version", () => {
      const wrapper = mountStorage();
      const radios = wrapper.findAll('input[type="radio"]');
      const disabledRadio = radios.find((r) => (r.element as HTMLInputElement).disabled);
      expect(disabledRadio).toBeDefined();
      expect((disabledRadio!.element as HTMLInputElement).value).toBe("armadillo-1.1.0.jar");
    });

    it("does not disable radios that are not the current version", () => {
      const wrapper = mountStorage();
      const radios = wrapper.findAll('input[type="radio"]');
      const enabledRadios = radios.filter((r) => !(r.element as HTMLInputElement).disabled);
      expect(enabledRadios).toHaveLength(1);
      expect((enabledRadios[0].element as HTMLInputElement).value).toBe("armadillo-1.0.0.jar");
    });
  });

  describe("delete button", () => {
    it("is disabled when appList has fewer than 2 items", () => {
      const wrapper = mountStorage({ appList: ["armadillo-1.0.0.jar"] });
      const btn = wrapper.find(".btn-danger");
      expect((btn.element as HTMLButtonElement).disabled).toBe(true);
    });

    it("is enabled when appList has 2 or more items", () => {
      const wrapper = mountStorage();
      const btn = wrapper.find(".btn-danger");
      expect((btn.element as HTMLButtonElement).disabled).toBe(false);
    });

    it("emits 'triggerDelete' with the selected app when delete is clicked", async () => {
      const wrapper = mountStorage();

      // Select the first non-disabled radio
      const enabledRadio = wrapper
        .findAll('input[type="radio"]')
        .find((r) => !(r.element as HTMLInputElement).disabled)!;

      await enabledRadio.setValue((enabledRadio.element as HTMLInputElement).value);

      await wrapper.find(".btn-danger").trigger("click");

      expect(wrapper.emitted("triggerDelete")).toBeTruthy();
      expect(wrapper.emitted("triggerDelete")![0]).toEqual(["armadillo-1.0.0.jar"]);
    });
  });

  describe("diskspace computed property", () => {
    it("passes correct used, total and percentage props to DiskSpace", () => {
      const wrapper = mountStorage({
        totalDiskSpace: 1_000_000_000,
        freeDiskSpace: 500_000_000,
      });

      const diskSpace = wrapper.findComponent({ name: "DiskSpace" });
      // used = 500_000_000, convertBytes mocked as `${n}B`
      expect(diskSpace.props("used")).toBe("500000000B");
      expect(diskSpace.props("total")).toBe("1000000000B");
      expect(diskSpace.props("percentage")).toBeCloseTo(50);
    });

    it("returns empty strings and 0% when disk space props are missing", () => {
      const wrapper = mountStorage({
        freeDiskSpace: undefined,
        totalDiskSpace: undefined,
      });
      const vm = wrapper.vm as InstanceType<typeof Storage>;
      expect(vm.diskspace).toEqual({ total: "", free: "", used: "", percentage: 0 });
    });
  });
});