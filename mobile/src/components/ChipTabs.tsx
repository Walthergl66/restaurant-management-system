/**
 * Estación Burger — Pestañas en forma de chip
 * Selector horizontal genérico (zonas, áreas, estados, categorías). No conoce el dominio.
 */

import React from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity } from 'react-native';
import { colors } from '@/theme/colors';

export interface OpcionChip<T extends string | number> {
  id: T;
  etiqueta: string;
  /** Número a mostrar junto a la etiqueta (p. ej. pendientes). */
  contador?: number;
}

interface ChipTabsProps<T extends string | number> {
  opciones: OpcionChip<T>[];
  /** `null` = la opción "Todas" (solo si `conTodas`). */
  seleccionado: T | null;
  onSelect: (id: T | null) => void;
  /** Añade al inicio una opción "Todas" que selecciona `null`. */
  conTodas?: boolean;
  etiquetaTodas?: string;
}

export function ChipTabs<T extends string | number>({
  opciones,
  seleccionado,
  onSelect,
  conTodas = false,
  etiquetaTodas = 'Todas',
}: ChipTabsProps<T>) {
  return (
    <View style={styles.wrapper}>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.container}>
        {conTodas && (
          <Chip etiqueta={etiquetaTodas} activo={seleccionado === null} onPress={() => onSelect(null)} />
        )}
        {opciones.map(opcion => (
          <Chip
            key={String(opcion.id)}
            etiqueta={opcion.etiqueta}
            contador={opcion.contador}
            activo={seleccionado === opcion.id}
            onPress={() => onSelect(opcion.id)}
          />
        ))}
      </ScrollView>
    </View>
  );
}

interface ChipProps {
  etiqueta: string;
  contador?: number;
  activo: boolean;
  onPress: () => void;
}

function Chip({ etiqueta, contador, activo, onPress }: ChipProps) {
  return (
    <TouchableOpacity
      onPress={onPress}
      style={[styles.chip, activo && styles.chipActive]}
      accessibilityRole="tab"
      accessibilityState={{ selected: activo }}
    >
      <Text style={[styles.chipText, activo && styles.chipTextActive]}>{etiqueta}</Text>
      {contador !== undefined && (
        <View style={[styles.badge, activo && styles.badgeActive]}>
          <Text style={[styles.badgeText, activo && styles.badgeTextActive]}>{contador}</Text>
        </View>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    paddingVertical: 8,
  },
  container: {
    paddingHorizontal: 16,
    paddingRight: 32,
    gap: 10,
  },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 25,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    gap: 6,
  },
  chipActive: {
    backgroundColor: colors.neonOrange,
    borderColor: colors.neonOrange,
    shadowColor: colors.neonOrange,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.4,
    shadowRadius: 8,
    elevation: 4,
  },
  chipText: {
    color: colors.textSecondary,
    fontSize: 13,
    fontWeight: '600',
  },
  chipTextActive: {
    color: colors.background,
    fontWeight: '700',
  },
  badge: {
    minWidth: 20,
    height: 20,
    borderRadius: 10,
    paddingHorizontal: 6,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surfaceLight,
  },
  badgeActive: {
    backgroundColor: colors.background,
  },
  badgeText: {
    color: colors.textOnSurface,
    fontSize: 11,
    fontWeight: '700',
  },
  badgeTextActive: {
    color: colors.neonOrange,
  },
});
