package com.somosayni.asistente.infrastructure.ai;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.domain.model.RecomendacionAprendizaje;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Override
    public List<RecomendacionAprendizaje> responderRecomendaciones(String systemPrompt, String userPrompt) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(new ParameterizedTypeReference<List<RecomendacionAprendizaje>>() {});
    }
}
