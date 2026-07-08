package com.somosayni.asistente.application.query;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.application.port.PostulacionContextoRepository;
import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.PostulacionContexto;
import com.somosayni.asistente.domain.model.RetoContexto;
import org.springframework.stereotype.Component;

@Component
public class ObtenerFeedbackSolucionQueryHandler {

    private final PostulacionContextoRepository postulacionContextoRepository;
    private final RetoContextoRepository retoContextoRepository;
    private final AsistenteIAPort asistenteIAPort;

    public ObtenerFeedbackSolucionQueryHandler(
            PostulacionContextoRepository postulacionContextoRepository,
            RetoContextoRepository retoContextoRepository,
            AsistenteIAPort asistenteIAPort) {
        this.postulacionContextoRepository = postulacionContextoRepository;
        this.retoContextoRepository = retoContextoRepository;
        this.asistenteIAPort = asistenteIAPort;
    }

    public String handle(ObtenerFeedbackSolucionQuery query) {
        PostulacionContexto postulacion = postulacionContextoRepository.obtenerPorId(query.postulacionId())
                .orElseThrow(() -> new IllegalArgumentException("Postulación no encontrada: " + query.postulacionId()));

        if (!postulacion.talentoId().equals(query.talentoId())) {
            throw new IllegalArgumentException("La postulación no pertenece al talento autenticado");
        }

        RetoContexto reto = retoContextoRepository.obtenerPorId(postulacion.retoId())
                .orElseThrow(() -> new IllegalArgumentException("Reto no encontrado: " + postulacion.retoId()));

        String systemPrompt = """
                Eres el asistente de IA de Somos Ayni, una plataforma de empleabilidad juvenil peruana.
                Das retroalimentación constructiva sobre el enfoque de solución que un talento propone
                para un reto, antes de que lo entregue. Responde en español: menciona fortalezas,
                riesgos y sugerencias concretas.
                """;

        String userPrompt = """
                Reto: %s
                Descripción: %s
                Requisitos: %s

                Enfoque de solución propuesto por el talento: %s
                """.formatted(
                reto.titulo(),
                reto.descripcion(),
                String.join(", ", reto.requisitos()),
                query.enfoqueSolucion());

        return asistenteIAPort.responder(systemPrompt, userPrompt);
    }
}
