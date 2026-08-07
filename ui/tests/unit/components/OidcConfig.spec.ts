import { mount, VueWrapper } from "@vue/test-utils";
import { nextTick } from "vue";
import OidcConfig from "@/components/OidcConfig.vue";
import FormInput from "@/components/FormInput.vue";

const defaultProps = {
  presetServerUri: "https://auth.example.com",
  presetClientId: "my-client-id",
  presetClientSecret: "my-client-secret",
  presetDeviceServerUri: "https://device.example.com",
  presetDeviceClientId: "my-device-client-id",
};

function createWrapper(props = {}): VueWrapper {
  return mount(OidcConfig, {
    props: { ...defaultProps, ...props }
  });
}

describe("OidcConfig.vue", () => {
  // -------------------------
  // Edit mode
  // -------------------------
  describe("Edit mode", () => {
    it("enters edit mode when the edit button is clicked", async () => {
      const wrapper = createWrapper();
      await wrapper.find("button.btn-primary.btn-sm").trigger("click");
      expect((wrapper.vm as any).isEditMode).toBe(true);
    });

    it("shows Cancel and Save buttons in edit mode", async () => {
      const wrapper = createWrapper();
      await wrapper.find("button.btn-primary.btn-sm").trigger("click");
      expect(wrapper.find("button.btn-danger").exists()).toBe(true);
      expect(
        wrapper.findAll("button.btn-primary").some((b) =>
          b.text().includes("Save")
        )
      ).toBe(true);
    });

    it("hides the edit button when in edit mode", async () => {
      const wrapper = createWrapper();
      await wrapper.find("button.btn-primary.btn-sm").trigger("click");
      // The small pencil edit button should be gone
      expect(wrapper.find("button.btn-primary.btn-sm").exists()).toBe(false);
    });
  });

  // -------------------------
  // Cancel
  // -------------------------
  describe("Cancel edit", () => {
    it("exits edit mode when Cancel is clicked", async () => {
      const wrapper = createWrapper();
      await wrapper.find("button.btn-primary.btn-sm").trigger("click");
      await wrapper.find("button.btn-danger").trigger("click");
      expect((wrapper.vm as any).isEditMode).toBe(false);
    });

    it("re-renders FormInput components after cancel (forceRerender)", async () => {
      const wrapper = createWrapper();
      await wrapper.find("button.btn-primary.btn-sm").trigger("click");

      // renderComponent briefly becomes false then true after nextTick
      await wrapper.find("button.btn-danger").trigger("click");
      await nextTick();

      expect((wrapper.vm as any).renderComponent).toBe(true);
    });

    it("does not emit saveOidcConfig on cancel", async () => {
      const wrapper = createWrapper();
      await wrapper.find("button.btn-primary.btn-sm").trigger("click");
      await wrapper.find("button.btn-danger").trigger("click");
      expect(wrapper.emitted("saveOidcConfig")).toBeFalsy();
    });
  });

  // -------------------------
  // Save
  // -------------------------
  describe("Save", () => {
    it("exits edit mode after saving", async () => {
      const wrapper = createWrapper();
      await wrapper.find("button.btn-primary.btn-sm").trigger("click");

      (wrapper.vm as any).$refs.serverUri = { mappedValue: "" };
      (wrapper.vm as any).$refs.clientId = { mappedValue: "" };
      (wrapper.vm as any).$refs.clientSecret = { mappedValue: "" };
      (wrapper.vm as any).$refs.deviceServerUri = { mappedValue: "" };
      (wrapper.vm as any).$refs.deviceClientId = { mappedValue: "" };

      const saveBtn = wrapper
        .findAll("button.btn-primary")
        .find((b) => b.text().includes("Save"))!;
      await saveBtn.trigger("click");

      expect((wrapper.vm as any).isEditMode).toBe(false);
    });
  });

  // -------------------------
  // forceRerender
  // -------------------------
  describe("forceRerender", () => {
    it("sets renderComponent to false then back to true", async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.forceRerender();
      expect(vm.renderComponent).toBe(false);

      await nextTick();
      expect(vm.renderComponent).toBe(true);
    });
  });
});