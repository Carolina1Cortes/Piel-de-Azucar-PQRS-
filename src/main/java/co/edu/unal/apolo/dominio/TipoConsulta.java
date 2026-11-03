package co.edu.unal.apolo.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_consulta")
public class TipoConsulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Prioridad prioridadPorDefecto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAutomatizacion automatizacion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Prioridad getPrioridadPorDefecto() {
        return prioridadPorDefecto;
    }

    public void setPrioridadPorDefecto(Prioridad prioridadPorDefecto) {
        this.prioridadPorDefecto = prioridadPorDefecto;
    }

    public TipoAutomatizacion getAutomatizacion() {
        return automatizacion;
    }

    public void setAutomatizacion(TipoAutomatizacion automatizacion) {
        this.automatizacion = automatizacion;
    }
}
