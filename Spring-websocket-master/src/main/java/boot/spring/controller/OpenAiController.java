package boot.spring.controller;

import boot.spring.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mac
 */
@RequestMapping("openai")
@RestController
public class OpenAiController {

    //添加openai的聊天接口

    @Autowired
    private OpenAiService openAiService;

    @PostMapping("/chat")
    public String chatWithOpenAI(@RequestBody String message) { // 接收前端发送的消息
        return openAiService.chatWithOpenAI(message);
    }


}
