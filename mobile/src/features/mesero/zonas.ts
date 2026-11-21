import type { MesaMesero } from './types';

export type ZonaMesas = { zona: string; mesas: MesaMesero[] };

const ZONA_POR_DEFECTO = 'Salón';

/** Clave de comparación: sin tildes, sin espacios laterales y sin distinguir mayúsculas. */
function claveZona(ubicacion: string | null | undefined): string {
  return (ubicacion ?? '').trim().normalize('NFD').replace(/[̀-ͯ]/g, '').toLocaleLowerCase('es');
}

function nombreDe(mesa: MesaMesero): string {
  return (mesa.ubicacion ?? '').trim() || ZONA_POR_DEFECTO;
}

/**
 * Agrupa las mesas por zona. Las zonas las define el administrador: se agrupan por `zonaId` y se
 * ordenan por `zonaOrden` (empate por nombre). Las mesas que llegan sin `zonaId` (un backend
 * anterior a las zonas) se agrupan por el nombre normalizado y van al final. Dentro de cada zona,
 * por número de mesa.
 */
export function agruparMesasPorZona(mesas: MesaMesero[]): ZonaMesas[] {
  const porId = new Map<number, { orden: number; grupo: ZonaMesas }>();
  const porNombre = new Map<string, ZonaMesas>();

  for (const mesa of mesas) {
    if (mesa.zonaId != null) {
      const entrada = porId.get(mesa.zonaId)
        ?? { orden: mesa.zonaOrden ?? Number.MAX_SAFE_INTEGER, grupo: { zona: nombreDe(mesa), mesas: [] } };
      entrada.grupo.mesas.push(mesa);
      porId.set(mesa.zonaId, entrada);
    } else {
      const nombre = nombreDe(mesa);
      const grupo = porNombre.get(claveZona(nombre)) ?? { zona: nombre, mesas: [] };
      grupo.mesas.push(mesa);
      porNombre.set(claveZona(nombre), grupo);
    }
  }

  const delAdministrador = [...porId.values()]
    .sort((a, b) => a.orden - b.orden || a.grupo.zona.localeCompare(b.grupo.zona, 'es'))
    .map(entrada => entrada.grupo);

  return [...delAdministrador, ...porNombre.values()]
    .map(grupo => ({ ...grupo, mesas: [...grupo.mesas].sort((a, b) => a.numero - b.numero) }));
}
