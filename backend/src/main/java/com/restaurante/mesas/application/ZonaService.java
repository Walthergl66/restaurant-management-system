package com.restaurante.mesas.application;

import com.restaurante.mesas.domain.Zona;
import com.restaurante.mesas.infrastructure.MesaRepository;
import com.restaurante.mesas.infrastructure.ZonaRepository;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Administración de las zonas del restaurante. El administrador las crea, renombra, ordena y
 * elimina; las mesas se ubican en una de ellas. Siempre debe existir al menos una zona.
 */
@Service
@Transactional
public class ZonaService {

    private final ZonaRepository zonaRepository;
    private final MesaRepository mesaRepository;

    public ZonaService(ZonaRepository zonaRepository, MesaRepository mesaRepository) {
        this.zonaRepository = zonaRepository;
        this.mesaRepository = mesaRepository;
    }

    @Transactional(readOnly = true)
    public List<ZonaVista> listar() {
        return zonaRepository.findAllByOrderByOrdenAscIdAsc().stream().map(this::vista).toList();
    }

    public ZonaVista crear(String nombre) {
        String limpio = Zona.normalizarNombre(nombre);
        validarDisponible(limpio, null);
        int ultimo = zonaRepository.findAll().stream().mapToInt(Zona::getOrden).max().orElse(0);
        return vista(zonaRepository.save(new Zona(limpio, ultimo + 1)));
    }

    public ZonaVista renombrar(Long id, String nombre) {
        Zona zona = cargar(id);
        String limpio = Zona.normalizarNombre(nombre);
        validarDisponible(limpio, id);
        zona.cambiarNombre(limpio);
        return vista(zona);
    }

    /** Guarda el orden completo: la lista debe contener todas las zonas, cada una una sola vez. */
    public List<ZonaVista> reordenar(List<Long> ids) {
        Map<Long, Zona> porId = zonaRepository.findAll().stream()
                .collect(Collectors.toMap(Zona::getId, Function.identity()));
        if (ids == null || ids.size() != porId.size() || new HashSet<>(ids).size() != ids.size()
                || !porId.keySet().containsAll(ids)) {
            throw new BusinessRuleException(
                    "La lista de zonas cambió. Actualiza y vuelve a ordenar todas las zonas.");
        }
        for (int i = 0; i < ids.size(); i++) {
            porId.get(ids.get(i)).cambiarOrden(i + 1);
        }
        return listar();
    }

    public void eliminar(Long id) {
        Zona zona = cargar(id);
        if (mesaRepository.countByZonaId(id) > 0) {
            throw new BusinessRuleException(
                    "No se puede eliminar una zona con mesas: mueve o elimina sus mesas primero");
        }
        if (zonaRepository.count() <= 1) {
            throw new BusinessRuleException("Debe existir al menos una zona");
        }
        zonaRepository.delete(zona);
        // Se mantienen consecutivos los órdenes de las que quedan.
        List<Zona> restantes = zonaRepository.findAllByOrderByOrdenAscIdAsc().stream()
                .filter(z -> !z.getId().equals(id)).toList();
        for (int i = 0; i < restantes.size(); i++) {
            restantes.get(i).cambiarOrden(i + 1);
        }
    }

    /** Zona por defecto: la primera según el orden. */
    @Transactional(readOnly = true)
    public Zona primera() {
        return zonaRepository.findAllByOrderByOrdenAscIdAsc().stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException("No hay zonas configuradas"));
    }

    @Transactional(readOnly = true)
    public Zona cargar(Long id) {
        return zonaRepository.findById(id).orElseThrow(() -> new NotFoundException("Zona no encontrada"));
    }

    private void validarDisponible(String nombre, Long idActual) {
        String clave = Zona.clave(nombre);
        boolean repetido = zonaRepository.findAll().stream()
                .anyMatch(z -> !z.getId().equals(idActual) && Zona.clave(z.getNombre()).equals(clave));
        if (repetido) {
            throw new ConflictException("Ya existe una zona llamada '" + nombre + "'");
        }
    }

    private ZonaVista vista(Zona zona) {
        return new ZonaVista(zona.getId(), zona.getNombre(), zona.getOrden(),
                mesaRepository.countByZonaId(zona.getId()));
    }
}
