import { mount, VueWrapper } from '@vue/test-utils';
import ExtraMetricsEndpoint from '@/components/ExtraMetricsEndpoint.vue';
import { nextTick } from 'vue';
import { APISettings } from '@/api/config';

global.fetch = jest.fn(() =>
  Promise.resolve({
    text: () => Promise.resolve({ message: "success" }),
  }),
) as jest.Mock;

jest.mock('@/components/LoadingSpinner.vue', () => {
  const vue = require('vue');
  return vue.defineComponent({
    name: 'LoadingSpinner',
    render() {
      return vue.h('div', { class: 'spinner-mock' });
    },
  });
});

jest.mock('@/components/FeedbackMessage.vue', () => {
  const vue = require('vue');
  return {
    default: vue.defineComponent({
      name: 'FeedbackMessage',
      props: ['errorMessage', 'successMessage'],
      render() {
        return vue.h('div', { class: 'feedback-mock' }, this.errorMessage);
      },
    }),
  };
});


describe('ExtraMetricsEndpoint.vue', () => {
  const nonTemplatedEndpoint = {
    key: 'Users',
    href: '/api/users',
    templated: false,
  };

  const templatedEndpoint = {
    key: 'Search',
    href: '/api/search/{?query}',
    templated: true,
  };

  function mountComponent(endpoint: any): VueWrapper<any> {
    return mount(ExtraMetricsEndpoint, {
      props: { endpoint },
    });
  }

  it('renders non-templated endpoint properly', () => {
    const wrapper = mountComponent(nonTemplatedEndpoint);
    expect(wrapper.text()).toContain('Users');
    expect(wrapper.text()).toContain('/api/users');
    expect(wrapper.find('a.btn-primary').exists()).toBe(true);
    expect(wrapper.find('form').exists()).toBe(false);
  });

  it('renders templated endpoint with argument input', () => {
    const wrapper = mountComponent(templatedEndpoint);
    expect(wrapper.text()).toContain('Search');
    expect(wrapper.find('form').exists()).toBe(true);
    expect(wrapper.find('input.form-control').exists()).toBe(true);
  });

  it('disables Execute button when input is empty for templated endpoint', async () => {
    const wrapper = mountComponent(templatedEndpoint);
    const button = wrapper.find('button.btn-success');
    expect(button.attributes('disabled')).toBeDefined();
  });

  it('enables Execute button when input is provided for templated endpoint', async () => {
    const wrapper = mountComponent(templatedEndpoint);
    await wrapper.setData({ argumentInput: 'hello' });
    const button = wrapper.find('button.btn-success');
    expect(button.attributes('disabled')).toBeUndefined();
  });

  it('calls get() and shows result on button click', async () => {
    const wrapper = mountComponent(templatedEndpoint);
    await wrapper.setData({ argumentInput: 'world' });

    await wrapper.find('button.btn-success').trigger('click');
    expect(fetch).toHaveBeenCalledWith('/api/search/world', {"headers": APISettings.headers, "method": "GET"});

    await nextTick();
    await nextTick();
    await nextTick(); // wait for all state updates

    expect(wrapper.text()).toContain('success');
    expect(wrapper.find('pre').exists()).toBe(true);
  });

  it('clears result when clearResult button is clicked', async () => {
    const wrapper = mountComponent(templatedEndpoint);
    await wrapper.setData({ result: { foo: 'bar' }, argumentInput: 'test' });

    expect(wrapper.find('pre').exists()).toBe(true);
    await wrapper.find('button.btn-danger').trigger('click');
    expect(wrapper.find('pre').exists()).toBe(false);
  });

  it('computed argument returns correct placeholder from href', () => {
    const wrapper = mountComponent(templatedEndpoint);
    expect(wrapper.vm.argument).toBe('?query');
  });

  it('splittedEndpoint computed splits href correctly', () => {
    const wrapper = mountComponent({
      ...templatedEndpoint,
      href: '/api/test/{id}/details',
    });

    expect(wrapper.vm.splittedEndpoint).toEqual([
      '/api/test/',
      'id',
      '/details',
    ]);
  });

  it('Execute button is disabled when result is non-empty', async () => {
    const wrapper = mountComponent(templatedEndpoint);
    await wrapper.setData({ argumentInput: 'hello', result: { some: 'data' } });

    const button = wrapper.find('button.btn-success');
    expect(button.attributes('disabled')).toBeDefined();
  });
});
