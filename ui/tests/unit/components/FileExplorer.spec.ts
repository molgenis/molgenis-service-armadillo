import { shallowMount, VueWrapper } from "@vue/test-utils";
import FileExplorer from "@/components/FileExplorer.vue";
import { createRouter, createWebHistory } from "vue-router";
import * as _api from "@/api/api";

const api = _api as any;

jest.mock("@/api/api");

const testFunction = jest.fn();

describe("FileExplorer", () => {
    const mock_routes = [
        {
            path: "/",
            redirect: "/item_a"
        },
        {
            path: "/item_a",
            component: {
                template: "Welcome to item a",
            },
        },
        {
            path: "/item_b",
            component: {
                template: "Welcome to item b",
            },
        },
        {
            path: "/item_c",
            component: {
                template: "Welcome to item c",
            },
        },
    ];
    const router = createRouter({
        history: createWebHistory(),
        routes: mock_routes,
    });

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

    beforeEach(function() {
        const mockRouter = {
            push: jest.fn(),
        };

        router.currentRoute.value.params = { projectId: "my-project" };

        wrapper = shallowMount(FileExplorer, {
            global: {
                plugins: [router],
                mocks: {
                    $router: mockRouter,
                },
            },
            props: {
             projectContent: projectContent,
             addNewFolder: testFunction
            },
        });

    });

    test("sorts folders", () => {
        expect(wrapper.vm.getSortedFolders()).toEqual(["anotherfolder", "folder1", "folder2"]);
    });

    test("sorts files", () => {
        wrapper.vm.selectedFolder = "folder2";
        expect(wrapper.vm.getSortedFiles()).toEqual([
          "my-img.jpg",
          "my-resource.rds",
          "the-actual-resource.rda"
        ],);
        wrapper.vm.selectedFolder = "folder1";
        expect(wrapper.vm.getSortedFiles()).toEqual([
          "file1.csv",
          "file2.png",
          "my-link.alf",
          "my-table.parquet"
        ]);
        wrapper.vm.selectedFolder = "anotherfolder";
        expect(wrapper.vm.getSortedFiles()).toEqual([
          "aap.test",
          "test123.abc"
        ]);
    });

    test("showSelectedFolderIcon shows folder icon when folder is selected", () => {
        const folder = "my-folder";
        wrapper.vm.selectedFolder = folder;
        expect(wrapper.vm.showSelectedFolderIcon(folder)).toEqual(true);
    });

    test("showSelectedFolderIcon doesnt show folder icon when folder is not selected", () => {
        const folder = "my-folder";
        wrapper.vm.selectedFolder = "another-folder";
        expect(wrapper.vm.showSelectedFolderIcon(folder)).toEqual(false);
    });

});
