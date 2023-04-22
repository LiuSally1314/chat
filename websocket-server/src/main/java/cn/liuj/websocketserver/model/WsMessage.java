package cn.liuj.websocketserver.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * WebSocket 消息类。
 * @author mac
 */
@Data
@NoArgsConstructor
public class WsMessage {

    // ObjectMapper对象，用于序列化和反序列化JSON
    private static final ObjectMapper mapper = new ObjectMapper();

    // 消息 ID
    private String id;

    // 消息发送方
    private String from;

    // 消息接收方
    private String to;

    // 消息内容
    private String content;

    // 发送时间
    private LocalDateTime sendTime;

    public WsMessage(String id, String from, String to, String content, LocalDateTime sendTime) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.content = content;
        this.sendTime = sendTime;
    }

    /**
     * 将当前对象转换为 JSON 格式的字符串
     *
     * @return JSON 格式的字符串
     * @throws RuntimeException 序列化异常
     */
    public String toJSONString() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将JSON格式的字符串解析成WebSocketMessage对象
     *
     * @param json JSON格式的字符串
     * @return WebSocketMessage对象
     * @throws RuntimeException 反序列化异常
     */
    public static WsMessage fromJSONString(String json) {
        try {
            return mapper.readValue(json, WsMessage.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
