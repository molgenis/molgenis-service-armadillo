import { mount, VueWrapper } from "@vue/test-utils";
import FormValidation from "@/components/FormValidation.vue";

describe("ButtonGroup", () => {
  const msg = "Invalid form! Please fix.";
  const mockFunction = jest.fn();
  let wrapper: VueWrapper<any>;
  beforeEach(function () {
    wrapper = mount(FormValidation, {
      props: {
        validationCondition: false,
        invalidMessage: msg
      },
    });
  });

  test("validation message not shown when isValidated not defined", () => {
   expect(wrapper.html()).not.toContain(msg)
  });

  test("validation message not shown when isValidated true and condition false", async () => {
    await wrapper.setProps({ isValidated: true});
    expect(wrapper.html()).not.toContain(msg)
   });

   test("validation message shown when isValidated true and condition false", async () => {
    await wrapper.setProps({ validationCondition: true, isValidated: true});
    expect(wrapper.html()).toContain(msg)
   });

});
