import { shallowMount, VueWrapper } from "@vue/test-utils";
import ProjectsExplorer from "@/views/ProjectsExplorer.vue";
import { createRouter, createWebHistory } from "vue-router";
import * as _api from "@/api/api";
import { ProjectsExplorerData, StringArray } from "@/types/types";

const api = _api as any;

jest.mock("@/api/api");

describe("ProjectsExplorer", () => {
    let testData: Array<string>;

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

    beforeEach(function() {
        const mockRouter = {
            push: jest.fn(),
        };

        router.currentRoute.value.params = { projectId: "some-project" };

        testData = [
            "some-project/folder-a-one/file1.parquet",
            "some-project/folder-a-one/file2.parquet",
            "some-project/folder-a-one/fileb.parquet",
            "some-project/folder-a-one/filea.parquet",
            "some-project/folder-b-two/file1.parquet",
            "some-project/folder-b-two/file2.parquet",
            "some-project/folder-d-four/file2.parquet",
            "some-project/folder-d-four/file1.parquet",
            "some-project/folder-c-three/file1.parquet"
        ];

        api.getProject.mockImplementationOnce(() => {
            return Promise.resolve(testData);
        });

        wrapper = shallowMount(ProjectsExplorer, {
            global: {
                plugins: [router],
                mocks: {
                    $router: mockRouter,
                },
            },
        });

    });

    test("setting project content", () => {
        expect(wrapper.vm.projectContent).toEqual(    
            {
                "folder-a-one": [
                  "file1.parquet",
                  "file2.parquet",
                  "fileb.parquet",
                  "filea.parquet"
                ],
                "folder-b-two": ["file1.parquet", "file2.parquet"],
                "folder-d-four": ["file2.parquet", "file1.parquet"],
                "folder-c-three": ["file1.parquet"]
                }
        );
    });

    test("sorts folders", () => {
        expect(wrapper.vm.getSortedFolders()).toEqual(["folder-a-one", "folder-b-two", "folder-c-three", "folder-d-four"]);
    });

    test("sorts files", () => {
        wrapper.vm.selectedFolder = "folder-a-one";
        expect(wrapper.vm.getSortedFiles()).toEqual(["file1.parquet", "file2.parquet", "filea.parquet", "fileb.parquet"]);
        wrapper.vm.selectedFolder = "folder-b-two";
        expect(wrapper.vm.getSortedFiles()).toEqual(["file1.parquet", "file2.parquet"]);
        wrapper.vm.selectedFolder = "folder-c-three";
        expect(wrapper.vm.getSortedFiles()).toEqual(["file1.parquet"]);
        wrapper.vm.selectedFolder = "folder-d-four";
        expect(wrapper.vm.getSortedFiles()).toEqual(["file1.parquet", "file2.parquet"]);
    });

    test("ask if preview is empty and setting empty", () => {
        expect(wrapper.vm.askIfPreviewIsEmpty()).toBe(true);
        wrapper.vm.filePreview = [{"some-file": "foobar"}];
        expect(wrapper.vm.askIfPreviewIsEmpty()).toBe(false);
        wrapper.vm.clearFilePreview();
        expect(wrapper.vm.askIfPreviewIsEmpty()).toBe(true);
    });

    test("succesfully creating a new folder", () => {
        expect(wrapper.vm.createNewFolder).toBe(false);
        wrapper.vm.setCreateNewFolder();
        expect(wrapper.vm.createNewFolder).toBe(true);
        wrapper.vm.newFolder = "FOLDER-e-five";
        wrapper.vm.addNewFolder();
        expect(wrapper.vm.getSortedFolders()).toContain("folder-e-five");
    });

    test("error creating folder containing /", () => {
        expect(wrapper.vm.errorMessage).toBe("");
        wrapper.vm.setCreateNewFolder();
        wrapper.vm.newFolder = "folder/five";
        wrapper.vm.addNewFolder();
        expect(wrapper.vm.errorMessage).toBe("Folder name cannot contain /");
    });

    test("error creating folder containing /", () => {
        expect(wrapper.vm.errorMessage).toBe("");
        wrapper.vm.setCreateNewFolder();
        wrapper.vm.addNewFolder();
        expect(wrapper.vm.errorMessage).toBe("Folder name cannot be empty");
    });

    test("cancel creating a new folder", () => {
        expect(wrapper.vm.createNewFolder).toBe(false);
        expect(wrapper.vm.newFolder).toBe("");
        wrapper.vm.setCreateNewFolder();
        wrapper.vm.newFolder = "some-folder";
        expect(wrapper.vm.createNewFolder).toBe(true);
        expect(wrapper.vm.newFolder).toBe("some-folder");
        wrapper.vm.cancelNewFolder();
        expect(wrapper.vm.createNewFolder).toBe(false);
        expect(wrapper.vm.newFolder).toBe("");
    });

    test("success message uploading a file", () => {
        expect(wrapper.vm.successMessage).toBe("");
        wrapper.vm.onUploadSuccess({object: "folder-d-four", filename: "some-new-file.extension"});
        expect(wrapper.vm.successMessage).toBe("Successfully uploaded file [some-new-file.extension] into directory [folder-d-four] of project: [some-project]");
    });

    test("non table type", () => {
        expect(wrapper.vm.isNonTableType("some-file.parquet")).toBe(false);
        expect(wrapper.vm.isNonTableType("some-file.csv.gz")).toBe(true);
        expect(wrapper.vm.isNonTableType("some-file.rda")).toBe(true);
        expect(wrapper.vm.isNonTableType("some-file")).toBe(true);
    });

    test("setting and clearing file to delete", () => {
        expect(wrapper.vm.fileToDelete).toBe("");
        expect(wrapper.vm.folderToDeleteFrom).toBe("");
        // Important: selectedFolder before selectedFile, since watcher for selectedFolder resets selectedFile to ""
        wrapper.vm.selectedFolder = "folder-d-four";
        wrapper.vm.selectedFile = "file1.parquet";
        wrapper.vm.deleteSelectedFile();
        expect(wrapper.vm.fileToDelete).toBe("file1.parquet");
        expect(wrapper.vm.folderToDeleteFrom).toBe("folder-d-four");
        wrapper.vm.clearRecordToDelete();
        expect(wrapper.vm.fileToDelete).toBe("");
        expect(wrapper.vm.folderToDeleteFrom).toBe("");
    });

    test("successfully proceed deleting file", async () => {
        api.deleteObject.mockImplementation(() => {
            return Promise.resolve({})
        });
        wrapper.vm.selectedFolder = "folder-d-four";
        wrapper.vm.selectedFile = "file2.parquet";
        await wrapper.vm.proceedDelete("folder-d-four/file2.parquet");
        expect(wrapper.vm.selectedFile).toBe("");
        expect(wrapper.vm.successMessage).toBe("Successfully deleted file [file2.parquet] from directory [folder-d-four] of project: [some-project]");
    });

    test("error proceed deleting file", async () => {
        const error = new Error("fail");
        api.deleteObject.mockImplementation(() => {
            return Promise.reject(error)
        });
        wrapper.vm.selectedFolder = "doesnt-exist";
        wrapper.vm.selectedFile = "doesnot.exists";
        await wrapper.vm.proceedDelete("doesnt-exist/doesnot.exists");
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.errorMessage).toBe(error);
    });

    test("successfully reloads project", async () => {
        let new_data = [
            "some-project/other-folder/one-some.file",
            "some-project/other-folder/one-some.file"
        ];
        expect(wrapper.vm.getSortedFolders()).toEqual(["folder-a-one", "folder-b-two", "folder-c-three", "folder-d-four"]);
        api.getProject.mockImplementation(() => {
            return Promise.resolve(new_data)
        });
        await wrapper.vm.reloadProject();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.getSortedFolders()).toEqual(["other-folder"]);
    });

    test("error reloading project", async () => {
        const error = new Error("fail");
        api.getProject.mockImplementation(() => {
            return Promise.reject(error)
        });
        await wrapper.vm.reloadProject();
        expect(wrapper.vm.errorMessage).toBe("Could not load project: Error: fail.");
    });

    test("reloads project with call to callback", async () => {
        const someFunction = jest.fn();
        await wrapper.vm.reloadProject(someFunction);
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        expect(someFunction).toBeCalled();
    });

    test("show error message with string", () => {
        wrapper.vm.showErrorMessage("some very random error");
        expect(wrapper.vm.errorMessage).toBe("some very random error");
    });

    test("show error message with Error", () => {
        const error = new Error("fail");
        wrapper.vm.showErrorMessage(error);
        expect(wrapper.vm.errorMessage).toBe(error);
    });
});
