export interface MesaMesero {
  id: number;
  numero: number;
  capacidad: number;
  ubicacion: string | null;
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
