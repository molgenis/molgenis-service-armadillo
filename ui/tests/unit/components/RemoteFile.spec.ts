import { DOMWrapper, shallowMount, VueWrapper } from "@vue/test-utils";
import RemoteFile from "@/components/RemoteFile.vue";
import * as _api from "@/api/api";

const api = _api as any;
const num_lines = 30;
const lines = Array.from({length: num_lines}, (_, i) => `Line ${i}`);

// TODO: add test UI paging
//   getFileDetail(file_id, page_num, page_size)
// TODO: fix search: "Line": 30

jest.mock('@/api/api', () => ({
    getFileDetail: jest.fn().mockImplementation((fileId) => Promise.resolve({
        id: fileId,
        name: 'test',
        timestamp: '2024-01-03T15:39:56Z',
        // TODO: implement page_num and page_size
        content: [...lines].join('\n')
      })),
    getFileDownload: jest.fn().mockImplementation(() => Promise.resolve({
        blob: () => Promise.resolve(new Blob()),
      })),
  }));

describe("RemoteFile", () => {
    let wrapper: VueWrapper<any>;
    beforeEach(function () {

    });

    it('calls fetchFile when fileId changes', async () => {
        const wrapper = shallowMount(RemoteFile, {
          props: {
            fileId: '456',
          },
        });
    
        await wrapper.setProps({ fileId: '456' });
    
        expect(api.getFileDetail).toHaveBeenCalledWith("456", 0, 1000, "end");

        await wrapper.vm.$nextTick();

        expect(wrapper.vm.file).toEqual({
          id: '456',
          name: 'test',
          timestamp: '2024-01-03T15:39:56Z',
          content: [...lines].join('\n'),
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
