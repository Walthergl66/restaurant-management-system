import { PedidosListosScreen } from '@/features/mostrador';

/** Mostrador de la cocina: entrega los pedidos de retiro que ya están listos. */
export default function MostradorDeCocina() {
  return <PedidosListosScreen incluirSalon={false} />;
}
