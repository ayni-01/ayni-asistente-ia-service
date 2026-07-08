package com.somosayni.asistente.application.query;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.RetoContexto;
import org.springframework.stereotype.Component;

@Component
public class ConsultarRetoQueryHandler {

    private final RetoContextoRepository retoContextoRepository;
    private final AsistenteIAPort asistenteIAPort;

    public ConsultarRetoQueryHandler(RetoContextoRepository retoContextoRepository, AsistenteIAPort asistenteIAPort) {
        this.retoContextoRepository = retoContextoRepository;
        this.asistenteIAPort = asistenteIAPort;
    }

    public String handle(ConsultarRetoQuery query) {
        RetoContexto reto = retoContextoRepository.obtenerPorId(query.retoId())
                .orElseThrow(() -> new IllegalArgumentException("Reto no encontrado: " + query.retoId()));

        String systemPrompt = """
                Eres el asistente de IA de Somos Ayni, una plataforma de empleabilidad juvenil peruana.
                Ayudas a talentos a entender un reto práctico antes de postular.
                Responde en español, de forma clara y concisa, basándote solo en el contexto del reto dado.
                """;

        String userPrompt = """
                Reto: %s
                Descripción: %s
                Categoría: %s
                Nivel de dificultad: %s
                Requisitos: %s

                Pregunta del talento: %s
                """.formatted(
                reto.titulo(),
                reto.descripcion(),
                reto.categoria(),
                reto.nivelDificultad(),
                String.join(", ", reto.requisitos()),
                query.pregunta());

        return asistenteIAPort.responder(systemPrompt, userPrompt);
    }
}
