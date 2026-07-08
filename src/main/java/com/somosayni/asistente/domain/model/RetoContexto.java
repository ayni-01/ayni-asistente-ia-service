package com.somosayni.asistente.domain.model;

import java.util.List;

public record RetoContexto(
        String id,
        String titulo,
        String descripcion,
        String categoria,
        String nivelDificultad,
        List<String> requisitos
) {}
