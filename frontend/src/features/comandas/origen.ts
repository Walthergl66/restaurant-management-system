import { mesaCode } from '@/shared/lib/mesaCode'

/** De dónde viene una comanda: la mesa del salón o la forma de entrega del pedido de la app. */
export function etiquetaOrigen(comanda: { canal?: string; mesaNumero?: number | null; entrega?: string | null }): string {
  if (comanda.mesaNumero != null) return `Mesa ${mesaCode(comanda.mesaNumero)}`
  if (comanda.canal === 'APP') {
    if (comanda.entrega === 'RETIRAR') return 'Retiro'
    if (comanda.entrega === 'DOMICILIO') return 'Domicilio'
    return 'Pedido app'
  }
  return 'Salón'
}
