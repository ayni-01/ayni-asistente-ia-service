package com.somosayni.asistente.infrastructure.ai.tool;

import com.somosayni.asistente.application.port.HabilidadContextoRepository;
import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.HabilidadContexto;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TalentoTool {

    private final HabilidadContextoRepository habilidadContextoRepository;
    private final RetoContextoRepository retoContextoRepository;

    public TalentoTool(HabilidadContextoRepository habilidadContextoRepository,
                       RetoContextoRepository retoContextoRepository) {
        this.habilidadContextoRepository = habilidadContextoRepository;
        this.retoContextoRepository = retoContextoRepository;
    }

    @Tool(description = "Devuelve las habilidades validadas del talento (nombre, nivel, porcentaje). Úsala para conocer su nivel actual antes de recomendar.")
    public List<HabilidadContexto> habilidadesDelTalento(
            @ToolParam(description = "id del talento") String talentoId) {
        return habilidadContextoRepository.obtenerPorTalentoId(talentoId);
    }

    @Tool(description = "Devuelve los retos activos de la plataforma como 'titulo — categoria (nivel)'. Úsala para saber qué habilidades está demandando el mercado.")
    public List<String> retosActivos() {
        return retoContextoRepository.obtenerActivos().stream()
                .map(r -> "%s — %s (%s)".formatted(r.titulo(), r.categoria(), r.nivelDificultad()))
                .toList();
    }
}
