package com.somosayni.asistente.application.query;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.application.port.CatalogoRecursosPort;
import com.somosayni.asistente.application.port.HabilidadContextoRepository;
import com.somosayni.asistente.domain.model.HabilidadContexto;
import com.somosayni.asistente.domain.model.RecomendacionAprendizaje;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ObtenerRecomendacionesQueryHandler {

    private final HabilidadContextoRepository habilidadContextoRepository;
    private final CatalogoRecursosPort catalogoRecursosPort;
    private final AsistenteIAPort asistenteIAPort;

    public ObtenerRecomendacionesQueryHandler(
            HabilidadContextoRepository habilidadContextoRepository,
            CatalogoRecursosPort catalogoRecursosPort,
            AsistenteIAPort asistenteIAPort) {
        this.habilidadContextoRepository = habilidadContextoRepository;
        this.catalogoRecursosPort = catalogoRecursosPort;
        this.asistenteIAPort = asistenteIAPort;
    }

    public List<RecomendacionAprendizaje> handle(ObtenerRecomendacionesQuery query) {
        List<HabilidadContexto> habilidades = habilidadContextoRepository.obtenerPorTalentoId(query.talentoId());

        String consulta = habilidades.isEmpty()
                ? "recursos de aprendizaje para un talento junior de tecnología"
                : "recursos de aprendizaje afines a: " + habilidades.stream()
                        .map(HabilidadContexto::nombre)
                        .collect(Collectors.joining(", "));

        String contexto = catalogoRecursosPort.buscarRelevantes(consulta, 5);

        return asistenteIAPort.recomendarConRag(query.talentoId(), contexto);
    }
}
