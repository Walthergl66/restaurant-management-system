/**
 * Estación Burger — Política de privacidad
 */

import React from 'react';
import { InfoPage } from '../components/InfoPage';

export default function PrivacidadScreen() {
  return (
    <InfoPage
      title="Política de privacidad"
      secciones={[
        {
          titulo: 'Datos que recopilamos',
          parrafos: [
            'Nombre, usuario y contraseña (almacenada de forma cifrada) para tu cuenta. Direcciones y métodos de pago de referencia para agilizar tus pedidos.',
          ],
        },
        {
          titulo: 'Cómo usamos tus datos',
          parrafos: [
            'Usamos tus datos para procesar pedidos, gestionar entregas a domicilio y notificarte el estado de tus pedidos. No vendemos tu información a terceros.',
          ],
        },
        {
          titulo: 'Seguridad',
          parrafos: [
            'Las contraseñas se almacenan con hash y las sesiones se protegen con tokens. Los códigos de recuperación son de un solo uso y expiran.',
          ],
        },
        {
          titulo: 'Tus derechos',
          parrafos: [
            'Puedes actualizar tus datos o solicitar la eliminación de tu cuenta escribiendo a soporte@estacionburger.app.',
          ],
        },
      ]}
    />
  );
}
