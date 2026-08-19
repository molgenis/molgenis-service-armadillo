import { shallowMount, VueWrapper } from "@vue/test-utils";
import FileExplorer from "@/components/FileExplorer.vue";
import { createRouter, createWebHistory } from "vue-router";
import * as _api from "@/api/api";

const api = _api as any;

jest.mock("@/api/api");

const testFunction = jest.fn();

describe("FileExplorer", () => {

    let wrapper: VueWrapper<any>;

    const projectContent = {
      folder1: [
        "file1.csv",
        "my-table.parquet",
        "my-link.alf",
        "file2.png"
      ],
      folder2: [
        "my-resource.rds",
        "the-actual-resource.rda",
        "my-img.jpg"
      ],
      anotherfolder: [
        "test123.abc",
        "aap.test"
      ]
    }

    function createWrapper(folder: string) {
        return shallowMount(FileExplorer, {
            props: {
             projectContent: projectContent,
             addNewFolder: testFunction,
             selectedFolder: folder,
             selectedFile: ""
            },
        });
}

    beforeEach(function() {
        wrapper = createWrapper("");
    });

    test("sorts folders", () => {
        expect(wrapper.vm.getSortedFolders()).toEqual(["anotherfolder", "folder1", "folder2"]);
    });

    test("sorts files", () => {
        wrapper = createWrapper("folder2");
        expect(wrapper.vm.getSortedFiles()).toEqual([
          "my-img.jpg",
          "my-resource.rds",
          "the-actual-resource.rda"
        ],);
       wrapper = createWrapper("folder1");
        expect(wrapper.vm.getSortedFiles()).toEqual([
          "file1.csv",
          "file2.png",
          "my-link.alf",
          "my-table.parquet"
        ]);
        wrapper = createWrapper("anotherfolder");
        expect(wrapper.vm.getSortedFiles()).toEqual([
          "aap.test",
          "test123.abc"
        ]);
    });

    test("showSelectedFolderIcon shows folder icon when folder is selected", () => {
        const folder = "my-folder";
        wrapper = wrapper = createWrapper(folder);
        expect(wrapper.vm.showSelectedFolderIcon(folder)).toEqual(true);
    });

    test("showSelectedFolderIcon doesnt show folder icon when folder is not selected", () => {
        wrapper = createWrapper("another-folder");
        const folder = "my-folder";
        expect(wrapper.vm.showSelectedFolderIcon(folder)).toEqual(false);
    });

    test("setCreateNewFolder sets createNewFolder to true", () => {
        wrapper.vm.createNewFolder = false;
        wrapper.vm.setCreateNewFolder();
        expect(wrapper.vm.createNewFolder).toEqual(true);
    });

    test("cancelNewFolder sets createNewFolder to false and empties newFolder", () => {
        wrapper.vm.createNewFolder = true;
        wrapper.vm.newFolder = "my-folder"
        wrapper.vm.cancelNewFolder();
        expect(wrapper.vm.createNewFolder).toEqual(false);
        expect(wrapper.vm.newFolder).toEqual("");
    });
});
