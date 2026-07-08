package com.somosayni.asistente.infrastructure.ai;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class SpringAiAsistenteAdapter implements AsistenteIAPort {

    private final ChatClient chatClient;

    public SpringAiAsistenteAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String responder(String systemPrompt, String userPrompt) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }
}
