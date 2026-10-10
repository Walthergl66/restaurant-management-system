/**
 * Estación Burger — Ayuda
 */

import React from 'react';
import { InfoPage } from '../components/InfoPage';

export default function AyudaScreen() {
  return (
    <InfoPage
      title="Ayuda"
      subtitulo="Preguntas frecuentes y contacto de soporte."
      secciones={[
        {
          titulo: '¿Cómo hago un pedido?',
          parrafos: [
            'Explora el menú, agrega productos al carrito y toca "Proceder al pago". Elige tu método de pago y de entrega (recoger o domicilio) y confirma.',
          ],
        },
        {
          titulo: '¿Cómo sigo mi pedido?',
          parrafos: [
            'En la pestaña "Pedidos" verás tu historial. Toca un pedido para ver su estado en tiempo real (confirmado, en preparación, listo y entregado).',
          ],
        },
        {
          titulo: '¿Puedo pagar en efectivo?',
          parrafos: [
            'Sí. Al confirmar puedes elegir EFECTIVO o TARJETA. El pago se registra al momento de la entrega o el cobro.',
          ],
        },
        {
          titulo: '¿Olvidé mi contraseña?',
          parrafos: [
            'En la pantalla de inicio de sesión toca "¿Olvidé mi contraseña?" e ingresa tu usuario para recibir un código de recuperación.',
          ],
        },
        {
          titulo: 'Contacto',
          parrafos: [
            'Escríbenos a soporte@estacionburger.app o llama al 555 000 0000.',
          ],
        },
      ]}
    />
  );
}
