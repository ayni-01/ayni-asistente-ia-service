package com.somosayni.asistente.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record FeedbackSolucionRequest(
        @NotBlank(message = "El enfoque de solución es obligatorio") String enfoqueSolucion
) {}
