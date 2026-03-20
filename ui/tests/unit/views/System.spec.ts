import { mount, VueWrapper } from "@vue/test-utils";
import System from "@/views/System.vue";
import * as _api from "@/api/api";
import { nextTick } from "vue";

const api = _api as any;

jest.mock("@/api/api");

const testData = {
  "issuerUri": "https://auth.molgenis.org",
  "clientId": "my-client-id",
  "clientSecret": "VeryS3cretP@ssw0rd"
}
describe("System", () => {
    let wrapper: VueWrapper<any>;

    beforeEach(function() {

        api.getAuthServerConfig.mockImplementation(() => {
            return Promise.resolve(testData);
        });

        wrapper = mount(System, {
            global: {
                mocks: {
                },
            },
        });

    });

    test("Verify url/client-id/secret are passed to config", () => {
      expect(wrapper.html()).toContain("https://auth.molgenis.org");
      expect(wrapper.html()).toContain("my-client-id");
      expect(wrapper.html()).toContain("VeryS3cretP@ssw0rd");
    });

    test("Verify restart button push opens confirmation dialog", async () => {
      const buttons = wrapper.findAll("button.btn-warning");
      buttons[0].trigger("click");
      await nextTick();
      expect(wrapper.vm.isRestartServerPushed).toBe(true);
      expect(wrapper.html()).toContain("The website will be down for a short period of time. Try refreshing until it is back up.");
    });

    test("Verify restart cancel closes confirmation dialog", async () => {
      const restartButton = wrapper.findAll("button.btn-warning");
      restartButton[0].trigger("click");
      await nextTick();
      const closeButton = wrapper.findAll("button.btn-sm.btn-danger");
      closeButton[0].trigger("click");
      await nextTick();
      expect(wrapper.vm.isRestartServerPushed).toBe(false);
    });

    test("Verify restart proceed calls restart function", async () => {
      const mockFunction = jest.fn();
      api.restartServer.mockImplementation(() => {
        mockFunction();
        return Promise.resolve();
      });
      const restartButton = wrapper.findAll("button.btn-warning");
      restartButton[0].trigger("click");
      await nextTick();
      const yesButton = wrapper.findAll("button.btn-sm.btn-success");
      yesButton[0].trigger("click");
      await nextTick();
      expect(mockFunction).toHaveBeenCalled();
      expect(wrapper.html()).toContain("Server will restart now. Please refresh and log back in.");
    });

    test("Verify oidc save opens confirmation dialog", async () => {
      const editButton = wrapper.findAll("button.btn-primary.btn-sm.float-end");
      editButton[0].trigger("click");
      await nextTick();
      const saveButton = wrapper.findAll("button.btn-primary");
      saveButton[0].trigger("click");
      await nextTick();
      expect(wrapper.vm.updateOidcTriggered).toBe(true);
    });

     test("Verify oidc save no cancels edit", async () => {
      const editButton = wrapper.findAll("button.btn-primary.btn-sm.float-end");
      editButton[0].trigger("click");
      await nextTick();
      const saveButton = wrapper.findAll("button.btn-primary");
      saveButton[0].trigger("click");
      await nextTick();
      expect(wrapper.vm.updateOidcTriggered).toBe(true);
      const closeButton = wrapper.findAll("button.btn-sm.btn-danger");
      closeButton[0].trigger("click");
      await nextTick();
      expect(wrapper.vm.updateOidcTriggered).toBe(false);
    });

     test("Verify oidc save yes triggers api call", async () => {
      const mockFunction = jest.fn();
      api.putAuthServerConfig.mockImplementation(() => {
        mockFunction();
        return Promise.resolve();
      });
      const editButton = wrapper.findAll("button.btn-primary.btn-sm.float-end");
      editButton[0].trigger("click");
      await nextTick();
      const saveButton = wrapper.findAll("button.btn-primary");
      saveButton[0].trigger("click");
      await nextTick();
      expect(wrapper.vm.updateOidcTriggered).toBe(true);
      const yesButton = wrapper.findAll("button.btn-sm.btn-success");
      yesButton[0].trigger("click");
      await nextTick();
      expect(mockFunction).toHaveBeenCalled();
    });

});
