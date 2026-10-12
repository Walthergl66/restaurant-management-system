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
