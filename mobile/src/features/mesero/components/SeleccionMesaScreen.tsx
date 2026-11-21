import React, { useCallback, useMemo, useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { PerfilSheet, useAuth, puedeAtenderMesas } from '@/features/auth';
import { useSondeo } from '@/shared/hooks/useSondeo';
import { colors } from '@/theme/colors';
import { meseroService } from '../api';
import { agruparMesasPorZona } from '../zonas';
import type { MesaMesero } from '../types';
import { MesaGrid } from './MesaGrid';
import { SelectorZonas } from './SelectorZonas';

const INTERVALO_SONDEO_MS = 30_000;

/** Paso 1 del mesero: elegir la mesa, agrupadas por zona, con las ocupadas marcadas. */
export default function SeleccionMesaScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { usuario } = useAuth();
  const [perfilVisible, setPerfilVisible] = useState(false);
  const [mesas, setMesas] = useState<MesaMesero[]>([]);
  const [zona, setZona] = useState<string | null>(null);
  const [cargando, setCargando] = useState(true);
  const [refrescando, setRefrescando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const autorizado = puedeAtenderMesas(usuario);

  const cargar = useCallback(async () => {
    if (!autorizado) return;
    try {
      setMesas(await meseroService.listarMesasHabilitadas());
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar las mesas');
    } finally {
      setCargando(false);
      setRefrescando(false);
    }
  }, [autorizado]);

  useSondeo(cargar, INTERVALO_SONDEO_MS);

  const zonas = useMemo(() => agruparMesasPorZona(mesas), [mesas]);
  const zonaActiva = zonas.find(z => z.zona === zona) ?? zonas[0];

  const abrirMesa = (mesa: MesaMesero) => router.push(`/(waiter)/mesa/${mesa.id}?numero=${mesa.numero}`);

  if (!autorizado) {
    return (
      <View style={styles.center}>
        <Text style={styles.error}>No tienes permisos de mesero.</Text>
      </View>
    );
  }

  return (
    <View style={[styles.screen, { paddingTop: insets.top + 8 }]}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => setPerfilVisible(true)} accessibilityLabel="Abrir perfil" style={styles.iconoLateral}>
          <Ionicons name="person-circle-outline" size={30} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.title}>Seleccionar mesa</Text>
        <View style={styles.iconoLateral} />
      </View>

      {cargando ? (
        <View style={styles.center}>
          <ActivityIndicator color={colors.neonOrange} />
        </View>
      ) : (
        <ScrollView
          contentContainerStyle={{ paddingBottom: insets.bottom + 24 }}
          refreshControl={
            <RefreshControl
              refreshing={refrescando}
              onRefresh={() => {
                setRefrescando(true);
                void cargar();
              }}
              tintColor={colors.neonOrange}
            />
          }
        >
          {error && (
            <View style={styles.errorBox}>
              <Text style={styles.error}>{error}</Text>
              <TouchableOpacity onPress={() => void cargar()}>
                <Text style={styles.retry}>Reintentar</Text>
              </TouchableOpacity>
            </View>
          )}

          {zonas.length > 0 && (
            <SelectorZonas zonas={zonas} seleccionada={zonaActiva?.zona ?? null} onSelect={setZona} />
          )}

          {zonaActiva ? (
            <MesaGrid mesas={zonaActiva.mesas} onSeleccionar={abrirMesa} />
          ) : (
            !error && (
              <Text style={styles.empty}>
                No hay mesas habilitadas. Pídele al administrador que cree o habilite mesas.
              </Text>
            )
          )}
        </ScrollView>
      )}

      <PerfilSheet visible={perfilVisible} onClose={() => setPerfilVisible(false)} />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background },
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, paddingBottom: 4 },
  iconoLateral: { width: 42, alignItems: 'center', padding: 4 },
  title: { flex: 1, color: colors.textPrimary, fontSize: 20, fontWeight: '800', textAlign: 'center' },
  empty: { color: colors.textMuted, fontSize: 14, textAlign: 'center', padding: 32 },
  errorBox: {
    margin: 20,
    padding: 14,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.error,
    backgroundColor: 'rgba(239, 68, 68, 0.1)',
    gap: 8,
  },
  error: { color: colors.error, fontSize: 14 },
  retry: { color: colors.neonOrange, fontSize: 14, fontWeight: '700' },
});
