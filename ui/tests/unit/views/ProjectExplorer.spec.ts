import { shallowMount, VueWrapper } from "@vue/test-utils";
import ProjectsExplorer from "@/views/ProjectsExplorer.vue";
import { createRouter, createWebHistory } from "vue-router";
import * as _api from "@/api/api";
import { StringObject } from "@/types/types";

const api = _api as any;

jest.mock("@/api/api");

describe("ProjectsExplorer", () => {
    let testData: Array<string>;
    let metaData: Record<string, StringObject>;

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

        metaData = {
            "sample_id": {
                "type": "BINARY",
                "missing": "0/243"
            },
            "patient_cohort": {
                "type": "BINARY",
                "missing": "0/243",
                "levels": [
                "Cohort1"
                ]
            },
            "sample_origin": {
                "type": "BINARY",
                "missing": "0/243",
                "levels": [
                "BPTB",
                "ESP",
                "LIV"
                ]
            },
            "age": {
                "type": "DOUBLE",
                "missing": "0/243"
            },
            "sex": {
                "type": "BINARY",
                "missing": "0/243",
                "levels": [
                "female",
                "male"
                ]
            },
            "diagnosis": {
                "type": "BINARY",
                "missing": "0/243",
                "levels": [
                "benign",
                "cancer"
                ]
            },
            "stage": {
                "type": "BINARY",
                "missing": "81/243",
                "levels": [
                "II",
                "IIA",
                "IIB",
                "IA",
                "III",
                "I",
                "IB",
                "IV"
                ]
            },
            "benign_sample_diagnosis": {
                "type": "BINARY",
                "missing": "243/243",
                "levels": []
            },
            "plasma_CA19_9": {
                "type": "DOUBLE",
                "missing": "80/243"
            },
            "creatinine": {
                "type": "DOUBLE",
                "missing": "0/243"
            },
            "LYVE1": {
                "type": "DOUBLE",
                "missing": "0/243"
            },
            "REG1B": {
                "type": "DOUBLE",
                "missing": "0/243"
            },
            "TFF1": {
                "type": "DOUBLE",
                "missing": "0/243"
            },
            "REG1A": {
                "type": "DOUBLE",
                "missing": "24/243"
            }
            };

        api.getProject.mockImplementationOnce(() => {
            return Promise.resolve(testData);
        });

        api.getMetaData.mockImplementationOnce(() => {
            return Promise.resolve(metaData);
        })

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
        wrapper.vm.addNewFolder("FOLDER-e-five");
        expect(Object.keys(wrapper.vm.projectContent)).toContain("folder-e-five");
    });

    test("error creating folder containing /", () => {
        expect(wrapper.vm.errorMessage).toBe("");
        wrapper.vm.setCreateNewFolder();
        wrapper.vm.addNewFolder("folder/five");
        expect(wrapper.vm.errorMessage).toBe("Folder name cannot contain /");
    });

    test("error creating empty folder", () => {
        expect(wrapper.vm.errorMessage).toBe("");
        wrapper.vm.setCreateNewFolder();
        wrapper.vm.addNewFolder();
        expect(wrapper.vm.errorMessage).toBe("Folder name cannot be empty");
    });

    test("cancel creating a new folder", () => {
        expect(wrapper.vm.createNewFolder).toBe(false);
        wrapper.vm.setCreateNewFolder();
        expect(wrapper.vm.createNewFolder).toBe(true);
        wrapper.vm.cancelNewFolder();
        expect(wrapper.vm.createNewFolder).toBe(false);
    });

    test("success message uploading a file", () => {
        expect(wrapper.vm.successMessage).toBe("");
        wrapper.vm.onUploadSuccess({object: "folder-d-four", filename: "some-new-file.extension"});
        expect(wrapper.vm.successMessage).toBe("Successfully uploaded file [some-new-file.extension] into directory [folder-d-four] of project: [some-project]");
    });

    test("setting and clearing file to delete", () => {
        api.previewObject.mockImplementation(() => {
            return Promise.resolve([{}]);
        });
        api.getFileDetails.mockImplementation(() => {
            return Promise.resolve({});
        });
        api.getTableVariables.mockImplementation(() => {
            return Promise.resolve({});
        });
        // Important: selectedFolder before selectedFile, since watcher for selectedFolder resets selectedFile to ""
        expect(wrapper.vm.fileToDelete).toBe("");
        expect(wrapper.vm.folderToDeleteFrom).toBe("");
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
        api.previewObject.mockImplementation(() => {
            return Promise.resolve([{}]);
        });
        api.getFileDetails.mockImplementation(() => {
            return Promise.resolve({});
        });
        api.deleteObject.mockImplementation(() => {
            return Promise.resolve({});
        });
        wrapper.vm.selectedFolder = "folder-d-four";
        wrapper.vm.selectedFile = "file2.parquet";
        await wrapper.vm.proceedDelete("folder-d-four/file2.parquet");
        expect(wrapper.vm.selectedFile).toBe("");
        expect(wrapper.vm.successMessage).toBe("Successfully deleted file [file2.parquet] from directory [folder-d-four] of project: [some-project]");
    });

    test("error proceed deleting file", async () => {
        api.getFileDetails.mockImplementation(() => {
            return Promise.resolve({});
        });
        const error = new Error("fail");
        api.deleteObject.mockImplementation(() => {
            return Promise.reject(error)
        });
        wrapper.vm.selectedFolder = "doesnt-exist";
        wrapper.vm.selectedFile = "doesnot.exists";
        await wrapper.vm.proceedDelete("doesnt-exist/doesnot.exists");
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.errorMessage).toBe(`${error}`);
    });

    test("error reloading project", async () => {
        const error = new Error("fail");
        api.getProject.mockImplementation(() => {
            return Promise.reject(error);
        });
        await wrapper.vm.reloadProject();
        expect(wrapper.vm.errorMessage).toBe("Could not load project: Error: fail.");
    });

    test("reloads project with call to callback", async () => {
        api.getFileDetails.mockImplementation(() => {
            return Promise.resolve({});
        });
        api.getProject.mockImplementationOnce(() => {
            return Promise.resolve(testData);
        });
        expect(wrapper.vm.loading).toBe(false);
        const someFunction = jest.fn();
        await wrapper.vm.reloadProject(someFunction);
        expect(wrapper.vm.loading).toBe(true);
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        expect(someFunction).toHaveBeenCalled();
        expect(wrapper.vm.loading).toBe(false);
    });

    test("show error message with string", () => {
        wrapper.vm.showErrorMessage("some very random error");
        expect(wrapper.vm.errorMessage).toBe("some very random error");
    });

    test("show error message used when UploadFile fails", () => {
        // Since showErrorMessage already expects a string and is only used in FileUpload 
        // and FileUpload only returns the error as string, using only a string in this test.
        wrapper.vm.showErrorMessage("A very random UploadFile error string");
        expect(wrapper.vm.errorMessage).toBe("A very random UploadFile error string");
    });

    test("set createLinkFromSrc to false", () => {
        wrapper.vm.createLinkFromSrc = true;
        wrapper.vm.resetCreateLinkFile();
        expect(wrapper.vm.createLinkFromSrc).toBe(false);
    });

    test("set createLinkFromTarget to false", () => {
        wrapper.vm.createLinkFromTarget = true;
        wrapper.vm.resetCreateLinkFile();
        expect(wrapper.vm.createLinkFromTarget).toBe(false);
    });

    test("set createLinkFromSrc and editView to false", () => {
        wrapper.vm.createLinkFromSrc = true;
        wrapper.vm.editView = true;
        wrapper.vm.cancelView();
        expect(wrapper.vm.createLinkFromSrc).toBe(false);
        expect(wrapper.vm.editView).toBe(false);
    });
    
    test("createLinkFile calls api to update linkfile", async () => {
        const createLinkFileMock = jest.fn();
        api.createLinkFile.mockImplementation(() => {
            createLinkFileMock();
            return Promise.resolve({});
        });
        wrapper.vm.doCreateLinkFile(
            "source-project",
            "src-folder/src-obj",
            "view-project",
            "view-folder/vw-obj",
            ["var1","var2","var3"]
        );
        expect(createLinkFileMock).toHaveBeenCalled();
    });
});
