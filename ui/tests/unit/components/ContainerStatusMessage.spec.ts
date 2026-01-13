import { mount, VueWrapper } from "@vue/test-utils";
import { ref, nextTick } from "vue";
import FeedbackMessage from "@/components/ContainerStatusMessage.vue"; // <-- rename to the actual filename

describe("Installing container progress", () => {
  let wrapper: VueWrapper<any>;

  beforeEach(function () {
    wrapper = mount(FeedbackMessage, {
      props: {
        containerName: "MyContainer",
        status: {
          status: "Installing container",
          totalLayers: 10,
          completedLayers: 5,
        },
      },
    });
  });

  test("renders only when status is 'Installing container'", async () => {
    // Initially renders
    expect(wrapper.text()).toContain("Installing container 'MyContainer'");
    expect(wrapper.find(".progress-bar").exists()).toBe(true);

    // Switch to a non-installing status -> should hide
    await wrapper.setProps({
      status: { status: "Idle", totalLayers: 10, completedLayers: 5 },
    });
    expect(wrapper.find(".progress-bar").exists()).toBe(false);

    // Null status -> should hide
    await wrapper.setProps({ status: null });
    expect(wrapper.find(".progress-bar").exists()).toBe(false);
  });

  test("computes and shows percentage from server (completed/total)", () => {
    // total=10, completed=5 -> 50%
    const bar = wrapper.get(".progress-bar");
    const now = Number(bar.attributes("aria-valuenow"));
    const width = (bar.element as HTMLElement).style.width;

    expect(now).toBeGreaterThanOrEqual(50); // smoothing never goes below server %
    expect(now).toBeLessThanOrEqual(99);    // cap is 99 until server hits 100
    expect(width).toContain(`${now}%`);
    expect(wrapper.text()).toContain(`${now}%`);
  });

  test("shows 100% when server reports completion", async () => {
    await wrapper.setProps({
      status: { status: "Installing container", totalLayers: 10, completedLayers: 10 },
    });
    const bar = wrapper.get(".progress-bar");
    const now = Number(bar.attributes("aria-valuenow"));
    const width = (bar.element as HTMLElement).style.width;

    expect(now).toBe(100);
    expect(width).toBe("100%");
    expect(wrapper.text()).toContain("100%");
  });

test("does not regress when totals are missing (stays at previous %)", async () => {
  const before = Number(wrapper.get(".progress-bar").attributes("aria-valuenow"));
  expect(before).toBeGreaterThanOrEqual(50);

  await wrapper.setProps({
    status: { status: "Installing container" }, 
  });

  const after = Number(wrapper.get(".progress-bar").attributes("aria-valuenow"));
  expect(after).toBeGreaterThanOrEqual(before);
  expect(after).toBeLessThanOrEqual(99);
});

test("accepts a Ref status and updates on change", async () => {
  const statusRef = ref<any>({
    status: "Installing container",
    totalLayers: 4,
    completedLayers: 1, // 25%
  });

  wrapper.unmount();
  wrapper = mount(FeedbackMessage, {
    props: { containerName: "RefContainer", status: statusRef },
  });

  let bar = wrapper.get(".progress-bar");
  let now = Number(bar.attributes("aria-valuenow"));
  expect(now).toBeGreaterThanOrEqual(25);

  // Update ref -> 75%
  statusRef.value = {
    status: "Installing container",
    totalLayers: 4,
    completedLayers: 3,
  };
  await nextTick();

  bar = wrapper.get(".progress-bar");
  now = Number(bar.attributes("aria-valuenow"));
  expect(now).toBeGreaterThanOrEqual(75);
  expect(now).toBeLessThanOrEqual(99);
});

test("smoothServerPercentage is monotonic and respects cap behavior", async () => {
  const callSmooth = async () => {
    (wrapper.vm as any).smoothServerPercentage();
    await nextTick();
    return Number(wrapper.get(".progress-bar").attributes("aria-valuenow"));
  };

  // Start from initial (≈50%+)
  const first = Number(
    wrapper.get(".progress-bar").attributes("aria-valuenow"),
  );

  // Same server percentage + extra smoothing tick -> must not go down, must stay < 99
  const second = await callSmooth();
  expect(second).toBeGreaterThanOrEqual(first);
  expect(second).toBeLessThanOrEqual(99);

  // Drop server-reported progress -> smoothed should NOT decrease
  await wrapper.setProps({
    status: {
      status: "Installing container",
      totalLayers: 10,
      completedLayers: 3, // 30% server, visual must not regress
    },
  });
  const third = await callSmooth();
  expect(third).toBeGreaterThanOrEqual(second);
  expect(third).toBeLessThanOrEqual(99);

  // Jump to completion -> should snap to 100 (cap=100 when serverPerc=100)
  await wrapper.setProps({
    status: {
      status: "Installing container",
      totalLayers: 10,
      completedLayers: 10,
    },
  });
  const fourth = await callSmooth();
  expect(fourth).toBe(100);
});
});
