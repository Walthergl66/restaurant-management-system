import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

/** Badge de estado logístico: punto + etiqueta con el color de acento del estado. */
export default function EstadoBadge({ etiqueta, color }: { etiqueta: string; color: string }) {
  return (
    <View
      style={[styles.badge, { borderColor: color, backgroundColor: `${color}22` }]}
      accessibilityRole="text"
      accessibilityLabel={`Estado: ${etiqueta}`}
    >
      <View style={[styles.dot, { backgroundColor: color }]} />
      <Text style={[styles.text, { color }]}>{etiqueta}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    gap: 6,
  },
  dot: { width: 7, height: 7, borderRadius: 4 },
  text: { fontSize: 11, fontWeight: '800', letterSpacing: 0.3 },
});
