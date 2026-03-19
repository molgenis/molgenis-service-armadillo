import { mount, flushPromises } from "@vue/test-utils";
import RemoteFile from "@/components/RemoteFile.vue";
import { getFileDetail } from "@/api/api";
import type { VueWrapper } from '@vue/test-utils';
import type RemoteFileComponent from '@/components/RemoteFile.vue';

// Mocks
jest.mock("@/api/api", () => ({
  getFileDetail: jest.fn(),
}));

const mockFile = {
  id: "123",
  page_num: 0,
  content: "line1\nline2\nline3_FAILURE",
  fetched: "Last reload: 10240",
  content_type: "text/plain",
};

describe("RemoteFile.vue", () => {

  let wrapper: VueWrapper<InstanceType<typeof RemoteFileComponent>>;

  beforeEach(async () => {
    (getFileDetail as jest.Mock).mockResolvedValue(mockFile);

    wrapper = mount(RemoteFile, {
      props: {
        fileId: "123",
        reloadFile: false,
      },
      global: {
        stubs: {
          LoadingSpinner: true,
          SearchBar: true,
          LogLine: true,
          ShowSwitch: true,
          NavigationButtons: true,
          PageSorter: true,
        },
      },
    });

    await flushPromises();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders loading spinner when file is not loaded", async () => {
    (getFileDetail as jest.Mock).mockResolvedValueOnce(null);
    const loadingWrapper = mount(RemoteFile, {
      props: { fileId: "123", reloadFile: false },
      global: { stubs: { LoadingSpinner: true } },
    });
    expect(loadingWrapper.findComponent({ name: "LoadingSpinner" }).exists()).toBe(true);
  });

  it("renders file info after loading", () => {
    expect(wrapper.text()).toContain("Log file size: 10.00 KB");
    expect(wrapper.text()).toContain("Last reload:");
  });

  it("renders LogLine components for each line", () => {
    const logLines = wrapper.findAllComponents({ name: "LogLine" });
    expect(logLines.length).toBe(3); // 3 lines
  });

  it("shows error message when showOnlyErrors is enabled and no error lines", async () => {
    await wrapper.vm.switchShowAll(true);
    wrapper.vm.lines = [];
    await flushPromises();
    const text = wrapper.text();
    expect(text).toContain("No lines found containing errors on page");
  });

  it("filters lines when filterValue is updated", async () => {
    await wrapper.vm.switchShowAll(true);
    await wrapper.setData({ filterValue: "FAILURE" });
    await flushPromises();

    expect(wrapper.vm.numberOfLines).toBe(1);
    const logLines = wrapper.findAllComponents({ name: "LogLine" });
    // Only matching line is shown
    expect(logLines.length).toBe(1);
  });

  it("emits resetReload on reloadFile prop change", async () => {
    await wrapper.setProps({ reloadFile: true });
    await flushPromises();

    expect(wrapper.emitted("resetReload")).toBeTruthy();
    expect(getFileDetail).toHaveBeenCalled();
  });

  it("navigates through matched lines correctly", async () => {
    await wrapper.setData({ filterValue: "FAILURE" });
    await flushPromises();

    wrapper.vm.navigate("next");
    expect(wrapper.vm.currentFocus).toBe(1); // should increment

    wrapper.vm.navigate("first");
    expect(wrapper.vm.currentFocus).toBe(0); // should go to first

    wrapper.vm.navigate("last");
    expect(wrapper.vm.currentFocus).toBe(wrapper.vm.matchedLines.length - 1);
  });

  it("calls methods when navigation buttons are clicked", async () => {
    const navButtons = wrapper.findAllComponents({ name: "NavigationButtons" });
    expect(navButtons.length).toBe(2); // top and bottom navigation

    const lastNav = navButtons[1];
    await lastNav.vm.$emit("click", "next"); // simulate next page
    expect(getFileDetail).toHaveBeenCalledTimes(1);
  });

  it("handles page navigation correctly", async () => {
    await wrapper.vm.loadMore();
    expect(wrapper.vm.file?.page_num).toBe(1);

    await wrapper.vm.decreasePageNum();
    expect(wrapper.vm.file?.page_num).toBe(0);
  });

  it("resets on fileId prop change", async () => {
    await wrapper.setProps({ fileId: "456" });
    await flushPromises();

    expect(getFileDetail).toHaveBeenCalledTimes(2);
    expect(wrapper.vm.lines.length).toBeGreaterThan(0);
  });
});
