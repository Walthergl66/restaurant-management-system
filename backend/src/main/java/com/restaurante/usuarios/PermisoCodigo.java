package com.restaurante.usuarios;

import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Códigos de permisos del sistema (columna {@code permisos.codigo}).
 * Las constantes se usan en {@code @PreAuthorize} con {@code hasAuthority}.
 *
 * <p>Convención: {@code <modulo>:<accion>}. Este tipo permanece en el paquete
 * raíz del módulo para que otros módulos puedan usarlo sin violar el
 * encapsulamiento que exige Spring Modulith.
 */
public final class PermisoCodigo {

    private PermisoCodigo() {
    }

    // usuarios
    public static final String USUARIOS_VER = "usuarios:ver";
    public static final String USUARIOS_CREAR = "usuarios:crear";
    public static final String USUARIOS_EDITAR = "usuarios:editar";
    public static final String USUARIOS_ELIMINAR = "usuarios:eliminar";
    public static final String ROLES_ASIGNAR = "roles:asignar";

    // catalogo
    public static final String CATALOGO_VER = "catalogo:ver";
    public static final String CATALOGO_GESTIONAR = "catalogo:gestionar";

    // mesas
    public static final String MESAS_VER = "mesas:ver";
    public static final String MESAS_GESTIONAR = "mesas:gestionar";

    // pedidos
    public static final String PEDIDOS_VER = "pedidos:ver";
    public static final String PEDIDOS_CREAR = "pedidos:crear";
    public static final String PEDIDOS_EDITAR = "pedidos:editar";
    public static final String PEDIDOS_CONFIRMAR = "pedidos:confirmar";
    public static final String PEDIDOS_ESTADO_PREPARACION = "pedidos:estado-preparacion";
    public static final String PEDIDOS_ESTADO_LISTO = "pedidos:estado-listo";

    // comandas
    public static final String COMANDAS_VER = "comandas:ver";
    public static final String COMANDAS_REENVIAR = "comandas:reenviar";

    // anulaciones
    public static final String ANULACIONES_SOLICITAR = "anulaciones:solicitar";
    public static final String ANULACIONES_APROBAR = "anulaciones:aprobar";

    // cuentas
    public static final String CUENTAS_VER = "cuentas:ver";
    public static final String CUENTAS_GESTIONAR = "cuentas:gestionar";

    // pagos / facturación
    public static final String PAGOS_COBRAR = "pagos:cobrar";
    public static final String FACTURACION_EMITIR = "facturacion:emitir";

    // caja
    public static final String CAJA_APERTURA = "caja:apertura";
    public static final String CAJA_MOVIMIENTOS = "caja:movimientos";
    public static final String CAJA_CIERRE = "caja:cierre";

    // finanzas / reportes / auditoría / clientes / configuración
    public static final String FINANZAS_VER = "finanzas:ver";
    public static final String REPORTES_VER = "reportes:ver";
    public static final String AUDITORIA_VER = "auditoria:ver";
    public static final String CLIENTES_GESTIONAR = "clientes:gestionar";
    public static final String CLIENTES_MENU_VER = "clientes:menu-ver";
    public static final String CLIENTES_CARRITO_GESTIONAR = "clientes:carrito-gestionar";
    public static final String CLIENTES_PEDIDO_CREAR = "clientes:pedido-crear";
    public static final String CLIENTES_PEDIDO_CONFIRMAR = "clientes:pedido-confirmar";
    public static final String CLIENTES_PEDIDO_ESTADO_VER = "clientes:pedido-estado-ver";
    public static final String CLIENTES_HISTORIAL_VER = "clientes:historial-ver";
    public static final String CLIENTES_CUENTA_NUEVA = "clientes:cuenta-nueva";
    public static final String CONFIGURACION_GESTIONAR = "configuracion:gestionar";

    /**
     * Todos los códigos de permiso del sistema, en orden de definición.
     */
    public static Set<String> todos() {
        Set<String> codigos = new LinkedHashSet<>();
        try (Stream<java.lang.reflect.Field> fields = Stream.of(PermisoCodigo.class.getDeclaredFields())) {
            fields.filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
                    .forEach(f -> {
                        try {
                            codigos.add((String) f.get(null));
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    });
        }
        return codigos;
    }

    /**
     * Códigos del módulo indicado (antes de los dos puntos).
     */
    public static Set<String> deModulo(String modulo) {
        return todos().stream().filter(c -> c.startsWith(modulo + ":")).collect(Collectors.toSet());
    }
}