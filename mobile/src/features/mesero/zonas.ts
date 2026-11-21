import type { CuentaAbierta, MesaMesero } from './types';

export type ZonaMesas = { zona: string; mesas: MesaMesero[] };

const ZONA_POR_DEFECTO = 'Salón';

/** Clave de comparación: sin tildes, sin espacios laterales y sin distinguir mayúsculas. */
function claveZona(ubicacion: string | null | undefined): string {
  return (ubicacion ?? '').trim().normalize('NFD').replace(/[̀-ͯ]/g, '').toLocaleLowerCase('es');
}

/** Agrupa las mesas por zona (orden de primera aparición) y ordena cada zona por número de mesa. */
export function agruparMesasPorZona(mesas: MesaMesero[]): ZonaMesas[] {
  const zonas = new Map<string, ZonaMesas>();
  for (const mesa of mesas) {
    const nombre = (mesa.ubicacion ?? '').trim() || ZONA_POR_DEFECTO;
    const clave = claveZona(nombre);
    const zona = zonas.get(clave) ?? { zona: nombre, mesas: [] };
    zona.mesas.push(mesa);
    zonas.set(clave, zona);
  }
  return [...zonas.values()].map(z => ({ ...z, mesas: [...z.mesas].sort((a, b) => a.numero - b.numero) }));
}

/** Ids de las mesas que tienen una cuenta abierta. */
export function idsMesasOcupadas(cuentas: CuentaAbierta[]): Set<number> {
  return new Set(cuentas.map(cuenta => cuenta.mesaId));
}
