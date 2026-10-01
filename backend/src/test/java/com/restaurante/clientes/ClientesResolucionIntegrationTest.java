package com.restaurante.clientes;

import com.restaurante.AbstractIntegracionApi;
import com.restaurante.clientes.domain.Cliente;
import com.restaurante.clientes.infrastructure.ClienteRepository;
import com.restaurante.usuarios.Usuarios;
import com.restaurante.usuarios.infrastructure.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La creación on-the-fly del cliente (RF-42) es un comprobar-crear sobre la
 * clave única {@code usuario_id}. Sin serializar, dos peticiones concurrentes
 * del mismo usuario pasan a la vez el "no existe cliente", ambas insertan y
 * una revienta con violación de unicidad: un 500 en el checkout del cliente.
 * Resolverlo con la fila del usuario bloqueada hace que la segunda peticion
 * espere y luego reutilice el cliente ya creado.
 */
class ClientesResolucionIntegrationTest extends AbstractIntegracionApi {

    @Autowired
    private Clientes clientes;

    @Autowired
    private Usuarios usuarios;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void laResolucionConcurrenteEsperaAlLockYReutilizaElClienteCreado() throws Exception {
        String username = asegurarUsuarioCliente();
        Long usuarioId = usuarioRepository.findByUsername(username).orElseThrow().getId();
        assertTrue(clienteRepository.findByUsuarioId(usuarioId).isEmpty(),
                "el usuario de prueba no debe tener cliente todavia");

        CountDownLatch insertado = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        AtomicLong idEnCurso = new AtomicLong();

        // Otra peticion ya esta creando el cliente del mismo usuario y mantiene
        // la transaccion abierta (y el lock) hasta que se lo permitamos.
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Long> enCurso = pool.submit(() -> new TransactionTemplate(transactionManager)
                    .execute(tx -> {
                        usuarios.porUsernameBloqueado(username).orElseThrow();
                        long creado = clienteRepository.saveAndFlush(new Cliente(
                                usuarioId,
                                "CC-" + System.nanoTime(),
                                "09" + System.nanoTime() % 100000000L,
                                "Cliente en curso")).getId();
                        idEnCurso.set(creado);
                        insertado.countDown();
                        await(liberar);
                        return creado;
                    }));

            assertTrue(insertado.await(10, TimeUnit.SECONDS), "no se inserto el cliente en curso");

            // La resolucion concurrente debe esperar al lock, no duplicar
            Future<Long> concurrente = pool.submit(() -> clientes.resolverClienteId(username));
            assertThrows(TimeoutException.class,
                    () -> concurrente.get(1, TimeUnit.SECONDS),
                    "resolverClienteId no respeta el lock del usuario");

            liberar.countDown();
            assertEquals(idEnCurso.get(), concurrente.get(15, TimeUnit.SECONDS),
                    "debe reutilizar el cliente ya creado");
            enCurso.get(15, TimeUnit.SECONDS);
            assertEquals(1, clienteRepository.findByUsuarioId(usuarioId).stream().count(),
                    "no debe quedar mas de un cliente para el usuario");
        } finally {
            liberar.countDown();
            pool.shutdownNow();
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("no se recibio la senal de liberacion");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private String asegurarUsuarioCliente() throws Exception {
        String username = "cli_resol_" + System.nanoTime();
        String password = "claveResol123";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"%s","password":"%s","rolCodigo":"CLIENTE"}
                                """.formatted(username, username, password)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isCreated());
        return username;
    }
}