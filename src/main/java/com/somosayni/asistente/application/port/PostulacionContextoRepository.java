package com.somosayni.asistente.application.port;

import com.somosayni.asistente.domain.model.PostulacionContexto;

import java.util.Optional;

public interface PostulacionContextoRepository {
    Optional<PostulacionContexto> obtenerPorId(String postulacionId);
}
