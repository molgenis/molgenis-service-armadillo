package org.molgenis.datashield;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  @Bean
  public ServletServerContainerFactoryBean createWebSocketContainer() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    container.setMaxTextMessageBufferSize(8192);
    container.setMaxBinaryMessageBufferSize(8192);
    container.setMaxSessionIdleTimeout(10000L); // 10 seconds for demo purposes
    return container;
  }

  @Bean
  public WebSocketHandler echoHandler() {
    return new AbstractWebSocketHandler() {
      @Override
      protected void handleTextMessage(WebSocketSession session, TextMessage message)
          throws Exception {
        String payload = message.getPayload();
        session.sendMessage(new TextMessage("Hello " + session.getPrincipal().getName()));
        session.sendMessage(new TextMessage("Datashield: " + payload));

      }
    };
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
        .addHandler(echoHandler(), "/ws/echo")
        .setAllowedOrigins("nu.nl")
        .addInterceptors(new HttpSessionHandshakeInterceptor());
  }
}