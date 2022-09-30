import { mount } from '@vue/test-utils'
import FeedbackMessage from '../../../src/components/FeedbackMessage.vue'


// The component to test
const MessageComponent = {
  template: '<p>{{ msg }}</p>',
  props: ['msg']
}
/**
 * @jest-environment jsdom
 */

test('displays message', () => {
  const wrapper = mount(FeedbackMessage, {
    props: {
      successMessage: 'Hello world',
      errorMessage: 'Goodbye world'
    }
  })

  // Assert the rendered text of the component
  expect(wrapper.text()).toContain('Hello world')
  expect(wrapper.text()).toContain('Goodbye world')
})