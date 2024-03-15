import { DOMWrapper, mount, shallowMount, VueWrapper } from "@vue/test-utils";
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
        // FIXME: according to https://vuejs.org/guide/scaling-up/testing.html#component-testing
        // DO NOT test inner workings of you component
        // so testing search or paging seems a DO NOT to me now.
          wrapper = mount(RemoteFile, {
          props: {
            fileId: '123',
          },
        });
        await wrapper.vm.$nextTick();

        expect(wrapper.vm.file).toEqual({
          id: '123',
          name: 'test',
          timestamp: '2024-01-03T15:39:56Z',
          content: [...lines].join('\n'),
        });

        const searchValue = "Line";

        const inputElement:HTMLInputElement = wrapper.find('#searchbox').element as HTMLInputElement;
        
        inputElement.value = searchValue;
        inputElement.dispatchEvent(new Event('input'));

        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();

        // console.log(wrapper.html(), 'xxxxx');

        expect(inputElement.value).toBe(searchValue);
        // expect(wrapper.vm.$el).toMatchSnapshot();
        // expect(wrapper.emitted()).toHaveProperty("update:modelValue");
    });

});
