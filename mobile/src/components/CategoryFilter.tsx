/**
 * Estación Burger — Filtro de Categorías
 */

import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../theme/colors';
import type { CategoriaMenu } from '../features/menu/types';

interface CategoryFilterProps {
  categorias: CategoriaMenu[];
  selectedId: number | null;
  onSelect: (id: number | null) => void;
}

export function CategoryFilter({
  categorias,
  selectedId,
  onSelect,
}: CategoryFilterProps) {
  return (
    <View style={styles.wrapper}>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.container}
      >
        <TouchableOpacity
          onPress={() => onSelect(null)}
          style={[
            styles.chip,
            selectedId === null && styles.chipActive,
          ]}
        >
          <Ionicons
            name="grid"
            size={16}
            color={selectedId === null ? colors.background : colors.textSecondary}
          />
          <Text
            style={[
              styles.chipText,
              selectedId === null && styles.chipTextActive,
            ]}
          >
            Todos
          </Text>
        </TouchableOpacity>

        {categorias.map(cat => (
          <TouchableOpacity
            key={cat.id}
            onPress={() => onSelect(cat.id)}
            style={[
              styles.chip,
              selectedId === cat.id && styles.chipActive,
            ]}
          >
            <Text
              style={[
                styles.chipText,
                selectedId === cat.id && styles.chipTextActive,
              ]}
            >
              {cat.nombre}
            </Text>
          </TouchableOpacity>
        ))}
      </ScrollView>
    </View>
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
});
