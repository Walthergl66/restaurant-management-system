import React, { useCallback, useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { mostrarDialogo } from '@/components/ui/dialogo';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { useSondeo } from '@/shared/hooks/useSondeo';
import { colors } from '@/theme/colors';
import { mostradorService } from '../api';
import { etiquetaEntrega } from '../unir';
import type { PedidoMostrador } from '../types';

const INTERVALO_SONDEO_MS = 15_000;

interface PedidosListosScreenProps {
  /** El mesero ve también los pedidos de sus mesas; la cocina, solo los de retiro. */
  incluirSalon: boolean;
}

/** Pedidos que ya están listos: se entregan a la mesa (salón) o al cliente (retiro en el local). */
export default function PedidosListosScreen({ incluirSalon }: PedidosListosScreenProps) {
  const insets = useSafeAreaInsets();
  const [pedidos, setPedidos] = useState<PedidoMostrador[]>([]);
  const [avisos, setAvisos] = useState<string[]>([]);
  const [cargando, setCargando] = useState(true);
  const [refrescando, setRefrescando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [enCurso, setEnCurso] = useState<Set<string>>(new Set());

  const cargar = useCallback(async () => {
    try {
      const resultado = await mostradorService.cargar({ incluirSalon });
      setPedidos(resultado.pedidos);
      setAvisos(resultado.avisos);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar los pedidos');
    } finally {
      setCargando(false);
      setRefrescando(false);
    }
  }, [incluirSalon]);

  useSondeo(cargar, INTERVALO_SONDEO_MS);

  const confirmarEntrega = (pedido: PedidoMostrador) => {
    const destino = pedido.origen === 'SALON' ? 'a la mesa' : 'al cliente';
    mostrarDialogo('Confirmar entrega', `¿Entregaste el pedido ${pedido.codigo} ${destino}?`, [
      { text: 'Cancelar', style: 'cancel' },
      { text: 'Sí, entregado', onPress: () => void entregar(pedido) },
    ]);
  };

  const entregar = async (pedido: PedidoMostrador) => {
    setEnCurso(actual => new Set(actual).add(pedido.codigo));
    try {
      await mostradorService.entregar(pedido);
      await cargar();
    } catch (e) {
      // Si falla, la tarjeta no cambia: el backend es la fuente de verdad.
      mostrarDialogo('No se pudo registrar la entrega', e instanceof Error ? e.message : 'Inténtalo nuevamente');
    } finally {
      setEnCurso(actual => {
        const siguiente = new Set(actual);
        siguiente.delete(pedido.codigo);
        return siguiente;
      });
    }
  };

  return (
    <View style={[styles.screen, { paddingTop: insets.top + 8 }]}>
      <Text style={styles.title}>{incluirSalon ? 'Listos para entregar' : 'Mostrador'}</Text>

      {cargando ? (
        <View style={styles.centro}>
          <ActivityIndicator color={colors.neonOrange} />
        </View>
      ) : (
        <ScrollView
          contentContainerStyle={{ paddingTop: 8, paddingBottom: insets.bottom + 24 }}
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
          {avisos.map(aviso => (
            <Text key={aviso} style={styles.aviso}>{aviso}</Text>
          ))}

          {!error && pedidos.length === 0 && <Text style={styles.vacio}>No hay pedidos listos para entregar.</Text>}

          {pedidos.map(pedido => (
            <Card key={`${pedido.origen}-${pedido.codigo}`} style={styles.tarjeta}>
              <View style={styles.encabezado}>
                <Text style={styles.codigo}>{pedido.codigo}</Text>
                <Badge text={etiquetaEntrega(pedido)} variant={pedido.origen === 'SALON' ? 'orange' : 'success'} />
              </View>
              <View style={styles.lineas}>
                {pedido.lineas.map((linea, i) => (
                  <Text key={`${linea.nombreProducto}-${i}`} style={styles.linea}>
                    {linea.cantidad}× {linea.nombreProducto}
                  </Text>
                ))}
              </View>
              <Button
                title={pedido.origen === 'SALON' ? 'Entregar a la mesa' : 'Entregar al cliente'}
                onPress={() => confirmarEntrega(pedido)}
                disabled={enCurso.has(pedido.codigo)}
                loading={enCurso.has(pedido.codigo)}
                size="md"
              />
            </Card>
          ))}
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  title: { color: colors.textPrimary, fontSize: 20, fontWeight: '800', textAlign: 'center', paddingBottom: 4 },
  centro: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  tarjeta: { padding: 16, marginHorizontal: 16, marginBottom: 12, gap: 10 },
  encabezado: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  codigo: { color: colors.textOnSurface, fontSize: 15, fontWeight: '800', flexShrink: 1 },
  lineas: { gap: 4 },
  linea: { color: colors.textOnSurface, fontSize: 15 },
  vacio: { color: colors.textMuted, textAlign: 'center', padding: 32, fontSize: 14 },
  aviso: { color: colors.warning, fontSize: 13, marginHorizontal: 16, marginBottom: 8 },
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
