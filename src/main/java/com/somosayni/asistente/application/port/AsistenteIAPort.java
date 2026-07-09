package com.somosayni.asistente.application.port;

import com.somosayni.asistente.domain.model.RecomendacionAprendizaje;

import java.util.List;

public interface AsistenteIAPort {
    String responder(String systemPrompt, String userPrompt);
    List<RecomendacionAprendizaje> responderRecomendaciones(String systemPrompt, String userPrompt);
    List<RecomendacionAprendizaje> recomendarConRag(String talentoId, String contextoRecursos);
}
