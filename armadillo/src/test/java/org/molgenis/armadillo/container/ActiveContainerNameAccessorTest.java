package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ActiveContainerNameAccessorTest {

  @AfterEach
  void resetContext() {
    RequestContextHolder.resetRequestAttributes();
    ActiveContainerNameAccessor.resetActiveContainerName();
  }

  @Test
  void constructor_throwsUnsupportedOperationException() {
    assertThrows(UnsupportedOperationException.class, this::instantiateViaReflection);
  }

  private void instantiateViaReflection() {
    try {
      var ctor = ActiveContainerNameAccessor.class.getDeclaredConstructor();
      ctor.setAccessible(true);
      ctor.newInstance();
    } catch (Exception e) {
      if (e.getCause() instanceof RuntimeException re) {
        throw re;
      }
      throw new RuntimeException(e);
    }
  }

  @Test
  void setAndGet_usesThreadLocalWhenNoRequestAttributes() {
    ActiveContainerNameAccessor.setActiveContainerName("c1");

    assertEquals("c1", ActiveContainerNameAccessor.getActiveContainerName());

    ActiveContainerNameAccessor.resetActiveContainerName();
    assertEquals(
        ActiveContainerNameAccessor.DEFAULT, ActiveContainerNameAccessor.getActiveContainerName());
  }

  @Test
  void setAndGet_usesSessionWhenRequestAttributesPresent() {
    var request = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    ActiveContainerNameAccessor.setActiveContainerName("session-container");

    HttpSession session = request.getSession(false);
    assertEquals("session-container", session.getAttribute("container"));
    assertEquals("session-container", ActiveContainerNameAccessor.getActiveContainerName());
  }
}
