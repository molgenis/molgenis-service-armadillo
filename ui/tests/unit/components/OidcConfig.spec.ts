import { mount, VueWrapper } from "@vue/test-utils";
import OidcConfig from "@/components/OidcConfig.vue";
import { nextTick } from "vue";

describe("OidcConfig", () => {
    let wrapper: VueWrapper<any>;
    beforeEach(function () {
     wrapper = mount(OidcConfig, {
      props: {
        presetServerUri: "http://auth-server",
        presetClientId: "my-id",
        presetClientSecret: "my-very-secret-secret" 
      }
    });
    });
  test("presets are loaded", () => {
    expect(wrapper.html()).toContain("http://auth-server");
    expect(wrapper.html()).toContain("my-id");
    expect(wrapper.html()).toContain("my-very-secret-secret");
  });

  test("turnOnEditmode", async () => {
    wrapper.vm.turnOnEditmode();
    await nextTick(); // wait for DOM to update
    expect(wrapper.vm.isEditMode).toBe(true);
    expect(wrapper.html()).toContain("Cancel");
    expect(wrapper.html()).toContain("Save");
    expect(wrapper.html()).not.toContain("bi-pencil-fill");
  });

  test("turnOffEditmode", async () => {
    wrapper.vm.isEditMode = true;
    await nextTick();
    wrapper.vm.turnOffEditmode();
    await nextTick();
    expect(wrapper.vm.isEditMode).toBe(false);
    expect(wrapper.html()).toContain("bi-pencil-fill");
    expect(wrapper.html()).not.toContain("Cancel");
    expect(wrapper.html()).not.toContain("Save");
  });

  test("triggerSave", async () => {
    wrapper.vm.isEditMode = true;
    await nextTick();
    wrapper.vm.triggerSave();
    await nextTick();
    expect(wrapper.vm.isEditMode).toBe(false);
    expect(wrapper.emitted()).toHaveProperty('saveOidcConfig');
  });

  test("cancelEdit", async () => {
    wrapper.vm.isEditMode = true;
    await nextTick();
    wrapper.vm.serverUri = "another-uri";
    wrapper.vm.clientId = "another-id";
    wrapper.vm.clientSecret = "another-secret";
    wrapper.vm.cancelEdit();
    await nextTick();
    expect(wrapper.vm.isEditMode).toBe(false);
    expect(wrapper.html()).toContain("http://auth-server");
    expect(wrapper.html()).toContain("my-id");
    expect(wrapper.html()).toContain("my-very-secret-secret"); 
  });
});
