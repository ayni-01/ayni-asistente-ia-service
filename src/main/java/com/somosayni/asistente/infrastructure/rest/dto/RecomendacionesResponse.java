package com.somosayni.asistente.infrastructure.rest.dto;

import com.somosayni.asistente.domain.model.RecomendacionAprendizaje;

import java.util.List;

public record RecomendacionesResponse(List<RecomendacionAprendizaje> recomendaciones) {}
