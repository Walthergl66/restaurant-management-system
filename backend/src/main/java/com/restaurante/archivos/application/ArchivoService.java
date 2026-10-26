package com.restaurante.archivos.application;

import com.restaurante.archivos.*;
import com.restaurante.shared.domain.exception.AlmacenamientoNoDisponible;
import com.restaurante.archivos.domain.Archivo;
import com.restaurante.archivos.infrastructure.ArchivoRepository;
import com.restaurante.shared.domain.exception.NotFoundException;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class ArchivoService implements Archivos {
    private static final Logger log=LoggerFactory.getLogger(ArchivoService.class);
    private final ArchivoRepository repository;
    private final AlmacenArchivos almacen;
    private final ValidadorArchivo validador;
    public ArchivoService(ArchivoRepository repository, AlmacenArchivos almacen, ValidadorArchivo validador) {
        this.repository=repository; this.almacen=almacen; this.validador=validador;
    }
    @Transactional
    public ArchivoResumen subir(MultipartFile archivo) {
        if (archivo.isEmpty() || archivo.getSize() > ValidadorArchivo.MAX_VIDEO) throw new BusinessRuleException("El archivo está vacío o supera 100 MB");
        Path temporal=null;
        try {
            temporal=Files.createTempFile("restaurante-upload-", ".bin");
            archivo.transferTo(temporal);
            var validado=validador.validar(temporal);
            UUID id=UUID.randomUUID();
            String clave=(validado.tipo() == TipoArchivo.IMAGEN ? "imagenes/" : "videos/")+id+"."+validado.extension();
            // Una reversión de PostgreSQL compensa la publicación en SFTP.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        try { almacen.eliminar(clave); }
                        catch (RuntimeException ex) { log.error("No se pudo compensar el archivo {}. Requiere conciliación", clave, ex); }
                    }
                }
            });
            almacen.guardar(temporal, clave);
            String nombre=archivo.getOriginalFilename() == null ? "archivo" : archivo.getOriginalFilename();
            nombre=nombre.replaceAll("[\\p{Cntrl}/\\\\]", "_");
            if (nombre.length() > 255) nombre=nombre.substring(nombre.length()-255);
            Archivo guardado=repository.saveAndFlush(new Archivo(id, nombre, clave, validado.tipo(), validado.mime(), validado.tamano(), usuario()));
            return resumen(guardado);
        } catch (IOException ex) { throw new AlmacenamientoNoDisponible(ex); }
        finally { if (temporal != null) try { Files.deleteIfExists(temporal); } catch (IOException ex) { log.warn("No se pudo borrar temporal de subida", ex); } }
    }
    @Override @Transactional
    public void vincular(UUID id, TipoArchivo tipo, Long productoId) {
        repository.bloquear(id).orElseThrow(() -> new NotFoundException("Archivo no encontrado")).vincular(tipo, productoId, usuario());
    }
    @Override @Transactional
    public void retirar(UUID id, Long productoId) {
        if (id != null) repository.bloquear(id).ifPresent(a -> a.retirar(productoId));
    }
    @Transactional
    public void descartar(UUID id) {
        Archivo a=repository.bloquear(id).orElseThrow(() -> new NotFoundException("Archivo no encontrado"));
        if (!a.getPropietario().equals(usuario()) || !"PENDIENTE".equals(a.getEstado())) throw new NotFoundException("Archivo temporal no encontrado");
        almacen.eliminar(a.getClave());
        repository.delete(a);
    }
    @Transactional(readOnly=true)
    public Archivo paraLectura(UUID id) {
        Archivo a=repository.findById(id).orElseThrow(() -> new NotFoundException("Archivo no encontrado"));
        if (!"VINCULADO".equals(a.getEstado()) && !("PENDIENTE".equals(a.getEstado()) && a.getPropietario().equals(usuario()))) throw new NotFoundException("Archivo no encontrado");
        return a;
    }
    private ArchivoResumen resumen(Archivo a) { return new ArchivoResumen(a.getId(), a.getNombreOriginal(), a.getTipo(), a.getContenidoTipo(), a.getTamano(), Archivos.url(a.getId())); }
    private String usuario() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() ? auth.getName() : "anonymousUser";
    }
}
