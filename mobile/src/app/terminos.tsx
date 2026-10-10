/**
 * Estación Burger — Términos y condiciones
 */

import React from 'react';
import { InfoPage } from '../components/InfoPage';

export default function TerminosScreen() {
  return (
    <InfoPage
      title="Términos y condiciones"
      secciones={[
        {
          titulo: '1. Uso del servicio',
          parrafos: [
            'Estación Burger ofrece un servicio de pedidos para consumo en el restaurante, para recoger y a domicilio. Al usar la app aceptas estos términos.',
          ],
        },
        {
          titulo: '2. Cuenta de usuario',
          parrafos: [
            'Eres responsable de mantener la confidencialidad de tu cuenta y contraseña, y de toda actividad realizada desde ella.',
          ],
        },
        {
          titulo: '3. Pedidos y precios',
          parrafos: [
            'Los precios mostrados incluyen los impuestos aplicables. El total definitivo se congela al confirmar el pedido y puede consultarse en el detalle.',
          ],
        },
        {
          titulo: '4. Pagos',
          parrafos: [
            'Los métodos de pago guardados solo almacenan datos de referencia (tipo, alias y últimos cuatro dígitos). No almacenamos números completos de tarjeta ni códigos de seguridad.',
          ],
        },
        {
          titulo: '5. Cancelaciones',
          parrafos: [
            'Una vez confirmado, un pedido queda bloqueado para edición. Las cancelaciones se solicitan y quedan sujetas a aprobación del restaurante.',
          ],
        },
      ]}
    />
  );
}
