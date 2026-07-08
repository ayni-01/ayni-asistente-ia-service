package com.somosayni.asistente.application.query;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.application.port.HabilidadContextoRepository;
import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.HabilidadContexto;
import com.somosayni.asistente.domain.model.RecomendacionAprendizaje;
import com.somosayni.asistente.domain.model.RetoContexto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ObtenerRecomendacionesQueryHandler {

    private final HabilidadContextoRepository habilidadContextoRepository;
    private final RetoContextoRepository retoContextoRepository;
    private final AsistenteIAPort asistenteIAPort;

    public ObtenerRecomendacionesQueryHandler(
            HabilidadContextoRepository habilidadContextoRepository,
            RetoContextoRepository retoContextoRepository,
            AsistenteIAPort asistenteIAPort) {
        this.habilidadContextoRepository = habilidadContextoRepository;
        this.retoContextoRepository = retoContextoRepository;
        this.asistenteIAPort = asistenteIAPort;
    }

    public List<RecomendacionAprendizaje> handle(ObtenerRecomendacionesQuery query) {
        List<HabilidadContexto> habilidades = habilidadContextoRepository.obtenerPorTalentoId(query.talentoId());
        List<RetoContexto> retosActivos = retoContextoRepository.obtenerActivos();

        String habilidadesTexto = habilidades.isEmpty()
                ? "El talento aún no tiene habilidades validadas."
                : habilidades.stream()
                        .map(h -> "%s (nivel %s, %d%%)".formatted(h.nombre(), h.nivel(), h.porcentaje()))
                        .collect(Collectors.joining(", "));

        String categoriasDemandadas = retosActivos.stream()
                .map(RetoContexto::categoria)
                .distinct()
                .collect(Collectors.joining(", "));

        String systemPrompt = """
                Eres el asistente de IA de Somos Ayni, una plataforma de empleabilidad juvenil peruana.
                Generas recomendaciones de aprendizaje personalizadas para talentos jóvenes.
                Responde siempre con 3 a 5 recomendaciones concretas, en español.
                """;

        String userPrompt = """
                Habilidades validadas del talento: %s
                Categorías con retos activos en la plataforma: %s

                Recomienda temas de aprendizaje que le ayuden a cerrar la brecha entre lo que sabe
                y lo que la plataforma está demandando.
                """.formatted(habilidadesTexto, categoriasDemandadas);

        return asistenteIAPort.responderRecomendaciones(systemPrompt, userPrompt);
    }
}
