package com.restaurante.delivery.application;

import com.restaurante.delivery.domain.ResultadoRuta;

/**
 * Ruta por carretera calculada por ORS Directions para un conjunto de paradas
 * en un orden dado: geometría codificada, distancia (m) y duración (s).
 *
 * <p>NO afirma optimalidad matemática: para una ruta individual solo hay un
 * destino; para multientrega, el orden lo propone ORS Optimization (VROOM) y
 * esta geometría dibuja ese orden por carretera, sin garantizar el óptimo
 * global.
 */
public record RutaCalculada(
        String geometria,
        int distanciaM,
        int duracionS) {

    static RutaCalculada de(ResultadoRuta r) {
        return new RutaCalculada(r.geometria(), r.distanciaM(), r.duracionS());
    }
}
