import { mount } from '@vue/test-utils'
import ErrorLogLine from '@/components/ErrorLogLine.vue'

describe('ErrorLogLine.vue', () => {
  const logLine = `2025-08-22T14:30:00.000Z [INFO]  INFO] my.logger - USER_LOGIN [timestamp=2025-08-22T14:30:00.000Z, principal=JaneDoe, type=LOGIN_SUCCESS, data={ip=127.0.0.1, location=Office}]`;

  it('parses and renders logLine correctly', async () => {
    const wrapper = mount(ErrorLogLine, {
      props: { logLine }
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain('Fri, 22 Aug 2025 14:30:00 GMT');
    expect(wrapper.text()).toContain('JaneDoe');
    expect(wrapper.text()).toContain('LOGIN_SUCCESS');
    expect(wrapper.text()).toContain('my.logger');
    expect(wrapper.text()).toContain('USER_LOGIN');
    expect(wrapper.text()).toContain('ip:');
    expect(wrapper.text()).toContain('127.0.0.1');
    expect(wrapper.text()).toContain('location:');
    expect(wrapper.text()).toContain('Office');

    // Icon should match 'success'
    expect(wrapper.find('.bi-check-circle-fill').exists()).toBe(true);
    expect(wrapper.find('.bi-x-circle-fill').exists()).toBe(false);
    expect(wrapper.find('.bi-info-circle-fill').exists()).toBe(false);
  });

  it('toggles collapse state when button is clicked', async () => {
    const wrapper = mount(ErrorLogLine, {
      props: { logLine }
    });

    await wrapper.vm.$nextTick();

    // Initially collapsed
    expect(wrapper.find('.card-body').classes()).toContain('d-none');

    // Click toggle button
    await wrapper.find('button').trigger('click');
    expect(wrapper.find('.card-body').classes()).toContain('d-inline');

    // Click again to collapse
    await wrapper.find('button').trigger('click');
    expect(wrapper.find('.card-body').classes()).toContain('d-none');
  });

  it('shows failure icon for type containing FAILURE', async () => {
    const failureLogLine = logLine.replace('LOGIN_SUCCESS', 'LOGIN_FAILURE');
    const wrapper = mount(ErrorLogLine, {
      props: { logLine: failureLogLine }
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.find('.bi-x-circle-fill').exists()).toBe(true);
  });

  it('shows info icon for unknown type', async () => {
    const neutralLogLine = logLine.replace('LOGIN_SUCCESS', 'LOGIN');
    const wrapper = mount(ErrorLogLine, {
      props: { logLine: neutralLogLine }
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.find('.bi-info-circle-fill').exists()).toBe(true);
  });

  it('handles malformed logLine gracefully', async () => {
    const badLogLine = `Just a broken string`;

    const wrapper = mount(ErrorLogLine, {
      props: { logLine: badLogLine }
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain('Just a broken string');
    expect(wrapper.text()).not.toContain('action:');
    expect(wrapper.find('.bi-check-circle-fill').exists()).toBe(false);
    expect(wrapper.find('.bi-x-circle-fill').exists()).toBe(false);
  });
});
