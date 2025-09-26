import { mount } from '@vue/test-utils'
import AuditLogLine from '@/components/AuditLogLine.vue'

describe('AuditLogLine.vue', () => {
  it('parses and renders logLine props correctly', async () => {
    const logLine = `timestamp: 2025-08-22T14:30:00.000Z
principal: JohnDoe
type: LOGIN
data: {"ip": "192.168.0.1", "location": "NYC"}`

    const wrapper = mount(AuditLogLine, {
      props: {
        logLine,
      },
    });

    // Wait for mounted hook
    await wrapper.vm.$nextTick();

    // Expect timestamp to be converted to UTC format
    expect(wrapper.text()).toContain('Fri, 22 Aug 2025 14:30:00 GMT');
    expect(wrapper.text()).toContain('JohnDoe');
    expect(wrapper.text()).toContain('LOGIN');
    expect(wrapper.text()).toContain('ip:');
    expect(wrapper.text()).toContain('192.168.0.1');
    expect(wrapper.text()).toContain('location:');
    expect(wrapper.text()).toContain('NYC');
  });

  it('handles invalid logLine gracefully', async () => {
    const logLine = `invalid format`;

    const wrapper = mount(AuditLogLine, {
      props: {
        logLine,
      },
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.text()).not.toContain('JohnDoe');
  });
});
