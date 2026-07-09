package com.somosayni.asistente.infrastructure.ai.config;

import com.somosayni.asistente.infrastructure.ai.tool.TalentoTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient recomendacionesChatClient(ChatClient.Builder builder, TalentoTool talentoTool) {
        return builder
                .defaultSystem("""
                        Eres el asistente de IA de Somos Ayni, plataforma de empleabilidad juvenil peruana.
                        Recomiendas recursos de aprendizaje a un talento, anclándote SOLO en el catálogo de
                        recursos que se te entrega como contexto. Usa las herramientas para conocer las
                        habilidades del talento y los retos activos de la plataforma, e identificar sus
                        brechas. Responde en español y no inventes recursos fuera del catálogo.
                        """)
                .defaultTools(talentoTool)
                .build();
    }
}
