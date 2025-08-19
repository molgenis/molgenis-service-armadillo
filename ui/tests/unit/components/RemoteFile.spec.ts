import { DOMWrapper, mount, shallowMount, VueWrapper } from "@vue/test-utils";
import RemoteFile from "@/components/RemoteFile.vue";
import * as _api from "@/api/api";

const api = _api as any;
const num_lines = 30;
const lines = Array.from({length: num_lines}, (_, i) => `Line ${i}`);

jest.mock('@/api/api', () => ({
    getFileDetail: jest.fn().mockImplementation((fileId) => Promise.resolve({
        id: fileId,
        name: 'test',
        timestamp: '2024-01-03T15:39:56Z',
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
            reloadFile: false
          },
        });
    
        await wrapper.setProps({ fileId: '456' });
    
        expect(api.getFileDetail).toHaveBeenCalledWith("456", 0, "end");

        await wrapper.vm.$nextTick();

        expect(wrapper.vm.file).toEqual({
          id: '456',
          name: 'test',
          timestamp: '2024-01-03T15:39:56Z',
          content: [...lines].join('\n'),
        });
    });
});
