package com.somosayni.asistente.infrastructure.persistence.mapper;

import com.somosayni.asistente.application.port.PostulacionContextoRepository;
import com.somosayni.asistente.domain.model.PostulacionContexto;
import com.somosayni.asistente.infrastructure.persistence.repository.JpaPostulacionSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PostulacionContextoRepositoryImpl implements PostulacionContextoRepository {

    private final JpaPostulacionSnapshotRepository jpaRepository;

    public PostulacionContextoRepositoryImpl(JpaPostulacionSnapshotRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<PostulacionContexto> obtenerPorId(String postulacionId) {
        return jpaRepository.findById(postulacionId)
                .map(s -> new PostulacionContexto(s.getId(), s.getTalentoId(), s.getRetoId()));
    }
}
