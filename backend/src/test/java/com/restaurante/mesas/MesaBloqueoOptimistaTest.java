package com.restaurante.mesas;

import com.restaurante.AbstractIntegrationTest;
import com.restaurante.mesas.domain.Mesa;
import com.restaurante.mesas.infrastructure.MesaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El estado de la mesa se muta desde varias rutas: el cajero la libera al
 * cobrar mientras el mesero la ocupa al abrir un pedido. Sin versionado, la
 * segunda escritura pisa a la primera sin avisar y el estado final es el de la
 * última en commitear, no el que la regla de negocio habría aceptado.
 *
 * El test es determinista: obliga a que las dos transacciones lean la mesa
 * antes de que ninguna escriba, de modo que siempre compiten por la misma
 * versión. Se parte de una mesa LIBRE y se aplican dos transiciones válidas
 * desde ese estado (ocupar y reservar); una gana y la otra debe ser rechazada
 * por versión. Sin @Version, las dos ganarían.
 */
class MesaBloqueoOptimistaTest extends AbstractIntegrationTest {

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private TransactionTemplate transacciones;

    @Test
    void dosEscriturasSimultaneasSobreLaMismaMesaNoSePisan() throws Exception {
        long mesaId = crearMesaLibre();

        CountDownLatch ambasCargadas = new CountDownLatch(2);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger conflictos = new AtomicInteger();

        Runnable ocupar = () -> ejecutar(mesaId, Mesa::ocupar, ambasCargadas, exitos, conflictos);
        Runnable reservar = () -> ejecutar(mesaId, Mesa::reservar, ambasCargadas, exitos, conflictos);

        Thread primera = new Thread(ocupar);
        Thread segunda = new Thread(reservar);
        primera.start();
        segunda.start();
        primera.join(30_000);
        segunda.join(30_000);

        assertEquals(1, exitos.get(), "Solo una de las dos escrituras puede ganar");
        assertEquals(1, conflictos.get(), "La otra debe ser rechazada por versión");
    }

    private void ejecutar(long mesaId, Consumer<Mesa> cambio, CountDownLatch ambasCargadas,
            AtomicInteger exitos, AtomicInteger conflictos) {
        try {
            transacciones.executeWithoutResult(estado -> {
                Mesa mesa = mesaRepository.findById(mesaId).orElseThrow();
                ambasCargadas.countDown();
                esperar(ambasCargadas);
                cambio.accept(mesa);
            });
            exitos.incrementAndGet();
        } catch (OptimisticLockingFailureException e) {
            conflictos.incrementAndGet();
        }
    }

    private long crearMesaLibre() {
        return transacciones.execute(estado -> {
            Mesa mesa = new Mesa((int) (System.nanoTime() % 1_000_000_000), 4);
            return mesaRepository.save(mesa).getId();
        });
    }

    private void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Las transacciones no se encontraron a tiempo");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
