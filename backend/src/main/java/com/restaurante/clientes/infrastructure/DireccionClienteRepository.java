package com.restaurante.clientes.infrastructure;

import com.restaurante.clientes.domain.DireccionCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DireccionClienteRepository extends JpaRepository<DireccionCliente, Long> {
    List<DireccionCliente> findByClienteId(Long clienteId);
    List<DireccionCliente> findByClienteIdAndActivaTrue(Long clienteId);
}
