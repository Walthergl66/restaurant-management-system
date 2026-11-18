package com.restaurante.archivos.web;

import com.restaurante.archivos.application.ArchivoContenidoService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** Mantiene accesibles las URLs de archivos ya guardadas en productos. */
@RestController
@RequestMapping("/api/v1/archivos")
public class ArchivoContenidoController {

    private final ArchivoContenidoService contenidoService;

    public ArchivoContenidoController(ArchivoContenidoService contenidoService) {
        this.contenidoService = contenidoService;
    }

    @RequestMapping(value = "/{id}/contenido", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<Void> contenido(@PathVariable UUID id) {
        URI destino = contenidoService.urlPublica(id);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(destino)
                .cacheControl(CacheControl.noCache())
                .build();
    }
}
