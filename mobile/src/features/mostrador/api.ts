import { apiClient } from '@/shared/api/apiClient';
import { meseroService } from '@/features/mesero';
import { desdeRetiro, desdeSalon, unirListos } from './unir';
import type { PedidoMostrador, PedidoRetiroApi, PedidoSalonApi, ResultadoMostrador } from './types';

const ENDPOINTS = {
  salonListos: '/pedidos?estado=LISTO',
  retiroListos: '/clientes/operacion/pedidos?estado=LISTO&entrega=RETIRAR',
  entregarSalon: (codigo: string) => `/pedidos/${codigo}/entregar`,
  entregarRetiro: (codigo: string) => `/clientes/pedidos/${codigo}/entregar`,
} as const;

async function cargarSalon(): Promise<PedidoMostrador[]> {
  const [pedidos, mesas] = await Promise.all([
    apiClient.get<PedidoSalonApi[]>(ENDPOINTS.salonListos),
    meseroService.listarMesas(),
  ]);
  const numeros = new Map(mesas.map(mesa => [mesa.id, mesa.numero] as [number, number]));
  return pedidos.map(pedido => desdeSalon(pedido, numeros));
}

async function cargarRetiro(): Promise<PedidoMostrador[]> {
  const pedidos = await apiClient.get<PedidoRetiroApi[]>(ENDPOINTS.retiroListos);
  return pedidos.map(desdeRetiro);
}

const mensaje = (motivo: unknown) => (motivo instanceof Error ? motivo.message : 'Error desconocido');

export const mostradorService = {
  /**
   * Pedidos listos para entregar. Con `incluirSalon` (el mesero) se leen también los de las mesas.
   * Si una de las dos listas falla (p. ej. un permiso que falta) se muestra la otra y se devuelve un
   * aviso; solo si no se pudo leer ninguna se lanza el error.
   */
  async cargar(opciones: { incluirSalon: boolean }): Promise<ResultadoMostrador> {
    const [salon, retiro] = await Promise.allSettled([
      opciones.incluirSalon ? cargarSalon() : Promise.resolve<PedidoMostrador[]>([]),
      cargarRetiro(),
    ]);
    if (salon.status === 'rejected' && retiro.status === 'rejected') {
      throw new Error(mensaje(retiro.reason));
    }
    const avisos: string[] = [];
    if (salon.status === 'rejected') avisos.push(`No se pudieron cargar los pedidos de las mesas: ${mensaje(salon.reason)}`);
    if (retiro.status === 'rejected') avisos.push(`No se pudieron cargar los pedidos de retiro: ${mensaje(retiro.reason)}`);
    return {
      pedidos: unirListos(
        salon.status === 'fulfilled' ? salon.value : [],
        retiro.status === 'fulfilled' ? retiro.value : [],
      ),
      avisos,
    };
  },

  /** Marca entregado: en salón sirve el pedido a la mesa; en retiro, se lo entrega al cliente. */
  async entregar(pedido: PedidoMostrador): Promise<void> {
    const endpoint = pedido.origen === 'SALON'
      ? ENDPOINTS.entregarSalon(pedido.codigo)
      : ENDPOINTS.entregarRetiro(pedido.codigo);
    await apiClient.post(endpoint);
  },
};
