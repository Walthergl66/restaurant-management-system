package com.restaurante.archivos;

import java.util.UUID;

public record ArchivoResumen(UUID id, String nombre, TipoArchivo tipo, String contenidoTipo,
                             long tamano, String url) {}
