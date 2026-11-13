/**
 * Estación Burger — Decodificador de polyline (delivery)
 *
 * El backend obtiene las rutas de ORS Directions /v2/directions/driving-car
 * (endpoint /json), por lo que `RutaCalculada.geometria` es un POLYLINE
 * CODIFICADO con el Encoded Polyline Algorithm de Google, precisión 5 (1e5).
 *
 * Esta función lo decodifica a un array de { latitude, longitude } para dibujar
 * la <Polyline> de react-native-maps. Son ~20 líneas, así que NO añadimos una
 * dependencia nueva (@mapbox/polyline) solo para esto.
 *
 * Convención de la ruta individual: el PRIMER punto decodificado es el origen
 * (restaurante) y el ÚLTIMO es el destino (cliente). Así el mapa puede pintar
 * ambos marcadores sin pedir coordenadas extra al backend.
 */

export interface LatLng {
  latitude: number;
  longitude: number;
}

/**
 * Decodifica un polyline codificado (precisión 5) a coordenadas geográficas.
 * Devuelve [] si la cadena está vacía o es inválida.
 */
export function decodePolyline(encoded: string): LatLng[] {
  if (!encoded) return [];

  const puntos: LatLng[] = [];
  let index = 0;
  let lat = 0;
  let lng = 0;
  const factor = 1e5;

  while (index < encoded.length) {
    let shift = 0;
    let result = 0;
    let byte: number;

    do {
      byte = encoded.charCodeAt(index++) - 63;
      result |= (byte & 0x1f) << shift;
      shift += 5;
    } while (byte >= 0x20);
    const deltaLat = result & 1 ? ~(result >> 1) : result >> 1;
    lat += deltaLat;

    shift = 0;
    result = 0;
    do {
      byte = encoded.charCodeAt(index++) - 63;
      result |= (byte & 0x1f) << shift;
      shift += 5;
    } while (byte >= 0x20);
    const deltaLng = result & 1 ? ~(result >> 1) : result >> 1;
    lng += deltaLng;

    puntos.push({ latitude: lat / factor, longitude: lng / factor });
  }

  return puntos;
}
