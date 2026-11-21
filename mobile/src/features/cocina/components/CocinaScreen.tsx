import React, { useCallback, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, TouchableOpacity, Vibration, View } from 'react-native';
import { mostrarDialogo } from '@/components/ui/dialogo';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { ChipTabs } from '@/components/ChipTabs';
import { PerfilSheet } from '@/features/auth';
import { useSondeo } from '@/shared/hooks/useSondeo';
import { colors } from '@/theme/colors';
import { cocinaService } from '../api';
import { agruparPorEstado } from '../agrupar';
import type { AccionComanda, AreaCocina, ComandaCocina, EstadoComanda } from '../types';
import { ComandaCard } from './ComandaCard';
import { FiltroAreas } from './FiltroAreas';

const INTERVALO_SONDEO_MS = 8_000;

const TITULO_ESTADO: Record<EstadoComanda, string> = {
  PENDIENTE: 'Pendientes',
  EN_PREPARACION: 'Preparando',
  LISTO: 'Listos',
};

const VACIO_ESTADO: Record<EstadoComanda, string> = {
  PENDIENTE: 'No hay comandas pendientes.',
  EN_PREPARACION: 'Nada en preparación.',
  LISTO: 'Todavía no hay comandas listas.',
};

interface CocinaScreenProps {
  /** Muestra el ícono de perfil con "Cerrar sesión" (en la app de cocina; el mesero ya lo tiene en su pantalla). */
  conPerfil?: boolean;
}

/**
 * Comandas para preparar: pendientes, en preparación y listas, con filtro por área. Quien la usa
 * (cocina o mesero) marca los estados a mano; el pedido avanza cuando todas sus áreas lo hacen.
 */
export default function CocinaScreen({ conPerfil = false }: CocinaScreenProps) {
  const insets = useSafeAreaInsets();
  const [comandas, setComandas] = useState<ComandaCocina[]>([]);
  const [areas, setAreas] = useState<AreaCocina[]>([]);
  const [areaId, setAreaId] = useState<number | null>(null);
  const [estado, setEstado] = useState<EstadoComanda>('PENDIENTE');
  const [cargando, setCargando] = useState(true);
  const [refrescando, setRefrescando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [enCurso, setEnCurso] = useState<Set<number>>(new Set());
  const [perfilVisible, setPerfilVisible] = useState(false);
  const pendientesPrevios = useRef<number | null>(null);

  const cargar = useCallback(async () => {
    try {
      const [lista, listaAreas] = await Promise.all([cocinaService.listarActivas(areaId), cocinaService.listarAreas()]);
      setComandas(lista);
      setAreas(listaAreas);
      setError(null);
      // Aviso corto cuando llegan comandas nuevas (no en la primera carga).
      const pendientes = lista.filter(c => c.estado === 'PENDIENTE').length;
      if (pendientesPrevios.current !== null && pendientes > pendientesPrevios.current) {
        Vibration.vibrate(300);
      }
      pendientesPrevios.current = pendientes;
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar las comandas');
    } finally {
      setCargando(false);
      setRefrescando(false);
    }
  }, [areaId]);

  useSondeo(cargar, INTERVALO_SONDEO_MS);

  const grupos = useMemo(() => agruparPorEstado(comandas), [comandas]);

  const cambiarArea = (id: number | null) => {
    pendientesPrevios.current = null;
    setAreaId(id);
  };

  const aplicar = async (comanda: ComandaCocina, accion: AccionComanda) => {
    setEnCurso(actual => new Set(actual).add(comanda.id));
    try {
      await cocinaService.ejecutar(comanda, accion);
      await cargar();
    } catch (e) {
      // La verdad es el backend: si falla, la lista no se altera y se avisa.
      mostrarDialogo('No se pudo actualizar la comanda', e instanceof Error ? e.message : 'Inténtalo nuevamente');
    } finally {
      setEnCurso(actual => {
        const siguiente = new Set(actual);
        siguiente.delete(comanda.id);
        return siguiente;
      });
    }
  };

  const visibles = grupos[estado];

  return (
    <View style={[styles.screen, { paddingTop: insets.top + 8 }]}>
      <View style={styles.header}>
        {conPerfil ? (
          <TouchableOpacity onPress={() => setPerfilVisible(true)} accessibilityLabel="Abrir perfil" style={styles.lateral}>
            <Ionicons name="person-circle-outline" size={30} color={colors.textPrimary} />
          </TouchableOpacity>
        ) : (
          <View style={styles.lateral} />
        )}
        <Text style={styles.title}>Cocina</Text>
        <View style={styles.lateral} />
      </View>

      <FiltroAreas areas={areas} seleccionada={areaId} onSelect={cambiarArea} />
      <ChipTabs<EstadoComanda>
        opciones={(Object.keys(TITULO_ESTADO) as EstadoComanda[]).map(id => ({
          id,
          etiqueta: TITULO_ESTADO[id],
          contador: grupos[id].length,
        }))}
        seleccionado={estado}
        onSelect={id => id && setEstado(id)}
      />

      {cargando ? (
        <View style={styles.centro}>
          <ActivityIndicator color={colors.neonOrange} />
        </View>
      ) : (
        <ScrollView
          contentContainerStyle={{ paddingTop: 4, paddingBottom: insets.bottom + 24 }}
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
                <Text style={styles.reintentar}>Reintentar</Text>
              </TouchableOpacity>
            </View>
          )}

          {visibles.length === 0 && !error && <Text style={styles.vacio}>{VACIO_ESTADO[estado]}</Text>}

          {visibles.map(comanda => (
            <ComandaCard key={comanda.id} comanda={comanda} ocupada={enCurso.has(comanda.id)} onAccion={aplicar} />
          ))}
        </ScrollView>
      )}

      {conPerfil && <PerfilSheet visible={perfilVisible} onClose={() => setPerfilVisible(false)} />}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, paddingBottom: 4 },
  lateral: { width: 42, alignItems: 'center', padding: 4 },
  title: { flex: 1, color: colors.textPrimary, fontSize: 20, fontWeight: '800', textAlign: 'center' },
  centro: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  vacio: { color: colors.textMuted, textAlign: 'center', padding: 32, fontSize: 14 },
  errorBox: {
    margin: 16,
    padding: 14,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.error,
    backgroundColor: 'rgba(239, 68, 68, 0.1)',
    gap: 8,
  },
  error: { color: colors.error, fontSize: 14 },
  reintentar: { color: colors.neonOrange, fontSize: 14, fontWeight: '700' },
});
