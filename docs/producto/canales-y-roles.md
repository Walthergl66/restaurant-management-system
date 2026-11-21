# Canales y roles

Quién hace qué y en qué interfaz. Describe lo que el sistema hace **hoy**; lo que aún no existe se marca como
pendiente. Los permisos exactos están en la [API](../api/README.md).

## Dos canales

- **Web** ([`frontend`](../../frontend/README.md)): solo **administrador** y **cajero**. Una cuenta de otro rol que
  inicia sesión en la web recibe un aviso y se cierra su sesión.
- **Aplicación móvil** ([`mobile`](../../mobile/README.md)): cliente, mesero, cocina y repartidor. Al iniciar
  sesión, cada usuario entra a su pantalla según sus permisos.

## Matriz de roles

| Rol | Interfaz | Qué hace |
|---|---|---|
| **Administrador** | Web | Usuarios y roles, productos y catálogo, **mesas y zonas**, reportes, finanzas, auditoría, aprobación de anulaciones, cuentas y caja, **asignación de repartidores** y entregas, monitor de pedidos y de comandas (con reimpresión). |
| **Cajero** | Web | Cobro de cuentas, caja (apertura, egresos y cierre), comprobantes, reportes y finanzas, aprobación de anulaciones, consulta de pedidos y comandas. |
| **Mesero** | Móvil | Elige la mesa por zona y toma el pedido; pestaña **Cocina** (marca las comandas como la cocina); pestaña **Listos** (entrega a la mesa los pedidos servidos y entrega los pedidos de retiro). |
| **Cocina** (rol `COCINA`) | Móvil | **Comandas** (preparar, listo, enterado de cancelaciones, filtro por área) y **Mostrador** (entrega los pedidos de retiro). |
| **Repartidor** | Móvil | Sus entregas asignadas, recorridos y rutas, y la confirmación de entrega. Se registra por API y lo habilita el administrador. |
| **Cliente** | Móvil | Menú, pedidos para **retirar** o a **domicilio**, seguimiento, historial, direcciones y métodos de pago. |

La cocina la pueden hacer el rol `COCINA` y el `MESERO`: ambos tienen los permisos de operación de comandas y de
entrega de retiro. Solo el mesero toma pedidos y entrega a las mesas.

## Cómo se cruzan en una jornada

**Pedido en el salón.** El mesero lo toma en la app (mesa por zona) → llega como comandas por área y se imprime →
cocina o mesero marcan preparación y listo → el mesero lo entrega a la mesa (*Listos*) → el cajero cobra en la web,
la cuenta se cierra y la mesa vuelve a *Libre*.

**Pedido de la app para retirar.** El cliente lo pide → llega a cocina → cocina o mesero lo dejan listo → se entrega
en el mostrador a quien lo pidió (sin PIN).

**Pedido de la app a domicilio.** El cliente lo pide → cocina lo deja listo → el administrador lo **asigna** a un
repartidor (web, *Entregas*) → el repartidor lo entrega y se confirma por ambas partes (repartidor y cliente).

## Pendiente

- **Domicilio con despacho y PIN:** previsto que cocina o mesero lo marquen "enviado para entrega", que el repartidor
  lo tome y que el cliente le dé un PIN de 4 dígitos para confirmar la entrega. Aún no está implementado.
- **Cobro de los pedidos de la app:** el pago de un pedido de retiro o domicilio no queda registrado en caja ni
  en los reportes. Pendiente de decisión.
- **Cierre de cuentas sin cobro** y **anulaciones posteriores al cobro:** reglas pendientes de definir.
- **Actualización automática de estados** por tiempo estimado, para no depender de que alguien los marque a mano.
- **Alta del repartidor desde la app** y su habilitación visible para el administrador.
