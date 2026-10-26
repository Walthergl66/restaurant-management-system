package com.restaurante.archivos.domain;

import com.restaurante.archivos.TipoArchivo;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "archivos", name = "archivos")
public class Archivo {
    @Id private UUID id;
    @Column(name="nombre_original", nullable=false, length=255) private String nombreOriginal;
    @Column(nullable=false, length=100) private String clave;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=10) private TipoArchivo tipo;
    @Column(name="contenido_tipo", nullable=false, length=100) private String contenidoTipo;
    @Column(nullable=false) private long tamano;
    @Column(nullable=false, length=255) private String propietario;
    @Column(name="producto_id") private Long productoId;
    @Column(nullable=false, length=12) private String estado = "PENDIENTE";
    @Column(name="creado_en", nullable=false) private Instant creadoEn = Instant.now();
    @Column(name="modificado_en", nullable=false) private Instant modificadoEn = Instant.now();
    @Version private long version;

    protected Archivo() {}
    public Archivo(UUID id, String nombre, String clave, TipoArchivo tipo, String mime, long tamano, String propietario) {
        this.id=id; this.nombreOriginal=nombre; this.clave=clave; this.tipo=tipo;
        this.contenidoTipo=mime; this.tamano=tamano; this.propietario=propietario;
    }
    public void vincular(TipoArchivo esperado, Long producto, String usuario) {
        if (tipo != esperado || "RETIRADO".equals(estado)) throw new BusinessRuleException("El archivo no es válido para este campo");
        if ("VINCULADO".equals(estado)) {
            if (!producto.equals(productoId)) throw new BusinessRuleException("El archivo ya pertenece a otro producto");
            return;
        }
        if (!propietario.equals(usuario)) throw new BusinessRuleException("El archivo no pertenece a esta sesión de usuario");
        productoId=producto; estado="VINCULADO"; modificadoEn=Instant.now();
    }
    public void retirar(Long producto) {
        if ("VINCULADO".equals(estado) && producto.equals(productoId)) {
            estado="RETIRADO"; modificadoEn=Instant.now();
        }
    }
    public UUID getId() { return id; }
    public String getNombreOriginal() { return nombreOriginal; }
    public String getClave() { return clave; }
    public TipoArchivo getTipo() { return tipo; }
    public String getContenidoTipo() { return contenidoTipo; }
    public long getTamano() { return tamano; }
    public String getPropietario() { return propietario; }
    public String getEstado() { return estado; }
}
