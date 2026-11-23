import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { colors } from '@/theme/colors';
import { accionesDe, etiquetaOrigen } from '../agrupar';
import type { AccionComanda, ComandaCocina } from '../types';

const TEXTO_ACCION: Record<AccionComanda, string> = {
  PREPARAR: 'Preparar',
  LISTO: 'Listo',
  ENTERADO: 'Enterado',
};

interface ComandaCardProps {
  comanda: ComandaCocina;
  /** Hay una acción en curso sobre esta comanda: se deshabilitan los botones. */
  ocupada: boolean;
  onAccion: (comanda: ComandaCocina, accion: AccionComanda) => void;
}

/** Tarjeta de una comanda: de dónde viene, qué lleva y las acciones que se pueden aplicar. */
export function ComandaCard({ comanda, ocupada, onAccion }: ComandaCardProps) {
  const esCancelacion = comanda.tipo === 'CANCELACION';
  const origen = etiquetaOrigen(comanda);
  const varianteOrigen = comanda.mesaNumero != null ? 'orange' : comanda.entrega === 'DOMICILIO' ? 'pink' : 'success';

  return (
    <Card style={{ ...styles.tarjeta, ...(esCancelacion ? styles.cancelacion : {}) }}>
      <View style={styles.encabezado}>
        <Text style={styles.numero}>#{comanda.numeroComanda}</Text>
        <View style={styles.insignias}>
          {esCancelacion && <Badge text="CANCELACIÓN" variant="error" />}
          <Badge text={origen} variant={varianteOrigen} />
        </View>
      </View>
      <Text style={styles.meta}>{comanda.areaNombre} · pedido {comanda.pedidoCodigo}</Text>

      <View style={styles.lineas}>
        {comanda.lineas.map(linea => (
          <View key={`${linea.productoId}-${linea.orden}`} style={styles.linea}>
            <Text style={[styles.producto, esCancelacion && styles.productoCancelado]}>
              {linea.cantidad}× {linea.nombreProducto}
            </Text>
            {!!linea.extras && <Text style={styles.detalle}>+ {linea.extras}</Text>}
            {!!linea.ingredientes && <Text style={styles.detalle}>Sin: {linea.ingredientes}</Text>}
            {!!linea.observaciones && <Text style={styles.observacion}>“{linea.observaciones}”</Text>}
          </View>
        ))}
      </View>

      <View style={styles.acciones}>
        {accionesDe(comanda).map(accion => (
          <Button
            key={accion}
            title={TEXTO_ACCION[accion]}
            onPress={() => onAccion(comanda, accion)}
            disabled={ocupada}
            loading={ocupada}
            size="md"
            variant={accion === 'PREPARAR' ? 'outline' : 'primary'}
            style={accion === 'ENTERADO' ? styles.botonEnterado : styles.boton}
          />
        ))}
      </View>
    </Card>
  );
}

const styles = StyleSheet.create({
  tarjeta: { padding: 16, marginHorizontal: 16, marginBottom: 12, gap: 6 },
  cancelacion: { borderWidth: 2, borderColor: colors.error },
  encabezado: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  numero: { color: colors.textOnSurface, fontSize: 22, fontWeight: '800' },
  insignias: { flexDirection: 'row', gap: 6 },
  meta: { color: colors.textSecondary, fontSize: 12 },
  lineas: { marginTop: 6, gap: 8 },
  linea: { gap: 2 },
  producto: { color: colors.textOnSurface, fontSize: 16, fontWeight: '700' },
  productoCancelado: { color: colors.error, textDecorationLine: 'line-through' },
  detalle: { color: colors.textSecondary, fontSize: 13 },
  observacion: { color: colors.neonPink, fontSize: 13, fontStyle: 'italic' },
  acciones: { flexDirection: 'row', gap: 10, marginTop: 10 },
  boton: { flex: 1 },
  botonEnterado: { flex: 1, backgroundColor: colors.error },
});
