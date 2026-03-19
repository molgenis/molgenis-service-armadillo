import { mount, VueWrapper } from '@vue/test-utils';
import MetricsCard from '@/components/MetricsCard.vue';

describe('MetricsCard.vue', () => {
  const basicMetric = {
    name: 'test_metric',
    description: 'This is a percent metric',
    baseUnit: 'percent',
    measurements: [
      {
        statistic: 'max',
        value: 0.75,
      },
    ],
    _display: true,
  };

  const props = {
    item: 'test',
    icon: 'bar-chart',
    metrics: {
      'metrics.test_metric': basicMetric,
    },
  };

  it('renders the icon and title correctly', () => {
    const wrapper = mount(MetricsCard, { props });
    expect(wrapper.find('i.bi-bar-chart').exists()).toBe(true);
    expect(wrapper.text()).toContain('Test'); // Capitalised item
  });

  it('renders metric name and description', () => {
    const wrapper = mount(MetricsCard, { props });
    expect(wrapper.text()).toContain('test_metric');
    expect(wrapper.text()).toContain('This is a percent metric');
  });

  it('renders percentage value correctly', () => {
    const wrapper = mount(MetricsCard, { props });
    expect(wrapper.text()).toContain('75%'); // 0.75 * 100
  });

  it('renders stopwatch icon for time-based stats', () => {
    const timeMetric = {
      ...basicMetric,
      baseUnit: 'ms',
      measurements: [
        { statistic: 'duration', value: 123 },
      ],
    };
    const wrapper = mount(MetricsCard, {
      props: {
        ...props,
        metrics: { 'metrics.time_metric': timeMetric },
      },
    });

    expect(wrapper.find('i.bi-stopwatch').exists()).toBe(true);
    expect(wrapper.text()).toContain('123');
    expect(wrapper.text()).toContain('ms');
  });

  it('renders memory icon if name or description contains "memory"', () => {
  const memoryMetric = {
    name: 'test_memory_usage', // must start with 'test'
    description: 'Amount of memory used',
    baseUnit: 'bytes',
    measurements: [{ statistic: 'used', value: 1048576 }],
    _display: true,
  };

  const wrapper = mount(MetricsCard, {
    props: {
      item: 'test',
      icon: 'bar-chart',
      metrics: { memory: memoryMetric },
    },
  });

  expect(wrapper.find('i.bi-memory').exists()).toBe(true);
  expect(wrapper.text()).toContain('1.00 MB');
});

  it('renders file and directory icons correctly', () => {
  const fileMetric = {
    name: 'test_file_metric', // starts with 'test'
    description: 'Number of files',
    baseUnit: 'files',
    measurements: [{ statistic: 'count', value: 5 }],
    _display: true,
  };

  const dirMetric = {
    name: 'test_directory_metric', // starts with 'test'
    description: 'Number of folders',
    baseUnit: 'directories',
    measurements: [{ statistic: 'count', value: 2 }],
    _display: true,
  };

  const wrapper = mount(MetricsCard, {
    props: {
      item: 'test',
      icon: 'bar-chart',
      metrics: {
        fileMetric,
        dirMetric,
      },
    },
  });

  expect(wrapper.find('i.bi-files').exists()).toBe(true);
  expect(wrapper.find('i.bi-folder').exists()).toBe(true);
});

  it('does not render metrics without _display or incorrect name prefix', () => {
    const hiddenMetric = {
      ...basicMetric,
      name: 'other_metric',
      _display: false,
    };

    const wrapper = mount(MetricsCard, {
      props: {
        ...props,
        metrics: {
          'metrics.hidden': hiddenMetric,
        },
      },
    });

    expect(wrapper.text()).not.toContain('other_metric');
  });

  it('renders preview value label when statistic is not VALUE', () => {
    const wrapper = mount(MetricsCard, { props });
    expect(wrapper.text()).toContain('Max:'); // "max" becomes "Max"
  });

  it('renders base unit if one measurement or time unit', () => {
    const wrapper = mount(MetricsCard, { props });
    expect(wrapper.text()).toContain('percent'); // baseUnit
  });
describe("methods", () => {
  const wrapper: VueWrapper<any> = mount(MetricsCard, {
      props: props,
      icon: 'window'
    });
    describe("isFileSizeUnit", () => {
      test("is not filesize unit", () => {
        expect(wrapper.vm.isFileSizeUnit("blaat")).toBe(false);
      });
      test("is  filesize unit", () => {
        expect(wrapper.vm.isFileSizeUnit("bytes")).toBe(true);
      });
    })

    describe("isOnlyOneMeasurement", () => {
      test("more than one measurement is false", () => {
        expect(wrapper.vm.isOnlyOneMeasurement([{}, {}])).toBe(false);
      });
      test("no measurements is false", () => {
        expect(wrapper.vm.isOnlyOneMeasurement([])).toBe(false);
      });
      test("has only one measurement is true", () => {
        expect(wrapper.vm.isOnlyOneMeasurement([{}])).toBe(true);
      });
    })

    describe("isTimeUnit", () => {
      test("when statistic contains time returns true", () => {
        expect(wrapper.vm.isTimeUnit("time after time", [], "something"), ).toBe(true);
      })
      test("when statistic contains duration returns true", () => {
        expect(wrapper.vm.isTimeUnit("duration is in here", [], "something"), ).toBe(true);
      })

      test("when statistic contains max returns true", () => {
        expect(wrapper.vm.isTimeUnit("maximum", [], "something"), ).toBe(true);
      })
    })

    describe("isPreviewValue", () => {
      test("is not preview value", () => {
        expect(wrapper.vm.isPreviewValue("VALUE")).toBe(false);
      });
      test("is  preview value", () => {
        expect(wrapper.vm.isPreviewValue("blaatblaat")).toBe(true);
      });
    })

    describe("isFiles", () => {
      test("isFiles returns false if unit doesnt imply files", () => {
        expect(wrapper.vm.isFiles("UNIT")).toBe(false);
      });
      test("isFiles returns true if unit implies files", () => {
        expect(wrapper.vm.isFiles("files")).toBe(true);
      });
    })

    describe("isDirectory", () => {
      test("isDirectory returns false if unit doesnt imply directory", () => {
        expect(wrapper.vm.isDirectory("UNIT")).toBe(false);
      });
      test("isDirectory returns true if unit implies directory", () => {
        expect(wrapper.vm.isDirectory("directories")).toBe(true);
      });
    })

    describe("getCapitalisedValue", () => {
      test("getCapitalisedValue returns value starting with capitalised character and other characters in lowercase", () => {
        expect(wrapper.vm.getCapitalisedValue("SOMETHING UPPERCASE lowercase MixED CaSe")).toBe("Something uppercase lowercase mixed case");
      });
    })

    describe("isMemory", () => {
      test("isMemory returns true when memory in description", () => {
        expect(wrapper.vm.isMemory("something", "something memory something")).toBe(true);
      });

      test("isMemory returns true when memory in name", () => {
        expect(wrapper.vm.isMemory("memory", "something something")).toBe(true);
      });

      test("isMemory returns true when memory in name and description", () => {
        expect(wrapper.vm.isMemory("memory", "something something memory")).toBe(true);
      });

      test("isMemory returns false when memory in neither name nor description", () => {
        expect(wrapper.vm.isMemory("something", "something something")).toBe(false);
      });
    })

     describe("isPercentage", () => {
      test("isPercentage returns false if description doesnt include percent", () => {
        expect(wrapper.vm.isPercentage("very extensive description")).toBe(false);
      });
      test("isPercentage returns true if description includes percent", () => {
        expect(wrapper.vm.isPercentage("something something percent something")).toBe(true);
      });
    })

    describe("toPercentage", () => {
      test("toPercentage returns percentage of value as string", () => {
        expect(wrapper.vm.toPercentage(0.89667)).toBe("89.667%");
      });
    })
  })
});
