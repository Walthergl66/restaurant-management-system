package com.restaurante.clientes.infrastructure;

import com.restaurante.clientes.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByUsuarioId(Long usuarioId);
    Optional<Cliente> findByCedula(String cedula);
    Optional<Cliente> findByTelefono(String telefono);
}
