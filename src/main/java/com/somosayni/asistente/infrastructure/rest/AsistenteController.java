package com.somosayni.asistente.infrastructure.rest;

import com.somosayni.asistente.application.query.ConsultarRetoQuery;
import com.somosayni.asistente.application.query.ConsultarRetoQueryHandler;
import com.somosayni.asistente.infrastructure.rest.dto.ConsultaRetoRequest;
import com.somosayni.asistente.infrastructure.rest.dto.ConsultaRetoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/asistente")
public class AsistenteController {

    private final ConsultarRetoQueryHandler consultarRetoHandler;

    public AsistenteController(ConsultarRetoQueryHandler consultarRetoHandler) {
        this.consultarRetoHandler = consultarRetoHandler;
    }

    @PostMapping("/retos/{retoId}/consulta")
    public ResponseEntity<ConsultaRetoResponse> consultarSobreReto(
            @PathVariable String retoId,
            @Valid @RequestBody ConsultaRetoRequest request) {
        String respuesta = consultarRetoHandler.handle(new ConsultarRetoQuery(retoId, request.pregunta()));
        return ResponseEntity.ok(new ConsultaRetoResponse(respuesta));
    }
}
