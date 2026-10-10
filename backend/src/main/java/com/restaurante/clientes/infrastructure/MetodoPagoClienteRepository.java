package com.restaurante.clientes.infrastructure;

import com.restaurante.clientes.domain.MetodoPagoCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetodoPagoClienteRepository extends JpaRepository<MetodoPagoCliente, Long> {

    List<MetodoPagoCliente> findByClienteIdAndActivoTrue(Long clienteId);

    Optional<MetodoPagoCliente> findByIdAndClienteId(Long id, Long clienteId);
}
