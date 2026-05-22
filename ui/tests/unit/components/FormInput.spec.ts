import { shallowMount, VueWrapper } from "@vue/test-utils";
import FormInput from "@/components/FormInput.vue";
import { nextTick } from "vue";

let clipboardContents = "";

Object.assign(navigator, {
  clipboard: {
    writeText: (text: string) => {
      clipboardContents = text;
      return new Promise((resolve) => {
        resolve("copied");
      });
    },
  },
});

describe("OidcCFormInputonfig", () => {
  let wrapper: VueWrapper<any>;
  beforeEach(function () {
     wrapper = shallowMount(FormInput, {
      props: {
       icon: "incognito",
       label: "Fill in your input",
       value: "Hello world",
       hasCopyButton: true,
       type: "password",
       isEditMode: false
      }
    });
  });
  test("label, icon and initial value loaded", () => {
    expect(wrapper.html()).toContain("Fill in your input");
    expect(wrapper.html()).toContain("bi-incognito");
    expect(wrapper.html()).toContain("Hello world");
  });

 test("input can be changed", async () => {
    wrapper.vm.mappedValue = "Goodbye world";
    await nextTick();
    expect(wrapper.html()).toContain("Goodbye world");
    expect(wrapper.html()).not.toContain("Hello world");
  }); 

  test("copyButton shown when set to true", () => {
    expect(wrapper.html()).toContain("bi bi-clipboard-fill");
  });

  test("copy fills clipboard with value of input", async () => {
    const button = wrapper.find("button.btn-secondary");
    button.trigger("click");
    await nextTick();
    expect(wrapper.vm.isCopied).toEqual(true);
    await nextTick();
    // check if copy button changes color and icon when copied
    expect(wrapper.html()).toContain('<button class="btn btn-success" type="button"><i class="bi bi-clipboard-check-fill"></i></button>');
    setTimeout(function() {
      // check if copied state resets so that color and icon of button reset after second
      expect(wrapper.vm.isCopied).toEqual(false);
    }, 1000);
    expect(clipboardContents).toEqual("Hello world");
  });

  test("copyButton shown when set to false", () => {
    wrapper = shallowMount(FormInput, {
      props: {
       icon: "incognito",
       label: "Fill in your input",
       value: "Hello world",
       hasCopyButton: false,
       type: "password",
       isEditMode: false
      }
    });
    expect(wrapper.html()).not.toContain("bi bi-clipboard-check-fill");
  });

  test("show password button shown", () => {
    expect(wrapper.html()).toContain("bi bi-eye-fill");
  });

  test("show password button makes visible when pushed", async () => {
    const button = wrapper.find("button.btn-info");
    button.trigger("click");
    await nextTick();
    expect(wrapper.vm.showSecret).toBe(true);
    expect(wrapper.html()).not.toContain("bi bi-eye-fill");
    expect(wrapper.html()).toContain("bi bi-eye-slash-fill");
  });

  test("show password button makes invisible when pushed twice", async () => {
    const button = wrapper.find("button.btn-info");
    button.trigger("click");
    await nextTick();
    expect(wrapper.vm.showSecret).toBe(true);
    button.trigger("click");
    await nextTick();
    expect(wrapper.vm.showSecret).toBe(false);
    expect(wrapper.html()).toContain("bi bi-eye-fill");
    expect(wrapper.html()).not.toContain("bi bi-eye-slash-fill");
  });

  test("show password button not shown when not a password", () => {
    wrapper = shallowMount(FormInput, {
      props: {
       icon: "incognito",
       label: "Fill in your input",
       value: "123",
       hasCopyButton: false,
       type: "number",
       isEditMode: false
      }
    });
    expect(wrapper.html()).not.toContain("bi bi-eye-slash-fill");
  });

});
