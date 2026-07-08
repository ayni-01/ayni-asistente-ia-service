package com.somosayni.asistente.infrastructure.rest;

import com.somosayni.asistente.application.query.ConsultarRetoQuery;
import com.somosayni.asistente.application.query.ConsultarRetoQueryHandler;
import com.somosayni.asistente.application.query.ObtenerRecomendacionesQuery;
import com.somosayni.asistente.application.query.ObtenerRecomendacionesQueryHandler;
import com.somosayni.asistente.infrastructure.rest.dto.ConsultaRetoRequest;
import com.somosayni.asistente.infrastructure.rest.dto.ConsultaRetoResponse;
import com.somosayni.asistente.infrastructure.rest.dto.RecomendacionesResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/asistente")
public class AsistenteController {

    private final ConsultarRetoQueryHandler consultarRetoHandler;
    private final ObtenerRecomendacionesQueryHandler recomendacionesHandler;

    public AsistenteController(
            ConsultarRetoQueryHandler consultarRetoHandler,
            ObtenerRecomendacionesQueryHandler recomendacionesHandler) {
        this.consultarRetoHandler = consultarRetoHandler;
        this.recomendacionesHandler = recomendacionesHandler;
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
}
