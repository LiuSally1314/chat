package cn.liuj.websocketclient.websocketclient.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Component
@Slf4j
@Data
public class WebSocketClientFactory {

    // WebSocket服务器地址
    public static final String OUT_CALL_WEB_SOCKET_URL = "ws://IP:端口";

    private WebSocketClient outCallWebSocketClientHolder;

    /**
     * 创建新的WebSocket客户端
     *
     * @return WebSocketClient
     * @throws URISyntaxException
     */
    private WebSocketClient createNewWebSocketClient() throws URISyntaxException {
        WebSocketClient webSocketClient = new WebSocketClient(new URI(OUT_CALL_WEB_SOCKET_URL)) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                log.info("WebSocket连接已打开");
            }

            @Override
            public void onMessage(String msg) {
                log.info("接收到WebSocket消息: {}", msg);
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                log.info("WebSocket连接已关闭，i={}, s={}, b={}", i, s, b);
                retryOutCallWebSocketClient();
            }

            @Override
            public void onError(Exception e) {
                log.error("WebSocket发生异常: {}", e.getMessage());
                retryOutCallWebSocketClient();
            }
        };
        webSocketClient.connect();
        return webSocketClient;
    }

    /**
     * 获取WebSocket客户端实例
     * 需要加同步，避免创建多个连接
     */
    public synchronized WebSocketClient getOutCallWebSocketClientHolder() {
        if (outCallWebSocketClientHolder == null ||
                !outCallWebSocketClientHolder.getReadyState().equals(WebSocket.READYSTATE.OPEN)) {
            // 创建新的WebSocket连接并进行认证
            outCallWebSocketClientHolder = retryOutCallWebSocketClient();
        }
        return outCallWebSocketClientHolder;
    }

    /**
     * 关闭WebSocket连接
     */
    public void closeWebSocketConnection() {
        WebSocketClient webSocketClient = getOutCallWebSocketClientHolder();
        if (webSocketClient != null) {
            try {
                webSocketClient.close();
            } catch (Exception e) {
                log.error("关闭WebSocket连接失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 重试连接WebSocket服务器
     */
    public WebSocketClient retryOutCallWebSocketClient() {
        try {
            // 关闭旧的WebSocket连接，避免占用资源
            WebSocketClient oldOutCallWebSocketClientHolder = this.getOutCallWebSocketClientHolder();
            if (oldOutCallWebSocketClientHolder != null) {
                log.info("关闭旧的WebSocket连接");
                oldOutCallWebSocketClientHolder.close();
            }

            log.info("打开新的WebSocket连接，并进行认证");
            WebSocketClient webSocketClient = this.createNewWebSocketClient();

            // 进行认证
            String sendOpenJsonStr = "{\"event\":\"connect\",\"sid\":\"1ae4e3167b3b49c7bfc6b79awww691562914214595\",\"token\":\"df59eba89\"}";
            this.sendMsg(webSocketClient, sendOpenJsonStr);

            // 每次创建新的就放进去
            this.setOutCallWebSocketClientHolder(webSocketClient);
            return webSocketClient;
        } catch (URISyntaxException e) {
            log.error(e.getMessage());
        }
        return null;
    }


    /**
     * 发送消息
     * 注意：要加超时设置，避免很多个都在同时超时占用资源
     *
     * @param webSocketClient WebSocket客户端实例
     * @param message         消息内容
     */
    public void sendMsg(WebSocketClient webSocketClient, String message) {
        log.info("发送WebSocket消息: {}", message);
        long startOpenTimeMillis = System.currentTimeMillis();
        while (!webSocketClient.getReadyState().equals(WebSocket.READYSTATE.OPEN)) {
            log.debug("正在建立WebSocket连接，请稍等");
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - startOpenTimeMillis >= 5000) {
                log.error("WebSocket连接超时，超过5秒钟未打开连接，不再等待");
                return;
            }
        }
        webSocketClient.send(message);
    }

    /**
     * 设置WebSocket客户端实例
     *
     * @param outCallWebSocketClientHolder WebSocket客户端实例
     */
    public void setOutCallWebSocketClientHolder(WebSocketClient outCallWebSocketClientHolder) {
        this.outCallWebSocketClientHolder = outCallWebSocketClientHolder;
    }


    /**
     * 定时发送WebSocket心跳消息
     */
//    @Async
//    @Scheduled(fixedRate = 10000)
    public void sendHeartBeat() {
        log.info("定时发送WebSocket心跳");
        try {
            WebSocketClient outCallWebSocketClientHolder = this.getOutCallWebSocketClientHolder();

            if (null == outCallWebSocketClientHolder) {
                log.info("WebSocket连接还未建立，暂不发送心跳消息");
                return;
            }

            // 发送心跳消息
            String heartBeatMsg = "{\"event\":\"heartbeat\",\"sid\":\"1ae4e3167b3b49c7bfc6b79a74f2296915222214595\"}";
            this.sendMsg(outCallWebSocketClientHolder, heartBeatMsg);
        } catch (Exception e) {
            log.error("发送WebSocket心跳失败: {}", e.getMessage());
            retryOutCallWebSocketClient();
        }
    }

}