package com.restaurante.archivos.web;

import com.restaurante.archivos.*;
import com.restaurante.archivos.application.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/archivos")
public class ArchivoController {
    private final ArchivoService archivos;
    private final LecturaArchivoService lectura;
    public ArchivoController(ArchivoService archivos, LecturaArchivoService lectura) { this.archivos=archivos; this.lectura=lectura; }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('catalogo:gestionar')")
    @ResponseStatus(HttpStatus.CREATED)
    public ArchivoResumen subir(@RequestParam("archivo") MultipartFile archivo) { return archivos.subir(archivo); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('catalogo:gestionar')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void descartar(@PathVariable UUID id) { archivos.descartar(id); }

    @RequestMapping(value="/{id}/contenido", method={RequestMethod.GET, RequestMethod.HEAD})
    public void contenido(@PathVariable UUID id, @RequestHeader(value="Range", required=false) String range,
                          jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) throws IOException {
        var archivo=archivos.paraLectura(id);
        if (range != null && (!range.matches("bytes=\\d*-\\d*") || "bytes=-".equals(range))) {
            response.setStatus(416); response.setHeader("Content-Range", "bytes */"+archivo.getTamano()); return;
        }
        boolean head="HEAD".equals(request.getMethod());
        try (var upstream=lectura.abrir(archivo, range, head)) {
            response.setStatus(upstream.estado());
            response.setContentType(archivo.getContenidoTipo());
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Accept-Ranges", "bytes");
            for (String header : new String[]{"Content-Length", "Content-Range", "Last-Modified", "ETag"})
                if (upstream.cabecera(header) != null) response.setHeader(header, upstream.cabecera(header));
            if (!head && upstream.estado() != 416) upstream.cuerpo().transferTo(response.getOutputStream());
        }
    }
}
