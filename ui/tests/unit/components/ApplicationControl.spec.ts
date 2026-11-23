import { mount, flushPromises } from "@vue/test-utils";
import { defineComponent } from "vue";
import ApplicationControl from "@/components/ApplicationControl.vue";

// ---------------------------------------------------------------------------
// Mock the downloadJar API so network calls never go out
// ---------------------------------------------------------------------------
const mockSource = {
  listeners: {} as Record<string, ((e: { data: string }) => void)[]>,
  addEventListener(event: string, cb: (e: any) => void) {
    if (!this.listeners[event]) this.listeners[event] = [];
    this.listeners[event].push(cb);
  },
  close: jest.fn()
};

jest.mock("@/api/api", () => ({
  downloadJar: jest.fn(() => mockSource),
}));

import { downloadJar } from "@/api/api";

// ---------------------------------------------------------------------------
// Helper factory
// ---------------------------------------------------------------------------
function createWrapper(propsOverride = {}) {
  return mount(ApplicationControl, {
    props: {
      currentReleaseVersion: "5.0.0",
      latestReleaseVersion: "5.0.0",
      latestVersionDownloaded: false,
      appList: [],
      ...propsOverride,
    }
  });
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------
describe("ApplicationControl", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Reset mock source listeners between tests
    mockSource.listeners = {};
    mockSource.close.mockClear();
  });

  // ── Computed: isUpdateAvailable ─────────────────────────────────────────
  describe("isUpdateAvailable", () => {
    it("is false when current and latest versions match", () => {
      const wrapper = createWrapper({
        currentReleaseVersion: "5.0.0",
        latestReleaseVersion: "5.0.0",
      });
      expect(wrapper.vm.isUpdateAvailable).toBe(false);
    });

    it("is true when versions differ", () => {
      const wrapper = createWrapper({
        currentReleaseVersion: "5.0.0",
        latestReleaseVersion: "5.1.0",
      });
      expect(wrapper.vm.isUpdateAvailable).toBe(true);
    });
  });

  // ── Update status alerts ────────────────────────────────────────────────
  describe("update status display", () => {
    it("shows 'running latest version' alert when up to date", () => {
      const wrapper = createWrapper();
      expect(wrapper.text()).toContain("Running latest version");
      expect(wrapper.text()).toContain("5.0.0");
    });

    it("shows 'update available' alert and Download button when not downloaded", () => {
      const wrapper = createWrapper({
        currentReleaseVersion: "5.0.0",
        latestReleaseVersion: "5.1.0",
        latestVersionDownloaded: false,
      });
      expect(wrapper.text()).toContain("Update available");
      expect(wrapper.text()).toContain("5.1.0");
      expect(wrapper.find("button.btn-primary").text()).toContain(
        "Download update"
      );
    });

    it("shows 'Update now' button when latest version is already downloaded", () => {
      const wrapper = createWrapper({
        currentReleaseVersion: "5.0.0",
        latestReleaseVersion: "5.1.0",
        latestVersionDownloaded: true,
      });
      expect(wrapper.find("button.btn-success").text()).toContain("Update now");
    });
  });

  // ── Update now button ───────────────────────────────────────────────────
  describe("'Update now' button", () => {
    it("emits update-app with the latest version", async () => {
      const wrapper = createWrapper({
        currentReleaseVersion: "5.0.0",
        latestReleaseVersion: "5.1.0",
        latestVersionDownloaded: true,
      });
      await wrapper.find("button.btn-success").trigger("click");
      expect(wrapper.emitted("update-app")).toEqual([["5.1.0"]]);
    });
  });

  // ── Download latest ─────────────────────────────────────────────────────
  describe("downloadLatest()", () => {
    it("calls downloadJar with the latest release version", async () => {
      const wrapper = createWrapper({
        currentReleaseVersion: "5.0.0",
        latestReleaseVersion: "5.1.0",
        latestVersionDownloaded: false,
      });
      await wrapper.find("button.btn-primary").trigger("click");
      expect(downloadJar).toHaveBeenCalledWith("5.1.0");
    });

    it("does nothing if latestReleaseVersion is undefined", async () => {
      const wrapper = createWrapper({
        currentReleaseVersion: "5.0.0",
        latestReleaseVersion: undefined,
        latestVersionDownloaded: false,
      });
      // No 'Update available' UI, so call the method directly
      wrapper.vm.downloadLatest();
      expect(downloadJar).not.toHaveBeenCalled();
    });
  });

  // ── Restart buttons ─────────────────────────────────────────────────────
  describe("restart buttons", () => {
    it("emits soft-restart-pushed on soft restart click", async () => {
      const wrapper = createWrapper();
      const buttons = wrapper.findAll("button.btn-warning");
      const softBtn = buttons.find((b) => b.text().includes("Soft restart"));
      await softBtn!.trigger("click");
      expect(wrapper.emitted("soft-restart-pushed")).toHaveLength(1);
    });

    it("emits hard-restart-pushed on hard restart click", async () => {
      const wrapper = createWrapper();
      const buttons = wrapper.findAll("button.btn-warning");
      const hardBtn = buttons.find((b) => b.text().includes("Hard"));
      await hardBtn!.trigger("click");
      expect(wrapper.emitted("hard-restart-pushed")).toHaveLength(1);
    });

    it("toggles restart info panel on info button click", async () => {
      const wrapper = createWrapper();
      expect(wrapper.find(".alert.alert-info").exists()).toBe(false);
      const infoBtn = wrapper.find("button.btn-link.text-info");
      await infoBtn.trigger("click");
      expect(wrapper.find(".alert.alert-info").exists()).toBe(true);
      // Click again to hide
      await wrapper.find("button.btn-link.text-secondary").trigger("click");
      expect(wrapper.find(".alert.alert-info").exists()).toBe(false);
    });
  });

  // ── Advanced update panel ───────────────────────────────────────────────
  describe("advanced update panel", () => {
    it("is collapsed by default", () => {
      const wrapper = createWrapper();
      expect(wrapper.find(".card .card-body .card").exists()).toBe(false);
    });

    it("expands when the toggle button is clicked", async () => {
      const wrapper = createWrapper();
      await wrapper.find("button.btn-outline-primary").trigger("click");
      expect(wrapper.find(".card .card-body .card").exists()).toBe(true);
    });

    it("collapses again on a second click", async () => {
      const wrapper = createWrapper();
      const toggle = wrapper.find("button.btn-outline-primary");
      await toggle.trigger("click");
      await toggle.trigger("click");
      expect(wrapper.find(".card .card-body .card").exists()).toBe(false);
    });
  });

  // ── isValidVersion ──────────────────────────────────────────────────────
  describe("isValidVersion()", () => {
    let wrapper: ReturnType<typeof createWrapper>;
    beforeEach(() => {
      wrapper = createWrapper();
    });

    it.each([
      ["5.12.2", true],
      ["0.0.1", true],
      ["v5.12.2", true],
      ["5.12", false],
      ["abc", false],
      ["", false],
    ])("returns %s for '%s'", (version, expected) => {
      expect(wrapper.vm.isValidVersion(version)).toBe(expected);
    });
  });

  // ── downloadVersion (advanced panel) ───────────────────────────────────
  describe("downloadVersion() in advanced panel", () => {
    async function openAdvanced(wrapper: ReturnType<typeof createWrapper>) {
      await wrapper.find("button.btn-outline-primary").trigger("click");
    }

    it("downloads when version is valid", async () => {
      const wrapper = createWrapper();
      await openAdvanced(wrapper);
      // Set the value that the FormInput ref exposes
      (wrapper.vm.$refs.versionInput as any).mappedValue = "5.2.0";
      await wrapper.find("button.btn-outline-dark, button.btn-primary").trigger;
      // Trigger via method directly to avoid DOM complexity with refs
      wrapper.vm.downloadVersion();
      expect(downloadJar).toHaveBeenCalledWith("5.2.0");
    });

    it("emits error when version is invalid", async () => {
      const wrapper = createWrapper();
      await openAdvanced(wrapper);
      (wrapper.vm.$refs.versionInput as any).mappedValue = "not-a-version";
      wrapper.vm.downloadVersion();
      expect(wrapper.emitted("error")).toBeTruthy();
      expect((wrapper.emitted("error")![0][0] as string)).toContain(
        "not a valid version"
      );
      expect(downloadJar).not.toHaveBeenCalled();
    });
  });

  // ── selectUpdateVersion ─────────────────────────────────────────────────
  describe("selectUpdateVersion()", () => {
    it("strips jar filename prefix and suffix from the selected value", () => {
      const wrapper = createWrapper({ appList: ["molgenis-armadillo-5.2.0.jar"] });
      wrapper.vm.selectUpdateVersion("molgenis-armadillo-5.2.0.jar" as any);
      expect(wrapper.vm.updateVersion).toBe("5.2.0");
    });
  });
});