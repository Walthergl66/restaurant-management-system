import React from 'react';
import { ChipTabs } from '@/components/ChipTabs';
import type { ZonaMesas } from '../zonas';

interface SelectorZonasProps {
  zonas: ZonaMesas[];
  /** Nombre de la zona activa. */
  seleccionada: string | null;
  onSelect: (zona: string) => void;
}

/** Pestañas de zona (Salón, Terraza…) con el número de mesas de cada una. */
export function SelectorZonas({ zonas, seleccionada, onSelect }: SelectorZonasProps) {
  return (
    <ChipTabs<string>
      opciones={zonas.map(z => ({ id: z.zona, etiqueta: z.zona, contador: z.mesas.length }))}
      seleccionado={seleccionada}
      onSelect={zona => zona !== null && onSelect(zona)}
    />
  );
}
