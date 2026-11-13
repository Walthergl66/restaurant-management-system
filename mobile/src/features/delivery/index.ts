export { deliveryService } from './api';
export { useMisEntregas } from './useMisEntregas';
export { useEntregaIndividual } from './useEntregaIndividual';
export { useMultientrega, MIN_PARADAS_MULTIENTREGA } from './useMultientrega';
export { useRecorridoActivo } from './useRecorridoActivo';
export { colaEntregas } from './colaOffline';
export { decodePolyline } from './polyline';
export { nuevoUuid } from './uuid';
export {
  estiloEstadoAsignacion,
  estiloEstadoParada,
  estiloEstadoRecorrido,
} from './estados';
export { default as MisEntregasScreen } from './components/MisEntregasScreen';
export { default as DetalleEntregaScreen } from './components/DetalleEntregaScreen';
export { default as MultientregaScreen } from './components/MultientregaScreen';
export { default as RecorridoActivoScreen } from './components/RecorridoActivoScreen';
export { default as EntregaCard } from './components/EntregaCard';
export { default as EstadoBadge } from './components/EstadoBadge';

// NOTA: MapaRuta (react-native-maps) y useUbicacion (expo-location) NO se
// re-exportan desde el barrel a propósito: solo se importan por ruta relativa
// desde DetalleEntregaScreen (pantalla del grupo (delivery)), para no arrastrar
// módulos nativos a flujos de cliente/mesero en Expo Go.
export type { LatLng } from './polyline';
export type { EstadoEntregaIndividual, AccionCargando } from './useEntregaIndividual';
export type { EstadoMultientrega, AccionMultientrega } from './useMultientrega';
export type {
  EstadoRecorridoActivo,
  AccionRecorrido,
  EstadoParadaUi,
} from './useRecorridoActivo';
export type { EntregaEncolada } from './colaOffline';
export type {
  CriterioOptimizacion,
  EntregaResumen,
  MisEntregas,
  ParadaPropuesta,
  ParadaResponse,
  ParadaResumen,
  PropuestaMultientrega,
  RecorridoResumen,
  RutaCalculada,
  TipoRecorrido,
} from './types';
