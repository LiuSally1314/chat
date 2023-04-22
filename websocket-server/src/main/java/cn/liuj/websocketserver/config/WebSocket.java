package cn.liuj.websocketserver.config;

import cn.liuj.websocketserver.model.WsMessage;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mac
 * @Description: WebSocket服务类，处理客户端的连接、断开和消息发送等操作
 */

@Slf4j
@Component // 注册为Spring组件
@ServerEndpoint("/websocket/{name}") // 标记为WebSocket服务端，并指定访问路径
public class WebSocket {

    /**
     * 与某个客户端的连接对话，需要通过它来给客户端发送消息
     */
    private Session session;

    /**
     * 标识当前连接客户端的用户名
     */
    private String name;

    /**
     * 用于存储所有连接到服务端的客户端，ConcurrentHashMap是线程安全的
     */
    private static ConcurrentHashMap<String, WebSocket> webSocketSet = new ConcurrentHashMap<>();

    /**
     * 当有新的客户端连接时触发
     *
     * @param session WebSocket会话对象
     * @param name    客户端名称
     */
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "name") String name) {
        this.session = session;
        if (this.name == null) {
            // 如果名称不合法，则返回一个错误信息并给出命名规范
            log.warn("[WebSocket] 检测到非法名称，关闭连接，名称为 '{}'", name);
            try {
                session.close();
            } catch (IOException e) {
                log.error("[WebSocket] 关闭非法连接时出错", e);
            }
            sendSystemMessageToUser(null, null, "系统提示：名称不合法，请使用字母、数字、下划线或破折号命名");
            return;
        }
        WebSocket existingSocket = webSocketSet.putIfAbsent(this.name, this);
        if (existingSocket != null) {
            // 如果已经有一个同名的socket连接，关闭新连接
            log.warn("[WebSocket] 检测到重复连接，关闭新连接，名称为 '{}'", this.name);
            try {
                session.close();
            } catch (IOException e) {
                log.error("[WebSocket] 关闭重复连接时出错", e);
            }
            sendSystemMessageToUser(this.name, null, "系统提示：名称已经存在，请使用其他名称");
            return;
        }
        sendSystemMessageToUser(this.name, null, "系统消息：连接成功");
        log.info("[WebSocket] 连接成功，当前连接人数为：={}", webSocketSet.size());
    }


    /**
     * 当有客户端断开连接时触发
     */
    @OnClose
    public void onClose() {
        webSocketSet.remove(this.name);
        log.info("[WebSocket] 客户端[{}]退出成功，当前连接人数为：={}", this.name, webSocketSet.size());
    }

    /**
     * 当接收到客户端发送的WebSocket消息时，会调用此方法进行处理。
     *
     * @param message 客户端发送的WebSocket消息
     */
    @OnMessage
    public void onMessage(String message) {
        // 检查消息是否为空或NULL
        if (message == null || message.isEmpty()) {
            log.error("[WebSocket] 收到的消息为空");
            sendSystemMessageToUser(this.name, null, "系统提示：接收到的消息为空");
            return;
        }

        // 打印日志，显示收到的消息内容
        log.info("[WebSocket] 收到客户端[{}]的消息：{}", this.name, message);

        try {
            JSONObject json = new JSONObject(message);
            String id = json.optString("id");
            String from = json.optString("from");
            String content = json.optString("content");

            if (id.isEmpty() || from.isEmpty() || content.isEmpty()) {
                log.error("[WebSocket] 接收到的消息格式错误: {}", message);
                sendSystemMessageToUser(this.name, id, "系统提示：接收到的消息格式错误，ID、发送方和消息内容均不能为空");
                return;
            }

            if (id.trim().isEmpty() || from.trim().isEmpty() || content.trim().isEmpty()) {
                log.error("[WebSocket] 接收到的消息格式错误: {}", message);
                sendSystemMessageToUser(this.name, id, "系统提示：接收到的消息格式错误，ID、发送方和消息内容均不能为空");
                return;
            }

            WsMessage wsMessage = new WsMessage(
                    id,
                    from,
                    json.optString("to"),
                    content,
                    LocalDateTime.now()
            );

            if (wsMessage.getTo() != null && !wsMessage.getTo().isEmpty()) {
                // 发送消息给指定客户端
                appointSending(wsMessage.getTo(), wsMessage.toJSONString());
                log.info("[WebSocket] 向客户端[{}]发送消息：{}", wsMessage.getTo(), wsMessage.toJSONString());
            } else {
                // 广播消息给所有客户端
                groupSend(wsMessage.toJSONString());
                log.info("[WebSocket] 广播消息：{}", wsMessage.toJSONString());
            }

        } catch (Exception e) {
            log.error("[WebSocket] 解析消息出错: {}", e.getMessage());
            log.error("[WebSocket] 接收到的消息格式错误: {}", message);
            sendSystemMessageToUser(this.name, null, "系统提示：接收到的消息格式错误，无法解析的JSON字符串或缺少必要属性");
        }
    }

    /**
     * 向指定用户发送系统消息
     */
    private void sendSystemMessageToUser(String toUserName, String wsMessageId, String message) {
        if (StringUtils.isEmpty(wsMessageId)) {
            wsMessageId = java.util.UUID.randomUUID().toString();
        }

        WsMessage wsMessage = new WsMessage(
                wsMessageId,
                "system",
                toUserName,
                message,
                LocalDateTime.now()
        );
        appointSending(toUserName, wsMessage.toJSONString());
    }


    /**
     * 群发消息
     *
     * @param message 消息内容
     */
    public void groupSend(String message) {
        for (String name : webSocketSet.keySet()) {
            try {
                WebSocket client = webSocketSet.get(name);
                synchronized (client) {
                    client.session.getBasicRemote().sendText(message); // 发送消息
                }
                log.info("[WebSocket] 向客户端[{}]发送消息：{}", client.name, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 指定客户端发送消息
     *
     * @param name    客户端名称
     * @param message 消息内容
     */
    public void appointSending(String name, String message) {
        try {
            WebSocket client = webSocketSet.get(name);
            if (client == null) {
                // 如果客户端不在线，则向原用户发送提示信息
                WsMessage wsMessage = WsMessage.fromJSONString(message);
                wsMessage.setTo(name);
                wsMessage.setContent("你所发的用户不在线");
                sendToSelf(wsMessage.toJSONString());
                return;
            }
            synchronized (client) {
                client.session.getBasicRemote().sendText(message); // 发送消息
            }
            log.info("[WebSocket] 向客户端[{}]发送消息：{}", client.name, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 向自身客户端发送消息
     *
     * @param message 消息内容
     */
    public void sendToSelf(String message) {
        try {
            synchronized (this) {
                session.getBasicRemote().sendText(message); // 发送消息
            }
            log.info("[WebSocket] 向客户端[{}]发送消息：{}", this.name, message);
        } catch (Exception e) {
            log.error("[WebSocket] 发送消息失败: {}", e.getMessage());
        }
    }
}

