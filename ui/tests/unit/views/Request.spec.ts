import { mount, flushPromises } from "@vue/test-utils";
import Request from "@/views/Request.vue";

const validUser = "alice@example.com";
const encodedRequestData = btoa("table1|varA,varB;table2|varC");

// Prefixed with "mock" so jest's hoisting allows the factories below to reference them.
// The route params are mutable so each test can set its own before mounting.
let mockRouteParams: Record<string, string | undefined> = {};
const mockApproveRequest = jest.fn();
const mockTableExists = jest.fn();
const mockGetMetaData = jest.fn();

jest.mock("vue-router", () => ({
  useRoute: () => ({ params: mockRouteParams }),
}));

jest.mock("@/api/api", () => ({
  approveRequest: (...args: unknown[]) => mockApproveRequest(...args),
  tableExists: (...args: unknown[]) => mockTableExists(...args),
  getMetaData: (...args: unknown[]) => mockGetMetaData(...args),
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

async function mountRequest() {
  const wrapper = mount(Request, { global: { stubs: globalStubs } });
  await flushPromises(); // let onMounted + async validation run
  return wrapper;
}

describe("Request.vue", () => {
  beforeEach(() => {
    mockRouteParams = {
      user: validUser,
      apiVersion: "v1",
      requestData: encodedRequestData,
    };
    mockApproveRequest.mockReset();
    mockTableExists.mockReset();
    mockGetMetaData.mockReset();

    // Happy-path defaults: every table exists and has all requested variables
    mockTableExists.mockResolvedValue(true);
    mockGetMetaData.mockResolvedValue({ varA: {}, varB: {}, varC: {} });
  });

  describe("rendering", () => {
    it("renders the user pulled from the route params", async () => {
      const wrapper = await mountRequest();
      expect(wrapper.text()).toContain(validUser);
    });

    it("decodes base64 requestData into a table/variable structure", async () => {
      const wrapper = await mountRequest();

      expect((wrapper.vm as any).decodedRequestData).toEqual([
        { table: "table1", variables: "varA,varB" },
        { table: "table2", variables: "varC" },
      ]);
    });

    it("shows the collapsed variable count by default, then the list after uncollapsing", async () => {
      const wrapper = await mountRequest();

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
      mockRouteParams.requestData = btoa("");

      const wrapper = await mountRequest();

      expect((wrapper.vm as any).decodedRequestData).toEqual([]);
    });
  });

  describe("validation of the route params", () => {
    it("shows no error and renders the request when everything is valid", async () => {
      const wrapper = await mountRequest();

      expect((wrapper.vm as any).errorMessage).toBe("");
      expect(wrapper.find("table").exists()).toBe(true);
      expect(wrapper.find("button.btn-success").exists()).toBe(true);
    });

    it("rejects an unsupported api version", async () => {
      mockRouteParams.apiVersion = "v2";

      const wrapper = await mountRequest();

      expect((wrapper.vm as any).errorMessage).toContain(
        "Provided invalid api version: v2"
      );
      expect(wrapper.find("table").exists()).toBe(false);
      expect(wrapper.find("button.btn-success").exists()).toBe(false);
      expect(mockTableExists).not.toHaveBeenCalled();
      expect(mockGetMetaData).not.toHaveBeenCalled();
    });

    it("rejects a missing api version", async () => {
      mockRouteParams.apiVersion = undefined;

      const wrapper = await mountRequest();

      expect((wrapper.vm as any).errorMessage).toContain(
        "Provided invalid api version"
      );
      expect(wrapper.find("table").exists()).toBe(false);
    });

    it("rejects a missing user", async () => {
      mockRouteParams.user = "";

      const wrapper = await mountRequest();

      expect((wrapper.vm as any).errorMessage).toContain(
        "Request does not have a valid user"
      );
      expect(wrapper.find("table").exists()).toBe(false);
      expect(mockTableExists).not.toHaveBeenCalled();
    });

    it("rejects a user that is not an email address", async () => {
      mockRouteParams.user = "alice";

      const wrapper = await mountRequest();

      expect((wrapper.vm as any).errorMessage).toContain(
        "user [alice] is not a valid email address"
      );
      expect(wrapper.find("table").exists()).toBe(false);
      expect(wrapper.find("button.btn-success").exists()).toBe(false);
      expect(mockTableExists).not.toHaveBeenCalled();
    });

    it("rejects empty requestData", async () => {
      mockRouteParams.requestData = btoa("");

      const wrapper = await mountRequest();

      expect((wrapper.vm as any).errorMessage).toContain(
        "Request does not have a valid request data"
      );
      expect(wrapper.find("table").exists()).toBe(false);
      expect(mockTableExists).not.toHaveBeenCalled();
    });
  });

  describe("validation of the base64 requestData", () => {
    it("rejects requestData that is not valid base64", async () => {
      mockRouteParams.requestData = "%%%not-base64%%%";

      const wrapper = await mountRequest();

      expect((wrapper.vm as any).errorMessage).toContain(
        "Cannot base64 decode provided requestData."
      );
      expect((wrapper.vm as any).decodedRequestData).toEqual([]);
      expect(wrapper.find("table").exists()).toBe(false);
      expect(mockTableExists).not.toHaveBeenCalled();
    });
  });

  describe("validation of the requested data against the api", () => {
    it("checks that each table exists and fetches its metadata", async () => {
      await mountRequest();

      expect(mockTableExists).toHaveBeenCalledWith("table1", "table1.parquet");
      expect(mockTableExists).toHaveBeenCalledWith("table2", "table2.parquet");
      expect(mockGetMetaData).toHaveBeenCalledWith("table1", "table1.parquet");
      expect(mockGetMetaData).toHaveBeenCalledWith("table2", "table2.parquet");
    });

    it("splits a table path into project and object", async () => {
      mockRouteParams.requestData = btoa("myproject/some/folder/tableX|varA");
      mockGetMetaData.mockResolvedValue({ varA: {} });

      await mountRequest();

      expect(mockTableExists).toHaveBeenCalledWith(
        "myproject",
        "some/folder/tableX.parquet"
      );
      expect(mockGetMetaData).toHaveBeenCalledWith(
        "myproject",
        "some/folder/tableX.parquet"
      );
    });

    it("keeps errorMessage empty when all tables and variables exist", async () => {
      const wrapper = await mountRequest();

      expect((wrapper.vm as any).errorMessage).toBe("");
    });

    it("sets an error message when a table does not exist", async () => {
      mockTableExists.mockImplementation((_project: string, object: string) =>
        object === "table1.parquet"
          ? Promise.reject(new Error("not found"))
          : Promise.resolve(true)
      );

      const wrapper = await mountRequest();

      expect((wrapper.vm as any).errorMessage).toContain(
        "Request invalid: table [table1] doesn't exist"
      );
    });

    it("does not fetch metadata for a table that does not exist", async () => {
      mockTableExists.mockRejectedValue(new Error("not found"));

      await mountRequest();

      expect(mockGetMetaData).not.toHaveBeenCalled();
    });

    it("sets an error message when the metadata cannot be retrieved", async () => {
      mockGetMetaData.mockImplementation((_project: string, object: string) =>
        object === "table1.parquet"
          ? Promise.reject(new Error("boom"))
          : Promise.resolve({ varA: {}, varB: {}, varC: {} })
      );

      const wrapper = await mountRequest();

      expect((wrapper.vm as any).errorMessage).toContain(
        "could not retrieve metadata for table [table1]"
      );
    });

    it("sets an error message naming the table that does not exist", async () => {
      mockTableExists.mockImplementation((_project: string, object: string) =>
        object === "table2.parquet"
          ? Promise.reject(new Error("not found"))
          : Promise.resolve(true)
      );

      const wrapper = await mountRequest();

      expect((wrapper.vm as any).errorMessage).toContain(
        "table [table2] doesn't exist"
      );
    });

    it("sets an error message when a requested variable does not exist in the table", async () => {
      // varB (requested for table1) is missing from the metadata
      mockGetMetaData.mockResolvedValue({ varA: {}, varC: {} });

      const wrapper = await mountRequest();

      const message = (wrapper.vm as any).errorMessage;
      expect(message).toContain("invalid variable(s) for table [table1]");
      expect(message).toContain("[varB] does not exist");
    });

    it("does not set an error when the metadata contains extra variables", async () => {
      mockGetMetaData.mockResolvedValue({
        varA: {},
        varB: {},
        varC: {},
        varUnrequested: {},
      });

      const wrapper = await mountRequest();

      expect((wrapper.vm as any).errorMessage).toBe("");
    });
  });

  describe("approving", () => {
    it("sets approved to true and shows the confirmation dialog when Approve is clicked", async () => {
      const wrapper = await mountRequest();

      expect(wrapper.find(".confirmation-stub").exists()).toBe(false);

      await wrapper.find("button.btn-success").trigger("click");

      expect((wrapper.vm as any).approved).toBe(true);
      expect(wrapper.find(".confirmation-stub").exists()).toBe(true);
    });

    it("resets approved to false when the confirmation dialog is cancelled", async () => {
      const wrapper = await mountRequest();

      (wrapper.vm as any).approved = true;
      await wrapper.vm.$nextTick();

      (wrapper.vm as any).cancelApprove();
      await wrapper.vm.$nextTick();

      expect((wrapper.vm as any).approved).toBe(false);
    });

    it("calls approveRequest and sets a success message when approval succeeds", async () => {
      mockApproveRequest.mockResolvedValueOnce({
        ok: true,
        text: () => Promise.resolve("request-abcdefgh"),
      } as unknown as Response);
      const wrapper = await mountRequest();

      await (wrapper.vm as any).proceedApprove();
      await flushPromises();

      expect(mockApproveRequest).toHaveBeenCalledWith(
        validUser,
        (wrapper.vm as any).decodedRequestData
      );
      expect((wrapper.vm as any).successMessage).toContain(validUser);
      expect((wrapper.vm as any).successMessage).toContain("request-abcdefgh");
      expect((wrapper.vm as any).errorMessage).toBe("");
    });

    it("sets an error message when approval fails", async () => {
      mockApproveRequest.mockRejectedValueOnce("network error");
      const wrapper = await mountRequest();

      await (wrapper.vm as any).proceedApprove();
      await flushPromises();

      expect((wrapper.vm as any).errorMessage).toContain("network error");
      expect((wrapper.vm as any).successMessage).toBe("");
    });
  });
});