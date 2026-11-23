import React from 'react';
import { Text, StyleSheet, TouchableOpacity, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';
import { disenarMesa } from '../geometriaMesa';
import type { MesaMesero } from '../types';

interface MesaGridProps {
  mesas: MesaMesero[];
  onSeleccionar: (mesa: MesaMesero) => void;
}

/**
 * Plano de mesas: cada mesa se dibuja con sus sillas según su capacidad. La ocupación la decide
 * el backend (`mesa.estado`); aquí solo se marca con un check.
 */
export function MesaGrid({ mesas, onSeleccionar }: MesaGridProps) {
  return (
    <View style={styles.grid}>
      {mesas.map(mesa => {
        const ocupada = mesa.estado === 'OCUPADA';
        const diseno = disenarMesa(mesa.capacidad);
        const radio = diseno.forma === 'redonda' ? diseno.cuerpo.w / 2 : 14;
        return (
          <TouchableOpacity
            key={mesa.id}
            onPress={() => onSeleccionar(mesa)}
            activeOpacity={0.8}
            style={{ width: diseno.ancho, height: diseno.alto }}
            accessibilityRole="button"
            accessibilityLabel={`Mesa ${mesa.numero}, ${mesa.capacidad} personas${ocupada ? ', ocupada' : ''}`}
          >
            {diseno.sillas.map((silla, i) => (
              <View
                key={i}
                style={[styles.silla, { left: silla.x, top: silla.y, width: silla.w, height: silla.h }]}
              />
            ))}
            <View
              style={[
                styles.cuerpo,
                ocupada && styles.cuerpoOcupado,
                {
                  left: diseno.cuerpo.x,
                  top: diseno.cuerpo.y,
                  width: diseno.cuerpo.w,
                  height: diseno.cuerpo.h,
                  borderRadius: radio,
                },
              ]}
            >
              <Text style={styles.numero}>{mesa.numero}</Text>
            </View>
            {ocupada && (
              <View style={styles.check}>
                <Ionicons name="checkmark" size={13} color={colors.background} />
              </View>
            )}
          </TouchableOpacity>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 20,
    paddingHorizontal: 16,
    paddingVertical: 20,
  },
  silla: {
    position: 'absolute',
    borderRadius: 4,
    backgroundColor: colors.textMuted,
  },
  cuerpo: {
    position: 'absolute',
    backgroundColor: colors.surface,
    borderWidth: 2,
    borderColor: colors.surfaceBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cuerpoOcupado: {
    borderColor: colors.success,
    backgroundColor: colors.surfaceLight,
  },
  numero: {
    color: colors.textOnSurface,
    fontSize: 20,
    fontWeight: '800',
  },
  check: {
    position: 'absolute',
    right: 0,
    bottom: 0,
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: colors.success,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
