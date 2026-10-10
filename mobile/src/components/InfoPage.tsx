/**
 * Estación Burger — Página informativa reutilizable
 *
 * Encabezado con botón atrás + título y una lista de secciones
 * (título + párrafos). Se usa para Ayuda, Términos y Privacidad.
 */

import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../theme/colors';

export interface InfoSeccion {
  titulo: string;
  parrafos: string[];
}

interface InfoPageProps {
  title: string;
  subtitulo?: string;
  secciones: InfoSeccion[];
}

export function InfoPage({ title, subtitulo, secciones }: InfoPageProps) {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  return (
    <View style={styles.container}>
      <View style={[styles.header, { paddingTop: insets.top + 12 }]}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Ionicons name="arrow-back" size={24} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{title}</Text>
        <View style={styles.backBtn} />
      </View>

      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        {subtitulo && <Text style={styles.subtitulo}>{subtitulo}</Text>}
        {secciones.map((seccion, index) => (
          <View key={index} style={styles.seccion}>
            <Text style={styles.seccionTitulo}>{seccion.titulo}</Text>
            {seccion.parrafos.map((parrafo, i) => (
              <Text key={i} style={styles.parrafo}>
                {parrafo}
              </Text>
            ))}
          </View>
        ))}
        <Text style={styles.version}>Estación Burger v1.0.0</Text>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.surfaceBorder,
  },
  backBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerTitle: {
    color: colors.textPrimary,
    fontSize: 18,
    fontWeight: '800',
  },
  content: {
    padding: 20,
    paddingBottom: 40,
  },
  subtitulo: {
    color: colors.textSecondary,
    fontSize: 14,
    lineHeight: 20,
    marginBottom: 24,
  },
  seccion: {
    marginBottom: 24,
  },
  seccionTitulo: {
    color: colors.neonOrange,
    fontSize: 16,
    fontWeight: '800',
    marginBottom: 8,
  },
  parrafo: {
    color: colors.textSecondary,
    fontSize: 14,
    lineHeight: 21,
    marginBottom: 8,
  },
  version: {
    color: colors.textMuted,
    fontSize: 12,
    textAlign: 'center',
    marginTop: 16,
  },
});
