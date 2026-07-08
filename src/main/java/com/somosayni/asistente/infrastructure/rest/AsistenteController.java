package com.somosayni.asistente.infrastructure.rest;

import com.somosayni.asistente.application.query.*;
import com.somosayni.asistente.infrastructure.rest.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/asistente")
public class AsistenteController {

    private final ConsultarRetoQueryHandler consultarRetoHandler;
    private final ObtenerRecomendacionesQueryHandler recomendacionesHandler;
    private final ObtenerFeedbackSolucionQueryHandler feedbackHandler;

    public AsistenteController(
            ConsultarRetoQueryHandler consultarRetoHandler,
            ObtenerRecomendacionesQueryHandler recomendacionesHandler,
            ObtenerFeedbackSolucionQueryHandler feedbackHandler) {
        this.consultarRetoHandler = consultarRetoHandler;
        this.recomendacionesHandler = recomendacionesHandler;
        this.feedbackHandler = feedbackHandler;
    }

    @PostMapping("/retos/{retoId}/consulta")
    public ResponseEntity<ConsultaRetoResponse> consultarSobreReto(
            @PathVariable String retoId,
            @Valid @RequestBody ConsultaRetoRequest request) {
        String respuesta = consultarRetoHandler.handle(new ConsultarRetoQuery(retoId, request.pregunta()));
        return ResponseEntity.ok(new ConsultaRetoResponse(respuesta));
    }

    @GetMapping("/recomendaciones")
    public ResponseEntity<RecomendacionesResponse> obtenerRecomendaciones(
            @RequestHeader("X-User-Id") String talentoId) {
        var recomendaciones = recomendacionesHandler.handle(new ObtenerRecomendacionesQuery(talentoId));
        return ResponseEntity.ok(new RecomendacionesResponse(recomendaciones));
    }

    @PostMapping("/postulaciones/{postulacionId}/feedback")
    public ResponseEntity<FeedbackSolucionResponse> obtenerFeedback(
            @PathVariable String postulacionId,
            @RequestHeader("X-User-Id") String talentoId,
            @Valid @RequestBody FeedbackSolucionRequest request) {
        String feedback = feedbackHandler.handle(
                new ObtenerFeedbackSolucionQuery(postulacionId, talentoId, request.enfoqueSolucion()));
        return ResponseEntity.ok(new FeedbackSolucionResponse(feedback));
    }
}
