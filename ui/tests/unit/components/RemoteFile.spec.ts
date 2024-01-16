import { DOMWrapper, shallowMount, VueWrapper } from "@vue/test-utils";
import RemoteFile from "@/components/RemoteFile.vue";
import * as _api from "@/api/api";
import { nextTick } from "vue";

const api = _api as any;

jest.mock('@/api/api', () => ({
    getFileDetail: jest.fn().mockImplementation((fileId) => Promise.resolve({
        id: fileId,
        name: 'test',
        timestamp: '2024-01-03T15:39:56Z',
        content: 'test content',
      })),
    getFileDownload: jest.fn().mockImplementation(() => Promise.resolve({
        blob: () => Promise.resolve(new Blob()),
      })),
  }));

describe("RemoteFile", () => {
    let wrapper: VueWrapper<any>;
    beforeEach(function () {

    });

    test("something", () => {

    });

    it('renders the component', () => {
        const wrapper = shallowMount(RemoteFile, {
          props: {
            fileId: '123',
          },
        });
        expect(wrapper.exists()).toBe(true);
    });
    
    it('calls fetchFile when fileId changes', async () => {
        const wrapper = shallowMount(RemoteFile, {
          props: {
            fileId: '123',
          },
        });
    
        await wrapper.setProps({ fileId: '456' });
    
        expect(api.getFileDetail).toHaveBeenCalledWith('456');

        // Wait for promises to resolve
        await wrapper.vm.$nextTick();

        // Check the state of the component
        expect(wrapper.vm.file).toEqual({
        id: '456',
        name: 'test',
        timestamp: '2024-01-03T15:39:56Z',
        content: 'test content',
        });
    });

    it('filters out value when searching', async () => {
        wrapper = shallowMount(RemoteFile, {
          props: {
            fileId: '123',
          },
        });
        await wrapper.vm.$nextTick();

        const searchValue = "somesearchvalue";
        const input: DOMWrapper<HTMLElement> = wrapper.find("#searchbox");
        // console.log(wrapper.html(), 'xxxxx');

        // FIXME: errors on Error: wrapper.setValue() cannot be called on SEARCH-BAR-STUB
        // console.log(input.html(), 'xxxxx');
        // input.setValue(searchValue);

        // expect(input.value).toBe(searchValue);
        // expect(wrapper.emitted()).toHaveProperty("update:modelValue");
    });

});
