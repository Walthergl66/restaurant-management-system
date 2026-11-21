export interface MesaMesero {
  id: number;
  numero: number;
  capacidad: number;
  ubicacion: string | null;
  activo: boolean;
}

export interface LineaPedidoMesero {
  productoId: number;
  cantidad: number;
  extraIds: number[];
  ingredientesRemovidos?: string[];
  observaciones?: string;
}

/** Resumen de una cuenta abierta (GET /cuentas?estado=ABIERTA): una mesa con cuenta abierta está ocupada. */
export interface CuentaAbierta {
  id: number;
  mesaId: number;
  numeroMesa: number;
  total: number;
}
