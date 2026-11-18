package com.restaurante.archivos.application;

import com.restaurante.archivos.infrastructure.ArchivoReferenciaRepository;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.UUID;
import java.util.regex.Pattern;

/** Resuelve las URLs públicas de los archivos multimedia históricos. */
@Service
public class ArchivoContenidoService {

    private static final Pattern CLAVE_PERMITIDA = Pattern.compile(
            "(imagenes|videos)/[a-f0-9-]{36}\\.(jpg|png|webp|mp4|webm)");

    private final ArchivoReferenciaRepository repository;
    private final String lecturaUrl;

    public ArchivoContenidoService(ArchivoReferenciaRepository repository,
                                   @Value("${app.archivos.lectura-url}") String lecturaUrl) {
        this.repository = repository;
        this.lecturaUrl = lecturaUrl.replaceAll("/+$", "");
    }

    public URI urlPublica(UUID id) {
        String clave = repository.buscarClaveVinculada(id)
                .filter(CLAVE_PERMITIDA.asPredicate())
                .orElseThrow(() -> new NotFoundException("Archivo no encontrado"));
        return URI.create(lecturaUrl + "/" + clave);
    }
}
