package com.restaurante.archivos.application;

import com.restaurante.shared.domain.exception.AlmacenamientoNoDisponible;
import com.restaurante.archivos.domain.Archivo;
import com.restaurante.archivos.infrastructure.ArchivoProperties;
import org.springframework.stereotype.Service;
import java.io.*;
import java.net.*;

@Service
public class LecturaArchivoService {
    private final ArchivoProperties config;
    public LecturaArchivoService(ArchivoProperties config) { this.config=config; }
    public record Contenido(HttpURLConnection conexion, InputStream cuerpo) implements AutoCloseable {
        public int estado() throws IOException { return conexion.getResponseCode(); }
        public String cabecera(String nombre) { return conexion.getHeaderField(nombre); }
        @Override public void close() throws IOException { try { cuerpo.close(); } finally { conexion.disconnect(); } }
    }
    public Contenido abrir(Archivo archivo, String range, boolean head) {
        HttpURLConnection conexion=null;
        try {
            config.comprobar();
            URI uri=URI.create(config.getLecturaUrl().replaceAll("/+$", "")+"/"+archivo.getClave());
            if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) throw new IOException("URL inválida");
            conexion=(HttpURLConnection)uri.toURL().openConnection();
            conexion.setConnectTimeout(Math.toIntExact(config.getTimeout().toMillis()));
            conexion.setReadTimeout(Math.toIntExact(config.getTimeout().toMillis()));
            conexion.setInstanceFollowRedirects(false);
            conexion.setRequestMethod(head ? "HEAD" : "GET");
            conexion.setRequestProperty("Accept-Encoding", "identity");
            if (range != null) conexion.setRequestProperty("Range", range);
            int status=conexion.getResponseCode();
            if (status != 200 && status != 206 && status != 416) throw new IOException("Respuesta del almacenamiento: "+status);
            return new Contenido(conexion, status == 416 || head ? InputStream.nullInputStream() : conexion.getInputStream());
        } catch (IOException | IllegalArgumentException ex) {
            if (conexion != null) conexion.disconnect();
            throw new AlmacenamientoNoDisponible(ex);
        }
    }
}
