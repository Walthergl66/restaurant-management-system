/**
 * Estación Burger — Medidor de fuerza de contraseña
 *
 * Muestra el rango de seguridad (débil/media/fuerte) con una barra y la lista
 * de criterios que el backend exige: letra, mayúscula, número y símbolo.
 */

import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../../theme/colors';
import {
  evaluarPassword,
  etiquetaNivel,
  type NivelPassword,
} from './validation';

interface PasswordStrengthProps {
  password: string;
  compact?: boolean;
}

const COLOR_NIVEL: Record<NivelPassword, string> = {
  debil: colors.error,
  media: colors.warning,
  fuerte: colors.success,
};

const CRITERIOS: { clave: keyof ReturnType<typeof evaluarPassword>['criterios']; texto: string; breve: string }[] = [
  { clave: 'longitud', texto: 'Al menos 8 caracteres', breve: '8+ caracteres' },
  { clave: 'minuscula', texto: 'Una letra minúscula', breve: 'Minúscula' },
  { clave: 'mayuscula', texto: 'Una letra mayúscula', breve: 'Mayúscula' },
  { clave: 'numero', texto: 'Un número', breve: 'Número' },
  { clave: 'simbolo', texto: 'Un símbolo (!@#…)', breve: 'Símbolo (!@#…)' },
];

export function PasswordStrength({ password, compact = false }: PasswordStrengthProps) {
  const evaluacion = evaluarPassword(password);
  const color = COLOR_NIVEL[evaluacion.nivel];

  return (
    <View style={[styles.container, compact && styles.containerCompact]}>
      <View style={styles.barRow}>
        <View style={styles.barTrack}>
          <View
            style={[
              styles.barFill,
              { width: `${evaluacion.porcentaje * 100}%`, backgroundColor: color },
            ]}
          />
        </View>
        <Text style={[styles.nivel, { color }]}>
          {etiquetaNivel(evaluacion.nivel)}
        </Text>
      </View>

      <View style={[styles.criterios, compact && styles.criteriosCompact]}>
        {CRITERIOS.map(({ clave, texto, breve }) => {
          const ok = evaluacion.criterios[clave];
          return (
            <View key={clave} style={styles.criterio}>
              <Ionicons
                name={ok ? 'checkmark-circle' : 'ellipse-outline'}
                size={14}
                color={ok ? colors.success : colors.textMuted}
              />
              <Text style={[styles.criterioText, ok && styles.criterioOk]}>
                {compact ? breve : texto}
              </Text>
            </View>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginTop: -8,
    marginBottom: 16,
  },
  containerCompact: {
    marginTop: 0,
    marginBottom: 8,
  },
  criteriosCompact: {
    gap: 4,
    marginTop: 4,
  },
  barRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  barTrack: {
    flex: 1,
    height: 6,
    borderRadius: 3,
    backgroundColor: colors.surfaceLight,
    overflow: 'hidden',
  },
  barFill: {
    height: 6,
    borderRadius: 3,
  },
  nivel: {
    fontSize: 12,
    fontWeight: '700',
    minWidth: 48,
    textAlign: 'right',
  },
  criterios: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginTop: 10,
  },
  criterio: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  criterioText: {
    color: colors.textMuted,
    fontSize: 11,
  },
  criterioOk: {
    color: colors.textSecondary,
  },
});
