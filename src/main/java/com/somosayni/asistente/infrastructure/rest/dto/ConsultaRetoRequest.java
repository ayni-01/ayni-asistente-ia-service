package com.somosayni.asistente.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsultaRetoRequest(@NotBlank(message = "La pregunta es obligatoria") String pregunta) {}
