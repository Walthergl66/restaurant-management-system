package com.restaurante.archivos;

import java.util.UUID;

/** Contrato del catálogo. Las vinculaciones participan en su misma transacción. */
public interface Archivos {
    void vincular(UUID id, TipoArchivo tipo, Long productoId);
    void retirar(UUID id, Long productoId);
    static String url(UUID id) { return id == null ? null : "/api/v1/archivos/" + id + "/contenido"; }
}
