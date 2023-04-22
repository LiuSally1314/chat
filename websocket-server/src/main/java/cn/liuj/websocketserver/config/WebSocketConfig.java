package cn.liuj.websocketserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket // 启用WebSocket支持
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册WebSocket处理程序，将“/my-websocket”端点映射到处理程序
        registry.addHandler(myWebSocketHandler(), "/my-websocket");
    }

    @Bean
    public WebSocketHandler myWebSocketHandler() {
        // 创建WebSocket处理程序实例
        return new MyWebSocketHandler();
    }
}
