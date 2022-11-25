import { DOMWrapper, shallowMount, VueWrapper } from "@vue/test-utils";
import FileUpload from "@/components/FileUpload.vue";
import * as _api from "@/api/api";

const api = _api as any;
jest.mock("@/api/api");

describe("FileUpload", () => {
  let localImageInput: DOMWrapper<Element>;
  let localImageInputFilesGet: jest.Mock<any, any>;
  let localImageInputValueGet;
  let localImageInputValueSet;
  let localImageInputValue = "some-image.gif";
  let wrapper: VueWrapper<any>;
  let mockFiles: {
    size: number;
    blob: string;
    width: number;
    height: number;
  }[];

  // https://stackoverflow.com/questions/48993134/how-to-test-input-file-with-jest-and-vue-test-utils
  beforeEach(function () {
    wrapper = shallowMount(FileUpload, {
      props: {
        object: "testObject",
        project: "molgenius",
        uniqueClass: "isThisUniqueEnough",
      },
    });
    localImageInput = wrapper.find("input.isThisUniqueEnough");
    localImageInputFilesGet = jest.fn();
    localImageInputValueGet = jest.fn().mockReturnValue(localImageInputValue);
    localImageInputValueSet = jest.fn().mockImplementation((v) => {
      localImageInputValue = v;
    });

    Object.defineProperty(localImageInput.element, "files", {
      // https://stackoverflow.com/questions/25517989/why-cant-i-redefine-a-property-in-a-javascript-object
      configurable: true,
      get: localImageInputFilesGet,
    });

    Object.defineProperty(localImageInput.element, "value", {
      configurable: true,
      get: localImageInputValueGet,
      set: localImageInputValueSet,
    });

    mockFiles = [
      {
        size: 12345,
        blob: "some-blob",
        width: 300,
        height: 200,
      },
    ];
  });

  test("emits event when files empty", () => {
    wrapper.find("input.isThisUniqueEnough").trigger("change");
    // test if event emitted
    expect(wrapper.emitted()).toHaveProperty("upload_error");
    expect(wrapper.emitted("upload_error")).toEqual([["Please select a file"]]);
  });

  test("emits event on upload success", async () => {
    api.uploadIntoProject.mockImplementation(() => {
      return Promise.resolve({});
    });
    localImageInputFilesGet.mockReturnValue(mockFiles);
    localImageInput.trigger("change");
    await wrapper.vm.$nextTick();
    //test if upload function called
    expect(api.uploadIntoProject).toHaveBeenCalled();
    // test if event emitted
    expect(wrapper.emitted()).toHaveProperty("upload_success");
  });

  test("emits event on upload fail", async () => {
    const error = new Error("fail");
    api.uploadIntoProject.mockImplementation(() => {
      return Promise.reject(error);
    });
    localImageInputFilesGet.mockReturnValue(mockFiles);
    localImageInput.trigger("change");
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    //test if upload function called
    expect(api.uploadIntoProject).toHaveBeenCalled();
    // test if event emitted
    expect(wrapper.emitted()).toHaveProperty("upload_error");
    expect(wrapper.emitted("upload_error")).toEqual([[error]]);
  });
});