package com.restaurante.configuracion.application;

import com.restaurante.configuracion.Parametros;
import com.restaurante.configuracion.domain.Parametro;
import com.restaurante.configuracion.domain.TipoParametro;
import com.restaurante.configuracion.infrastructure.ParametroRepository;
import com.restaurante.configuracion.web.dto.ParametroResponse;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Lectura y administración de los parámetros del restaurante.
 * Implementa {@link Parametros}, la API pública que consumen otros módulos.
 */
@Service
@Transactional
public class ParametroService implements Parametros {

    private static final String CLAVE_IVA = "impuestos.iva";
    private static final String CLAVE_NOMBRE = "restaurant.nombre";
    private static final String CLAVE_MONEDA = "moneda";

    private final ParametroRepository parametroRepository;

    @Value("${app.iva.rate:0.15}")
    private BigDecimal ivaConfiguracion;

    public ParametroService(ParametroRepository parametroRepository) {
        this.parametroRepository = parametroRepository;
    }

    @Transactional(readOnly = true)
    public List<ParametroResponse> listar() {
        return parametroRepository.findAll().stream().map(ParametroResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ParametroResponse obtenerPorClave(String clave) {
        return parametroRepository.findByClave(clave)
                .map(ParametroResponse::from)
                .orElseThrow(() -> new NotFoundException("Parámetro no encontrado: " + clave));
    }

    public ParametroResponse actualizar(String clave, String valor) {
        Parametro parametro = parametroRepository.findByClave(clave)
                .orElseThrow(() -> new NotFoundException("Parámetro no encontrado: " + clave));
        parametro.cambiarValor(valor);
        return ParametroResponse.from(parametro);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> obtener(String clave) {
        return parametroRepository.findByClave(clave).map(Parametro::getValor);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal tasaIva() {
        Optional<BigDecimal> valor = parametroRepository.findByClave(CLAVE_IVA)
                .filter(p -> p.getTipo() == TipoParametro.NUMERICO)
                .map(Parametro::valorComoBigDecimal);
        return valor.orElse(ivaConfiguracion);
    }

    @Override
    @Transactional(readOnly = true)
    public String nombreRestaurante() {
        return obtener(CLAVE_NOMBRE).filter(v -> !v.isBlank()).orElse("Restaurante");
    }

    @Override
    @Transactional(readOnly = true)
    public String moneda() {
        return obtener(CLAVE_MONEDA).filter(v -> !v.isBlank()).orElse("USD");
    }
}