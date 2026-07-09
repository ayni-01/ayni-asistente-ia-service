package com.somosayni.asistente.infrastructure.ai;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.domain.model.RecomendacionAprendizaje;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpringAiAsistenteAdapter implements AsistenteIAPort {

    private final ChatClient chatClient;
    private final ChatClient recomendacionesChatClient;

    public SpringAiAsistenteAdapter(ChatClient.Builder chatClientBuilder,
                                    @Qualifier("recomendacionesChatClient") ChatClient recomendacionesChatClient) {
        this.chatClient = chatClientBuilder.build();
        this.recomendacionesChatClient = recomendacionesChatClient;
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

    @Override
    public List<RecomendacionAprendizaje> recomendarConRag(String talentoId, String contextoRecursos) {
        String userPrompt = """
                Talento: %s

                Recursos disponibles (catálogo):
                %s

                Recomienda entre 3 y 5 recursos de aprendizaje para este talento, basándote SOLO en el
                catálogo anterior y en su perfil. Usa las herramientas para conocer sus habilidades y los
                retos activos de la plataforma. Para cada recomendación indica tema, motivo, nivelSugerido,
                recursoTitulo y recursoUrl tomados del catálogo.
                """.formatted(talentoId, contextoRecursos);

        return recomendacionesChatClient.prompt()
                .user(userPrompt)
                .call()
                .entity(new ParameterizedTypeReference<List<RecomendacionAprendizaje>>() {});
    }
}
