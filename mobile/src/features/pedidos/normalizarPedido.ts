/** Modelo independiente del transporte para presentar un pedido en la app. */
export interface PedidoNormalizado {
  codigo: string;
  estado: string;
  pago: string;
  entrega: string;
  subtotal: number;
  costoEntrega: number;
  total: number;
  creadoEn: string;
  actualizadoEn: string;
  lineas: Array<{
    productoId: number;
    nombre: string;
    precioUnitario: number;
    cantidad: number;
    subtotal: number;
    extras: Array<{ id: number; nombre: string; precio: number }>;
  }>;
}

type Registro = Record<string, unknown>;

/** Backend SPI: nombres precio/extraId y fechas ISO con zona horaria. */
export function desdeSpi(origen: Registro): PedidoNormalizado {
  return convertir(origen, {
    pago: 'metodoPago', entrega: 'metodoEntrega', creado: 'creadoAt', actualizado: 'actualizadoAt',
    precio: 'precio', extrasId: 'extraId',
  });
}

/** Respuesta web REST: precioUnitario, extras.id y confirmadoAt opcional. */
export function desdeRespuestaWeb(origen: Registro): PedidoNormalizado {
  return convertir(origen, {
    pago: 'metodoPago', entrega: 'metodoEntrega', creado: 'creadoAt', actualizado: 'actualizadoAt',
    precio: 'precioUnitario', extrasId: 'id',
  });
}

function convertir(origen: Registro, campos: {
  pago: string; entrega: string; creado: string; actualizado: string; precio: string; extrasId: string;
}): PedidoNormalizado {
  if (!origen || typeof origen !== 'object') {
    throw new Error('El pedido debe ser un objeto');
  }
  const codigo = texto(origen.codigo, 'codigo');
  const estado = texto(origen.estado, 'estado').trim().toUpperCase();
  const creadoEn = fecha(origen[campos.creado], campos.creado);
  const actualizadoEn = fecha(origen[campos.actualizado], campos.actualizado);
  const lineasOrigen = origen.lineas;
  if (!Array.isArray(lineasOrigen)) throw new Error('lineas debe ser una lista');
  const subtotal = origen.subtotal === undefined
    ? sumarLineas(lineasOrigen)
    : dinero(origen.subtotal, 'subtotal');
  const costoEntrega = dinero(origen.costoEntrega, 'costoEntrega');
  const total = origen.total === undefined
    ? dinero(subtotal + costoEntrega, 'total') : dinero(origen.total, 'total');
  if (Math.abs(subtotal + costoEntrega - total) > 0.011) {
    throw new Error('total no coincide con subtotal + costoEntrega');
  }
  const lineas = lineasOrigen.map((raw, i) => {
    const l = objeto(raw, `lineas[${i}]`);
    const productoId = enteroPositivo(l.productoId, `lineas[${i}].productoId`);
    const cantidad = enteroPositivo(l.cantidad, `lineas[${i}].cantidad`);
    const precioUnitario = dinero(l[campos.precio], `lineas[${i}].${campos.precio}`);
    const lineaSubtotal = dinero(l.subtotal, `lineas[${i}].subtotal`);
    if (!Array.isArray(l.extras)) throw new Error(`lineas[${i}].extras debe ser una lista`);
    return {
      productoId,
      nombre: texto(l.nombre, `lineas[${i}].nombre`).trim(),
      precioUnitario,
      cantidad,
      subtotal: lineaSubtotal,
      extras: l.extras.map((rawExtra, j) => {
        const e = objeto(rawExtra, `lineas[${i}].extras[${j}]`);
        return {
        id: enteroPositivo(e[campos.extrasId], `lineas[${i}].extras[${j}].${campos.extrasId}`),
        nombre: texto(e.nombre, `lineas[${i}].extras[${j}].nombre`).trim(),
        precio: dinero(e.precio, `lineas[${i}].extras[${j}].precio`),
      };}),
    };
  });
  return {
    codigo, estado, pago: texto(origen[campos.pago], campos.pago).trim().toUpperCase(),
    entrega: texto(origen[campos.entrega], campos.entrega).trim().toUpperCase(),
    subtotal, costoEntrega, total, creadoEn, actualizadoEn,
    lineas,
  };
}

function objeto(value: unknown, campo: string): Registro {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error(`${campo} debe ser un objeto`);
  return value as Registro;
}

function sumarLineas(lineas: unknown[]): number {
  return lineas.reduce<number>((acc, raw, i) => {
    const linea = objeto(raw, `lineas[${i}]`);
    return acc + dinero(linea.subtotal, `lineas[${i}].subtotal`);
  }, 0);
}

function texto(v: unknown, campo: string): string {
  if (typeof v !== 'string' || !v.trim()) throw new Error(`${campo} debe ser texto no vacío`);
  return v;
}
function dinero(v: unknown, campo: string): number {
  const n = typeof v === 'number' ? v : typeof v === 'string' && v.trim() ? Number(v) : NaN;
  if (!Number.isFinite(n) || n < 0) throw new Error(`${campo} debe ser un importe no negativo`);
  return Math.round((n + Number.EPSILON) * 100) / 100;
}
function enteroPositivo(v: unknown, campo: string, permiteCero = false): number {
  if (!Number.isSafeInteger(v) || (v as number) < (permiteCero ? 0 : 1)) {
    throw new Error(`${campo} debe ser entero ${permiteCero ? 'no negativo' : 'positivo'}`);
  }
  return v as number;
}
function fecha(v: unknown, campo: string): string {
  const s = texto(v, campo);
  if (!/(Z|[+-]\d{2}:?\d{2})$/i.test(s) || !Number.isFinite(Date.parse(s))) {
    throw new Error(`${campo} debe ser fecha ISO-8601 con zona horaria`);
  }
  return new Date(s).toISOString();
}
