import React from 'react';
import { ChipTabs } from '@/components/ChipTabs';
import type { AreaCocina } from '../types';

interface FiltroAreasProps {
  areas: AreaCocina[];
  /** `null` = todas las áreas. */
  seleccionada: number | null;
  onSelect: (areaId: number | null) => void;
}

/** Chips para ver las comandas de una sola área de preparación (Cocina, Barra…) o de todas. */
export function FiltroAreas({ areas, seleccionada, onSelect }: FiltroAreasProps) {
  return (
    <ChipTabs<number>
      opciones={areas.map(area => ({ id: area.id, etiqueta: area.nombre }))}
      seleccionado={seleccionada}
      onSelect={onSelect}
      conTodas
    />
  );
}
