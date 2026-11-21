import { Alert, Platform } from 'react-native';
import type { AlertButton } from 'react-native';

/**
 * Diálogo de aviso o confirmación que funciona en el teléfono y en el navegador.
 * `Alert.alert` de React Native no hace nada en la versión web, así que allí se usan los
 * diálogos del navegador. Misma firma que `Alert.alert`: el botón con estilo `cancel` es la
 * salida negativa y el último botón sin ese estilo es la acción afirmativa.
 */
export function mostrarDialogo(titulo: string, mensaje?: string, botones?: AlertButton[]): void {
  if (Platform.OS !== 'web') {
    Alert.alert(titulo, mensaje, botones);
    return;
  }

  const texto = mensaje ? `${titulo}\n\n${mensaje}` : titulo;
  const afirmativo = [...(botones ?? [])].reverse().find(b => b.style !== 'cancel');
  const cancelar = botones?.find(b => b.style === 'cancel');

  if (!botones || botones.length < 2) {
    window.alert(texto);
    (botones?.[0] ?? afirmativo)?.onPress?.();
    return;
  }
  if (window.confirm(texto)) afirmativo?.onPress?.();
  else cancelar?.onPress?.();
}
