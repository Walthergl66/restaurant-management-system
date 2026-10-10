/**
 * Estación Burger — Métodos de pago guardados (RF-45)
 *
 * Lista y administra métodos de pago. Solo se guarda metadata no
 * sensible (tipo, alias, últimos 4 dígitos); nunca el número completo
 * ni el CVV.
 */

import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../theme/colors';
import { pedidosService } from '../features/pedidos/pedidosService';
import type { MetodoPagoCliente } from '../features/pedidos/types';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';

const TIPOS = ['EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'OTRO'] as const;
type Tipo = (typeof TIPOS)[number];

const ICONO_TIPO: Record<string, keyof typeof Ionicons.glyphMap> = {
  EFECTIVO: 'cash-outline',
  TARJETA: 'card-outline',
  TRANSFERENCIA: 'swap-horizontal-outline',
  OTRO: 'wallet-outline',
};

export default function MetodosPagoScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  const [metodos, setMetodos] = useState<MetodoPagoCliente[]>([]);
  const [loading, setLoading] = useState(true);
  const [formVisible, setFormVisible] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [tipo, setTipo] = useState<Tipo>('TARJETA');
  const [alias, setAlias] = useState('');
  const [ultimos4, setUltimos4] = useState('');
  const [predeterminado, setPredeterminado] = useState(false);

  const cargar = useCallback(async () => {
    try {
      setError(null);
      const data = await pedidosService.getMetodosPago();
      setMetodos(data);
    } catch (err: any) {
      setError(err.message || 'Error al cargar métodos de pago');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const handleGuardar = async () => {
    if (!alias.trim()) {
      setError('El alias es obligatorio');
      return;
    }
    if (ultimos4 && !/^\d{4}$/.test(ultimos4)) {
      setError('Los últimos 4 dígitos deben ser 4 números');
      return;
    }
    setError(null);
    setGuardando(true);
    try {
      await pedidosService.agregarMetodoPago({
        tipo,
        alias: alias.trim(),
        ultimos4: ultimos4.trim() || undefined,
        predeterminado,
      });
      setAlias('');
      setUltimos4('');
      setPredeterminado(false);
      setFormVisible(false);
      await cargar();
    } catch (err: any) {
      setError(err.message || 'No se pudo guardar el método de pago');
    } finally {
      setGuardando(false);
    }
  };

  const handleEliminar = async (id: number) => {
    try {
      await pedidosService.eliminarMetodoPago(id);
      await cargar();
    } catch (err: any) {
      setError(err.message || 'No se pudo eliminar');
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={[styles.header, { paddingTop: insets.top + 12 }]}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Ionicons name="arrow-back" size={24} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Métodos de pago</Text>
        <TouchableOpacity
          onPress={() => {
            setFormVisible(v => !v);
            setError(null);
          }}
          style={styles.backBtn}
        >
          <Ionicons
            name={formVisible ? 'close' : 'add'}
            size={24}
            color={colors.neonOrange}
          />
        </TouchableOpacity>
      </View>

      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
      >
        {formVisible && (
          <View style={styles.formSection}>
            <Text style={styles.formTitle}>Nuevo método</Text>

            <Text style={styles.label}>TIPO</Text>
            <View style={styles.tiposRow}>
              {TIPOS.map(t => (
                <TouchableOpacity
                  key={t}
                  onPress={() => setTipo(t)}
                  style={[styles.tipo, tipo === t && styles.tipoActivo]}
                >
                  <Ionicons
                    name={ICONO_TIPO[t]}
                    size={16}
                    color={tipo === t ? colors.background : colors.neonOrange}
                  />
                  <Text
                    style={[
                      styles.tipoText,
                      tipo === t && styles.tipoTextActivo,
                    ]}
                  >
                    {t}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            <Input
              label="ALIAS"
              placeholder="Mi tarjeta, efectivo..."
              value={alias}
              onChangeText={setAlias}
              icon="pricetag-outline"
              maxLength={40}
            />

            {(tipo === 'TARJETA' || tipo === 'TRANSFERENCIA') && (
              <Input
                label="ÚLTIMOS 4 (OPCIONAL)"
                placeholder="1234"
                value={ultimos4}
                onChangeText={setUltimos4}
                icon="keypad-outline"
                keyboardType="number-pad"
                maxLength={4}
              />
            )}

            <TouchableOpacity
              style={styles.checkRow}
              onPress={() => setPredeterminado(v => !v)}
            >
              <View
                style={[
                  styles.checkbox,
                  predeterminado && styles.checkboxActivo,
                ]}
              >
                {predeterminado && (
                  <Ionicons
                    name="checkmark"
                    size={14}
                    color={colors.background}
                  />
                )}
              </View>
              <Text style={styles.checkLabel}>Usar como predeterminado</Text>
            </TouchableOpacity>

            <Button
              title="GUARDAR MÉTODO"
              onPress={handleGuardar}
              loading={guardando}
              size="lg"
            />
          </View>
        )}

        {error && (
          <View style={styles.errorContainer}>
            <Ionicons name="alert-circle" size={16} color={colors.error} />
            <Text style={styles.errorText}>{error}</Text>
          </View>
        )}

        {loading ? (
          <ActivityIndicator
            size="large"
            color={colors.neonOrange}
            style={styles.loader}
          />
        ) : metodos.length === 0 ? (
          <View style={styles.empty}>
            <Ionicons name="card-outline" size={64} color={colors.textMuted} />
            <Text style={styles.emptyTitle}>Sin métodos guardados</Text>
            <Text style={styles.emptySubtitle}>
              Guarda un método para agilizar tus pagos
            </Text>
          </View>
        ) : (
          metodos.map(metodo => (
            <View key={metodo.id} style={styles.card}>
              <View style={styles.cardIcon}>
                <Ionicons
                  name={ICONO_TIPO[metodo.tipo] || 'wallet-outline'}
                  size={20}
                  color={colors.neonOrange}
                />
              </View>
              <View style={styles.cardInfo}>
                <View style={styles.cardTop}>
                  <Text style={styles.cardAlias}>{metodo.alias}</Text>
                  {metodo.predeterminado && (
                    <View style={styles.badge}>
                      <Text style={styles.badgeText}>PREDET.</Text>
                    </View>
                  )}
                </View>
                <Text style={styles.cardMeta}>
                  {metodo.tipo}
                  {metodo.ultimos4 ? ` •••• ${metodo.ultimos4}` : ''}
                </Text>
              </View>
              <TouchableOpacity
                onPress={() => handleEliminar(metodo.id)}
                style={styles.deleteBtn}
              >
                <Ionicons
                  name="trash-outline"
                  size={20}
                  color={colors.error}
                />
              </TouchableOpacity>
            </View>
          ))
        )}
      </ScrollView>
    </KeyboardAvoidingView>
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
    padding: 16,
    paddingBottom: 40,
  },
  formSection: {
    backgroundColor: colors.surface,
    borderRadius: 16,
    padding: 16,
    marginBottom: 20,
  },
  formTitle: {
    color: colors.textPrimary,
    fontSize: 16,
    fontWeight: '800',
    marginBottom: 16,
  },
  label: {
    color: colors.textSecondary,
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 8,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  tiposRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 16,
  },
  tipo: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: colors.surfaceLight,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  tipoActivo: {
    backgroundColor: colors.neonOrange,
    borderColor: colors.neonOrange,
  },
  tipoText: {
    color: colors.textSecondary,
    fontSize: 12,
    fontWeight: '700',
  },
  tipoTextActivo: {
    color: colors.background,
  },
  checkRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    marginBottom: 20,
  },
  checkbox: {
    width: 24,
    height: 24,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: colors.surfaceBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkboxActivo: {
    backgroundColor: colors.neonOrange,
    borderColor: colors.neonOrange,
  },
  checkLabel: {
    color: colors.textSecondary,
    fontSize: 14,
  },
  errorContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 16,
    paddingHorizontal: 4,
  },
  errorText: {
    color: colors.error,
    fontSize: 13,
    flex: 1,
  },
  loader: {
    marginTop: 40,
  },
  empty: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 80,
    gap: 8,
  },
  emptyTitle: {
    color: colors.textPrimary,
    fontSize: 18,
    fontWeight: '700',
    marginTop: 8,
  },
  emptySubtitle: {
    color: colors.textMuted,
    fontSize: 14,
    textAlign: 'center',
    paddingHorizontal: 32,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: 16,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  cardIcon: {
    width: 40,
    height: 40,
    borderRadius: 12,
    backgroundColor: colors.neonOrangeAlpha,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  cardInfo: {
    flex: 1,
  },
  cardTop: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  cardAlias: {
    color: colors.textPrimary,
    fontSize: 15,
    fontWeight: '700',
  },
  badge: {
    backgroundColor: colors.neonPinkAlpha,
    borderRadius: 6,
    paddingHorizontal: 6,
    paddingVertical: 2,
  },
  badgeText: {
    color: colors.neonPink,
    fontSize: 9,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  cardMeta: {
    color: colors.textMuted,
    fontSize: 13,
    marginTop: 2,
  },
  deleteBtn: {
    padding: 8,
  },
});
