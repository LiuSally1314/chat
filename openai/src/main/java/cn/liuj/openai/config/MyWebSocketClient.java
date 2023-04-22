package cn.liuj.openai.config;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

@Service
public class MyWebSocketClient {

    private final WebSocketClient webSocketClient;

    public MyWebSocketClient(WebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
    }

    public void connect() throws Exception {
        URI uri = new URI("ws://localhost:8080/my-ws-endpoint");
        webSocketClient.doHandshake(new MyWebSocketHandler(), null, uri).get();
    }

    private static class MyWebSocketHandler extends TextWebSocketHandler {
        
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            // 处理接收到的消息逻辑
        }
    }
}
