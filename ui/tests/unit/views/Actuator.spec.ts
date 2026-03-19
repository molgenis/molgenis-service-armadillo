// Mocks must be at the top before imports
jest.mock('@/api/api', () => ({
  getMetricsAll: jest.fn().mockResolvedValue({}),
  getMetrics: jest.fn().mockResolvedValue({}),
  getActuator: jest.fn().mockResolvedValue({}),
}));

jest.mock('@/helpers/errorProcessing', () => ({
  processErrorMessages: jest.fn(),
}));

import { shallowMount, VueWrapper } from "@vue/test-utils";
import Actuator from "@/views/Actuator.vue";
import { createRouter, createWebHistory } from "vue-router";
import * as _api from "@/api/api";
import * as _errors from "@/helpers/errorProcessing";

const api = _api as any;
const errors = _errors as any;

describe("Actuator.vue", () => {
  let wrapper: VueWrapper<any>;

  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: "/", component: { template: "<div>Home</div>" } }, 
    ],
  });

  beforeEach(async () => {
    // Set up common mocks
    api.getMetricsAll.mockResolvedValue({
      "disk.total": { measurements: [{ value: 100 }] },
      "disk.free": { measurements: [{ value: 40 }] },
    });

    errors.processErrorMessages.mockImplementation((e: string) => `Processed error: ${e}`);

    wrapper = shallowMount(Actuator, {
      global: {
        plugins: [router],
        mocks: {
          $router: router,
        },
      },
    });

    // Wait for lifecycle hooks to finish
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick(); // sometimes needed due to async API chains
  });

  test("loads and computes diskspace correctly", () => {
    const disk = wrapper.vm.diskspace;
    expect(disk.total).toBe("100.00 bytes");
    expect(disk.free).toBe("40.00 bytes");
    expect(disk.used).toBe("60.00 bytes");
    expect(disk.percentage).toBe(60);
  });

  test("converts bytes correctly", () => {
    expect(wrapper.vm.convertBytes(0)).toBe("0.00 bytes");
    expect(wrapper.vm.convertBytes(1024)).toBe("1.00 KB");
    expect(wrapper.vm.convertBytes(1024 ** 2)).toBe("1.00 MB");
    expect(wrapper.vm.convertBytes(1024 ** 3)).toBe("1.00 GB");
  });

  test("handles missing measurements safely", async () => {
    // Override metrics to simulate missing keys
    api.getMetricsAll.mockResolvedValue({
      // empty object
    });

    const disk = wrapper.vm.diskspace;
    expect(disk.total).toBe("100.00 bytes");
    expect(disk.free).toBe("40.00 bytes");
    expect(disk.used).toBe("60.00 bytes");
    expect(disk.percentage).toBe(60);
  });
});
