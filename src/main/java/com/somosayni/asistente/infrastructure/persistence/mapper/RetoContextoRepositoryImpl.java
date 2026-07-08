package com.somosayni.asistente.infrastructure.persistence.mapper;

import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.RetoContexto;
import com.somosayni.asistente.infrastructure.persistence.entity.RetoSnapshot;
import com.somosayni.asistente.infrastructure.persistence.repository.JpaRetoSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RetoContextoRepositoryImpl implements RetoContextoRepository {

    private final JpaRetoSnapshotRepository jpaRepository;

    public RetoContextoRepositoryImpl(JpaRetoSnapshotRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RetoContexto> obtenerPorId(String retoId) {
        return jpaRepository.findById(retoId).map(this::toDomain);
    }

    @Override
    public List<RetoContexto> obtenerActivos() {
        return jpaRepository.findByEstado("ACTIVO").stream().map(this::toDomain).toList();
    }

    private RetoContexto toDomain(RetoSnapshot snapshot) {
        List<String> requisitos = snapshot.getRequisitos() == null ? List.of() : snapshot.getRequisitos();
        return new RetoContexto(
                snapshot.getId(),
                snapshot.getTitulo(),
                snapshot.getDescripcion(),
                snapshot.getCategoria(),
                snapshot.getNivelDificultad(),
                requisitos);
    }
}
