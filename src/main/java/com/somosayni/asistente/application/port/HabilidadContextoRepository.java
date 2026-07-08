package com.somosayni.asistente.application.port;

import com.somosayni.asistente.domain.model.HabilidadContexto;

import java.util.List;

public interface HabilidadContextoRepository {
    List<HabilidadContexto> obtenerPorTalentoId(String talentoId);
}
