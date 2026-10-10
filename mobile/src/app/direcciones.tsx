/**
 * Estación Burger — Direcciones del cliente (RF-42)
 *
 * Lista las direcciones activas y permite registrar una nueva.
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
import type { DireccionCliente } from '../features/pedidos/types';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';

export default function DireccionesScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  const [direcciones, setDirecciones] = useState<DireccionCliente[]>([]);
  const [loading, setLoading] = useState(true);
  const [formVisible, setFormVisible] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [etiqueta, setEtiqueta] = useState('');
  const [direccion, setDireccion] = useState('');
  const [telefono, setTelefono] = useState('');
  const [observaciones, setObservaciones] = useState('');

  const cargar = useCallback(async () => {
    try {
      setError(null);
      const data = await pedidosService.getDirecciones();
      setDirecciones(data);
    } catch (err: any) {
      setError(err.message || 'Error al cargar direcciones');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const handleGuardar = async () => {
    if (!etiqueta.trim() || !direccion.trim()) {
      setError('Etiqueta y dirección son obligatorias');
      return;
    }
    setError(null);
    setGuardando(true);
    try {
      await pedidosService.agregarDireccion({
        etiqueta: etiqueta.trim(),
        direccion: direccion.trim(),
        telefono: telefono.trim() || undefined,
        observaciones: observaciones.trim() || undefined,
      });
      setEtiqueta('');
      setDireccion('');
      setTelefono('');
      setObservaciones('');
      setFormVisible(false);
      await cargar();
    } catch (err: any) {
      setError(err.message || 'No se pudo guardar la dirección');
    } finally {
      setGuardando(false);
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
        <Text style={styles.headerTitle}>Direcciones</Text>
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
            <Text style={styles.formTitle}>Nueva dirección</Text>
            <Input
              label="ETIQUETA"
              placeholder="Casa, oficina..."
              value={etiqueta}
              onChangeText={setEtiqueta}
              icon="bookmark-outline"
              maxLength={40}
            />
            <Input
              label="DIRECCIÓN"
              placeholder="Calle, número, colonia..."
              value={direccion}
              onChangeText={setDireccion}
              icon="location-outline"
              maxLength={200}
            />
            <Input
              label="TELÉFONO (OPCIONAL)"
              placeholder="555 000 0000"
              value={telefono}
              onChangeText={setTelefono}
              icon="call-outline"
              keyboardType="phone-pad"
              maxLength={20}
            />
            <Input
              label="OBSERVACIONES (OPCIONAL)"
              placeholder="Referencias, portón..."
              value={observaciones}
              onChangeText={setObservaciones}
              icon="document-text-outline"
              maxLength={200}
            />
            <Button
              title="GUARDAR DIRECCIÓN"
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
        ) : direcciones.length === 0 ? (
          <View style={styles.empty}>
            <Ionicons
              name="location-outline"
              size={64}
              color={colors.textMuted}
            />
            <Text style={styles.emptyTitle}>Sin direcciones</Text>
            <Text style={styles.emptySubtitle}>
              Agrega una dirección para tus pedidos a domicilio
            </Text>
          </View>
        ) : (
          direcciones.map(dir => (
            <View key={dir.id} style={styles.card}>
              <View style={styles.cardIcon}>
                <Ionicons
                  name="location"
                  size={20}
                  color={colors.neonOrange}
                />
              </View>
              <View style={styles.cardInfo}>
                <Text style={styles.cardEtiqueta}>{dir.etiqueta}</Text>
                <Text style={styles.cardDireccion}>{dir.direccion}</Text>
                {dir.telefono && (
                  <Text style={styles.cardMeta}>
                    <Ionicons name="call" size={11} color={colors.textMuted} />{' '}
                    {dir.telefono}
                  </Text>
                )}
                {dir.observaciones && (
                  <Text style={styles.cardMeta}>{dir.observaciones}</Text>
                )}
              </View>
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
  cardEtiqueta: {
    color: colors.textPrimary,
    fontSize: 15,
    fontWeight: '700',
  },
  cardDireccion: {
    color: colors.textSecondary,
    fontSize: 13,
    marginTop: 2,
  },
  cardMeta: {
    color: colors.textMuted,
    fontSize: 12,
    marginTop: 4,
  },
});
