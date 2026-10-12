export interface ReporteVentas {
  total: number; numeroVentas: number
  porMetodo: { metodo: string; total: number }[]
  porDia: { fecha: string; total: number }[]
  porProducto: { productoId: string; nombre: string; cantidad: number; monto: number }[]
}

export interface ResumenFinanzas { ingresos: number; egresos: number; resultado: number }

export interface RangoFechas { desde: string; hasta: string }
