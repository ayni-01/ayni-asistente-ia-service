package com.somosayni.asistente.infrastructure.rest;

import com.somosayni.asistente.application.port.CatalogoRecursosPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/asistente/recursos")
public class RecursosController {

    private final CatalogoRecursosPort catalogoRecursosPort;

    public RecursosController(CatalogoRecursosPort catalogoRecursosPort) {
        this.catalogoRecursosPort = catalogoRecursosPort;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Integer>> ingestar() {
        return ResponseEntity.ok(Map.of("chunks", catalogoRecursosPort.ingestar()));
    }
}
