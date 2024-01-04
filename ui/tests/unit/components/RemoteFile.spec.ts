import { DOMWrapper, shallowMount, VueWrapper } from "@vue/test-utils";
import RemoteFile from "@/components/RemoteFile.vue";
import * as _api from "@/api/api";

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
    // Vars
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

      it('calls downloadFile and creates a download link', async () => {
        // Mock locally window function(s)
        const mockCreateObjectURL = jest.fn(() => 'mocked_blob_url');
        const originalCreateObjectURL = URL.createObjectURL;
        URL.createObjectURL = mockCreateObjectURL;
      
        const wrapper = shallowMount(RemoteFile, {
          props: {
            fileId: '123',
          },
        });
    
        await wrapper.vm.$nextTick();

        wrapper.vm.downloadFile();
        await wrapper.vm.$nextTick();

        expect(api.getFileDownload).toHaveBeenCalledWith('123');
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();

        expect(URL.createObjectURL).toHaveBeenCalled();
        // Restore overwrites
        URL.createObjectURL = originalCreateObjectURL;
      });
});
