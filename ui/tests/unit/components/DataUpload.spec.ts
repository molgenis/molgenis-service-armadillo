import { mount, VueWrapper } from "@vue/test-utils";
import DataUpload from "@/components/DataUpload.vue";
import * as _api from "@/api/api";

const api = _api as any;
jest.mock("@/api/api");

describe("DataUpload", () => {
  let wrapper: VueWrapper<any>;

  beforeEach(() => {
    jest.clearAllMocks();

    wrapper = mount(DataUpload, {
      props: {
        object: "obj1",
        project: "project1",
        uniqueClass: "uploadInput",
        triggerUpload: false,
      },
    });
  });

  async function selectFile(file: { name: string; size: number }) {
  wrapper.vm.$refs.fileUpload.file = file;
  wrapper.vm.$forceUpdate();
  await wrapper.vm.$nextTick();
}
  test("does not show parquet options when no file selected", () => {
    expect(wrapper.find("#csv-checkbox").exists()).toBe(false);
  });

  test("shows parquet checkbox for csv files", async () => {
    await selectFile({ name: "data.csv", size: 10 });
    expect(wrapper.find("#csv-checkbox").exists()).toBe(true);
  });

  test("shows parquet checkbox for tsv files", async () => {
    await selectFile({ name: "data.tsv", size: 10 });
    expect(wrapper.find("#csv-checkbox").exists()).toBe(true);
  });

  test("does not show parquet checkbox for other file types", async () => {
    await selectFile({ name: "data.txt", size: 10 });
    expect(wrapper.find("#csv-checkbox").exists()).toBe(false);
  });

  test("shows typeRows input when uploadCsvAsParquet is checked (default)", async () => {
    await selectFile({ name: "data.csv", size: 10 });
    expect(wrapper.vm.uploadCsvAsParquet).toBe(true);
    expect(wrapper.find("#typeRows").exists()).toBe(true);
  });

  test("hides typeRows input when uploadCsvAsParquet is unchecked", async () => {
    await selectFile({ name: "data.csv", size: 10 });

    wrapper.vm.uploadCsvAsParquet = false;
    await wrapper.vm.$nextTick();

    expect(wrapper.find("#typeRows").exists()).toBe(false);
  });

  test("calls uploadCsvIntoProject with typeRows when file is csv and uploadCsvAsParquet is true", async () => {
    api.uploadCsvIntoProject.mockResolvedValue({});
    await selectFile({ name: "data.csv", size: 10 });

    wrapper.vm.typeRows = 50;
    wrapper.vm.uploadDataFile();

    expect(api.uploadCsvIntoProject).toHaveBeenCalledWith(
      wrapper.vm.file.file,
      "obj1",
      "project1",
      50
    );
  });

  test("calls uploadIntoProject when csv file but uploadCsvAsParquet is false", async () => {
    api.uploadIntoProject.mockResolvedValue({});
    await selectFile({ name: "data.csv", size: 10 });

    wrapper.vm.uploadCsvAsParquet = false;
    wrapper.vm.uploadDataFile();

    expect(api.uploadIntoProject).toHaveBeenCalledWith(
      wrapper.vm.file.file,
      "obj1",
      "project1"
    );
  });

  test("calls uploadIntoProject for non csv/tsv files", async () => {
    api.uploadIntoProject.mockResolvedValue({});
    await selectFile({ name: "image.png", size: 10 });

    wrapper.vm.uploadDataFile();

    expect(api.uploadIntoProject).toHaveBeenCalledWith(
      wrapper.vm.file.file,
      "obj1",
      "project1"
    );
    expect(api.uploadCsvIntoProject).not.toHaveBeenCalled();
  });

  test("sets isUploadingFile to true when upload starts", async () => {
    api.uploadIntoProject.mockResolvedValue({});
    await selectFile({ name: "image.png", size: 10 });

    wrapper.vm.uploadDataFile();

    expect(wrapper.vm.isUploadingFile).toBe(true);
  });

  test("emits upload_success with filename", async () => {
    await selectFile({ name: "image.png", size: 10 });

    wrapper.vm.emitSuccess();

    expect(wrapper.emitted()).toHaveProperty("upload_success");
    expect(wrapper.emitted("upload_success")).toEqual([
      [{ filename: "image.png" }],
    ]);
  });

  test("emits upload_error with message", () => {
    wrapper.vm.emitError("Something went wrong");

    expect(wrapper.emitted()).toHaveProperty("upload_error");
    expect(wrapper.emitted("upload_error")).toEqual([
      ["Something went wrong"],
    ]);
  });

  test("getFileName returns empty string when no file selected", () => {
    expect(wrapper.vm.getFileName()).toBe("");
  });

  test("dragover prevents default", () => {
    const event = { preventDefault: jest.fn() };
    wrapper.vm.dragover(event as unknown as Event);
    expect(event.preventDefault).toHaveBeenCalled();
  });
});