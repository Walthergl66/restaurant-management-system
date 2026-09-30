package com.restaurante.comandas;

import com.restaurante.AbstractIntegrationTest;
import com.restaurante.comandas.domain.Comanda;
import com.restaurante.catalogo.domain.Area;
import com.restaurante.catalogo.infrastructure.AreaRepository;
import com.restaurante.comandas.infrastructure.ComandaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Dos terminales de cocina pueden mover la MISMA comanda a la vez (una la da
 * por lista mientras la otra la vuelve a tocar). Sin versionado, la segunda
 * escritura pisa a la primera sin avisar y la comanda retrocede: el tablero
 * pierde un plato que ya estaba listo.
 *
 * El test es determinista: obliga a que las dos transacciones lean la comanda
 * antes de que ninguna escriba, de modo que siempre compiten por la misma
 * versión. Una gana y la otra debe ser rechazada por versión; sin @Version,
 * las dos ganarían y el retroceso pasaría desapercibido.
 */
class ComandaBloqueoOptimistaTest extends AbstractIntegrationTest {

    @Autowired
    private ComandaRepository comandaRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private TransactionTemplate transacciones;

    @Test
    void dosEscriturasSimultaneasSobreLaMismaComandaNoSePisan() throws Exception {
        long comandaId = crearComandaEnPreparacion();

        CountDownLatch ambasCargadas = new CountDownLatch(2);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger conflictos = new AtomicInteger();

        Runnable marcarListo = () -> {
            try {
                transacciones.executeWithoutResult(estado -> {
                    Comanda comanda = comandaRepository.findById(comandaId).orElseThrow();
                    ambasCargadas.countDown();
                    esperar(ambasCargadas);
                    comanda.marcarListo();
                });
                exitos.incrementAndGet();
            } catch (OptimisticLockingFailureException e) {
                conflictos.incrementAndGet();
            }
        };

        Thread primera = new Thread(marcarListo);
        Thread segunda = new Thread(marcarListo);
        primera.start();
        segunda.start();
        primera.join(30_000);
        segunda.join(30_000);

        assertEquals(1, exitos.get(), "Solo una de las dos escrituras puede ganar");
        assertEquals(1, conflictos.get(), "La otra debe ser rechazada por versión");
    }

    private long crearComandaEnPreparacion() {
        return transacciones.execute(estado -> {
            Area area = areaRepository.save(new Area("Cocina " + System.nanoTime()));
            Comanda comanda = new Comanda("PED-LOCK-" + System.nanoTime(), 1, area.getId(), area.getNombre());
            comanda.marcarEnPreparacion();
            return comandaRepository.save(comanda).getId();
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
