import { Directive } from "vue";

function resize(el: HTMLTextAreaElement) {
  requestAnimationFrame(() => {
    el.style.height = "auto";
    const h = el.scrollHeight;
    el.style.height = (h > 0 ? h : 24) + "px";
  });
}

export const vAutosize: Directive<HTMLTextAreaElement, unknown> = {
  mounted(el) {
    el.style.overflow = "hidden";
    el.style.resize = "none";
    resize(el);
    el.addEventListener("input", () => resize(el));
  },
  updated(el, binding) {
    if (binding.value !== binding.oldValue) resize(el);
  },
  unmounted(el) {
    el.removeEventListener("input", () => resize(el));
  },
};
