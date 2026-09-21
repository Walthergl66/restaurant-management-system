package com.restaurante.catalogo.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.infrastructure.MoneyConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Producto del catálogo. El precio se guarda congelado aquí; los pedidos
 * congelan su propia copia al confirmar (regla 3 del negocio).
 */
@Entity
@Table(name = "productos")
public class Producto extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "imagen_url", length = 300)
    private String imagenUrl;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false, precision = 12, scale = 2)
    private Money precio;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private Area area;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "producto_extras",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "extra_id"))
    private Set<Extra> extras = new LinkedHashSet<>();

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IngredienteRemovible> ingredientes = new ArrayList<>();

    protected Producto() {
    }

    public Producto(String nombre, Money precio) {
        cambiarNombre(nombre);
        cambiarPrecio(precio);
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public Money getPrecio() {
        return precio;
    }

    public boolean isActivo() {
        return activo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Area getArea() {
        return area;
    }

    public Set<Extra> getExtras() {
        return extras;
    }

    public List<IngredienteRemovible> getIngredientes() {
        return ingredientes;
    }

    public void cambiarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessRuleException("El nombre del producto no puede estar vacío");
        }
        this.nombre = nombre.trim();
    }

    public void cambiarDescripcion(String descripcion) {
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
    }

    public void cambiarImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl == null || imagenUrl.isBlank() ? null : imagenUrl.trim();
    }

    public void cambiarPrecio(Money precio) {
        if (precio == null || precio.isNegative()) {
            throw new BusinessRuleException("El precio del producto no puede ser negativo");
        }
        this.precio = precio;
    }

    public void cambiarCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public void cambiarArea(Area area) {
        this.area = area;
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    /**
     * Reemplaza el conjunto de extras asociados. Los extras del catálogo no
     * se borran si dejan de estar asociados a este producto.
     */
    public void reemplazarExtras(Set<Extra> extras) {
        this.extras = new LinkedHashSet<>(extras);
    }

    /**
     * Reemplaza la lista de ingredientes removibles por nuevos registros.
     */
    public void reemplazarIngredientes(List<String> nombres) {
        this.ingredientes.clear();
        if (nombres != null) {
            nombres.stream()
                    .filter(n -> n != null && !n.isBlank())
                    .map(n -> new IngredienteRemovible(this, n))
                    .forEach(this.ingredientes::add);
        }
    }
}