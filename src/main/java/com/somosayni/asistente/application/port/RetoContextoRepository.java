package com.somosayni.asistente.application.port;

import com.somosayni.asistente.domain.model.RetoContexto;

import java.util.Optional;

public interface RetoContextoRepository {
    Optional<RetoContexto> obtenerPorId(String retoId);
}
