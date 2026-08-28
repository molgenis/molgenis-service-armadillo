import { mount, flushPromises } from "@vue/test-utils";
import Request from "@/views/Request.vue";

const encodedRequestData = btoa("table1|varA,varB;table2|varC");

jest.mock("vue-router", () => ({
  useRoute: () => ({
    params: {
      requestId: "req-123",
      user: "alice",
      requestData: encodedRequestData,
    },
  }),
}));

const approveRequestMock = jest.fn();
jest.mock("@/api/api", () => ({
  approveRequest: (...args: unknown[]) => approveRequestMock(...args),
}));

const globalStubs = {
  FeedbackMessage: {
    template: "<div class='feedback-stub' />",
    props: ["successMessage", "errorMessage"],
  },
  ConfirmationDialog: {
    template: "<div class='confirmation-stub' />",
    props: ["record", "action", "recordType"],
    emits: ["proceed", "cancel"],
  },
};

describe("Request.vue", () => {
  beforeEach(() => {
    approveRequestMock.mockReset();
  });

  it("renders requestId and user pulled from the route params", async () => {
    const wrapper = mount(Request, { global: { stubs: globalStubs } });
    await flushPromises(); // let onMounted run

    expect(wrapper.text()).toContain("req-123");
    expect(wrapper.text()).toContain("alice");
  });

  it("decodes base64 requestData into a table/variable structure", async () => {
    const wrapper = mount(Request, { global: { stubs: globalStubs } });
    await flushPromises();

    const decoded = (wrapper.vm as any).decodedRequestData;
    expect(decoded).toEqual([
      { table: "table1", variables: "varA,varB" },
      { table: "table2", variables: "varC" },
    ]);
  });

  it("shows the collapsed variable count by default, then the list after uncollapsing", async () => {
    const wrapper = mount(Request, { global: { stubs: globalStubs } });
    await flushPromises();

    // table1 has 2 variables -> collapsed view shows "2 variables"
    expect(wrapper.text()).toContain("2 variables");
    expect(wrapper.find(".variables-0").exists()).toBe(false);

    // Expand the first row
    await wrapper.findAll("button")[0].trigger("click");
    expect(wrapper.find(".variables-0").exists()).toBe(true);
    expect(wrapper.text()).toContain("varA");
    expect(wrapper.text()).toContain("varB");

    // Collapse it again
    await wrapper.findAll("button")[0].trigger("click");
    expect(wrapper.find(".variables-0").exists()).toBe(false);
  });

  it("returns an empty array from decodedRequestData when requestData is empty", async () => {
    // Re-mock the route for this single test case, then re-import the
    // component fresh so it picks up the new mock (jest.mock factories are
    // otherwise fixed for the whole file's module graph).
    jest.resetModules();
    jest.doMock("vue-router", () => ({
      useRoute: () => ({
        params: { requestId: "req-empty", user: "bob", requestData: btoa("") },
      }),
    }));
    jest.doMock("@/api/api", () => ({
      approveRequest: (...args: unknown[]) => approveRequestMock(...args),
    }));

    const { default: RequestFresh } = await import("@/views/Request.vue");
    const wrapper = mount(RequestFresh, { global: { stubs: globalStubs } });
    await flushPromises();

    expect((wrapper.vm as any).decodedRequestData).toEqual([]);
  });

  it("sets approved to true and shows the confirmation dialog when Approve is clicked", async () => {
    const wrapper = mount(Request, { global: { stubs: globalStubs } });
    await flushPromises();

    expect(wrapper.find(".confirmation-stub").exists()).toBe(false);

    await wrapper.find("button.btn-success").trigger("click");

    expect((wrapper.vm as any).approved).toBe(true);
    expect(wrapper.find(".confirmation-stub").exists()).toBe(true);
  });

  it("resets approved to false when the confirmation dialog is cancelled", async () => {
    const wrapper = mount(Request, { global: { stubs: globalStubs } });
    await flushPromises();

    (wrapper.vm as any).approved = true;
    await wrapper.vm.$nextTick();

    (wrapper.vm as any).cancelApprove();
    await wrapper.vm.$nextTick();

    expect((wrapper.vm as any).approved).toBe(false);
  });

  it("calls approveRequest and sets a success message when approval succeeds", async () => {
    approveRequestMock.mockResolvedValueOnce({});
    const wrapper = mount(Request, { global: { stubs: globalStubs } });
    await flushPromises();

    await (wrapper.vm as any).proceedApprove();
    await flushPromises();

    expect(approveRequestMock).toHaveBeenCalledWith(
      "alice",
      "req-123",
      (wrapper.vm as any).decodedRequestData
    );
    expect((wrapper.vm as any).successMessage).toContain("req-123");
    expect((wrapper.vm as any).successMessage).toContain("alice");
    expect((wrapper.vm as any).errorMessage).toBe("");
  });

  it("sets an error message when approval fails", async () => {
    approveRequestMock.mockRejectedValueOnce("network error");
    const wrapper = mount(Request, { global: { stubs: globalStubs } });
    await flushPromises();

    await (wrapper.vm as any).proceedApprove();
    await flushPromises();

    expect((wrapper.vm as any).errorMessage).toContain("network error");
    expect((wrapper.vm as any).successMessage).toBe("");
  });
});