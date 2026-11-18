package com.restaurante.catalogo.infrastructure;

import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Guarda archivos (fotos y videos de productos) en el servidor de archivos
 * propio (VM Ubuntu con nginx + WebDAV), sin usar servicios en la nube.
 * <p>
 * El envío al servidor de archivos se hace con {@code PUT} directo del cuerpo
 * del archivo (WebDAV), autenticado con {@code Basic Auth}. La URL pública
 * devuelta es la que se guarda en {@code producto.imagenUrl} para que el
 * navegador la descargue directamente de la VM, sin pasar por el backend.
 */
@Component
public class AlmacenadorArchivos {

    private static final long MAX_TAMANIO_BYTES = 50L * 1024 * 1024; // 50 MB

    private final RestClient restClient;
    private final String uploadUrl;
    private final String publicUrl;
    private final String authHeader;

    public AlmacenadorArchivos(RestClient.Builder builder,
                               @Value("${app.media.upload-url}") String uploadUrl,
                               @Value("${app.media.public-url}") String publicUrl,
                               @Value("${app.media.usuario}") String usuario,
                               @Value("${app.media.clave}") String clave) {
        this.restClient = builder.build();
        this.uploadUrl = uploadUrl.endsWith("/") ? uploadUrl.substring(0, uploadUrl.length() - 1) : uploadUrl;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        String creds = usuario + ":" + clave;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(creds.getBytes());
    }

    /**
     * Sube el archivo al servidor de archivos y devuelve la URL pública.
     *
     * @param archivo archivo a subir (ya validado de tipo y tamaño por el caller)
     * @param carpeta carpeta destino dentro de /srv/media/ (ej. "productos")
     * @return URL pública del archivo (ej. http://192.168.1.50/media/productos/abc.jpg)
     */
    public String guardar(MultipartFile archivo, String carpeta) {
        validar(archivo);

        String nombreArchivo = UUID.randomUUID() + "-" + nombreSeguro(archivo.getOriginalFilename());
        String nombre = carpeta + "/" + nombreArchivo;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        headers.setContentType(MediaType.parseMediaType(archivo.getContentType()));

        try {
            URI destino = construirUriArchivo(uploadUrl, carpeta, nombreArchivo);
            String respuesta = restClient.put()
                    .uri(destino)
                    .headers(h -> h.addAll(headers))
                    .body(new ByteArrayResource(archivo.getBytes()) {
                        @Override
                        public String getFilename() {
                            return archivo.getOriginalFilename();
                        }
                    })
                    .retrieve()
                    .onStatus(status -> status.isError() || status.is4xxClientError() || status.is5xxServerError(),
                            (req, res) -> {
                                throw new BusinessRuleException(
                                        "El servidor de archivos respondió " + res.getStatusCode() + " al subir '" + nombre + "'");
                            })
                    .body(String.class);

            return construirUrlArchivo(publicUrl, carpeta, nombreArchivo);
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessRuleException("No se pudo subir el archivo '" + archivo.getOriginalFilename() + "': " + e.getMessage());
        }
    }

    private URI construirUriArchivo(String baseUrl, String carpeta, String nombreArchivo) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment(carpeta, nombreArchivo)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
    }

    private String construirUrlArchivo(String baseUrl, String carpeta, String nombreArchivo) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment(carpeta, nombreArchivo)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    private String nombreSeguro(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            return "archivo";
        }
        String nombreBase = nombreOriginal.replace('\\', '/');
        nombreBase = nombreBase.substring(nombreBase.lastIndexOf('/') + 1).trim();
        if (nombreBase.isBlank()) {
            return "archivo";
        }
        return nombreBase.replaceAll("[\\p{Cntrl}]", "_");
    }

    private void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessRuleException("No hay archivo para subir");
        }
        String contentType = archivo.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            throw new BusinessRuleException("Solo se permiten imágenes o videos");
        }
        if (archivo.getSize() > MAX_TAMANIO_BYTES) {
            throw new BusinessRuleException("El archivo supera los 50 MB");
        }
    }
}
