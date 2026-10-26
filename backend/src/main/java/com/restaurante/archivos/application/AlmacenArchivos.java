package com.restaurante.archivos.application;

import java.nio.file.Path;

public interface AlmacenArchivos {
    void guardar(Path archivo, String clave);
    void eliminar(String clave);
}
