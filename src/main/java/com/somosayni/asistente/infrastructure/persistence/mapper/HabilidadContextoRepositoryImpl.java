package com.somosayni.asistente.infrastructure.persistence.mapper;

import com.somosayni.asistente.application.port.HabilidadContextoRepository;
import com.somosayni.asistente.domain.model.HabilidadContexto;
import com.somosayni.asistente.infrastructure.persistence.repository.JpaHabilidadValidadaSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HabilidadContextoRepositoryImpl implements HabilidadContextoRepository {

    private final JpaHabilidadValidadaSnapshotRepository jpaRepository;

    public HabilidadContextoRepositoryImpl(JpaHabilidadValidadaSnapshotRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<HabilidadContexto> obtenerPorTalentoId(String talentoId) {
        return jpaRepository.findByTalentoId(talentoId).stream()
                .map(s -> new HabilidadContexto(s.getNombre(), s.getNivel(), s.getPorcentaje()))
                .toList();
    }
}
