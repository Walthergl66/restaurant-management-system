export interface MesaMesero {
  id: number;
  numero: number;
  capacidad: number;
  /** Nombre de la zona (la define el administrador). */
  ubicacion: string | null;
  /** Id y posición de la zona; faltan solo si el backend es anterior a las zonas. */
  zonaId?: number;
  zonaOrden?: number;
  /** Lo decide el backend: OCUPADA mientras la mesa tenga una cuenta abierta. */
  estado: 'LIBRE' | 'OCUPADA' | 'RESERVADA' | 'INACTIVA';
  activo: boolean;
}

export interface LineaPedidoMesero {
  productoId: number;
  cantidad: number;
  extraIds: number[];
  ingredientesRemovidos?: string[];
  observaciones?: string;
}
