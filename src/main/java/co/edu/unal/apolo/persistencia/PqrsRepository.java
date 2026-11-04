package co.edu.unal.apolo.persistencia;

import co.edu.unal.apolo.dominio.EstadoPqrs;
import co.edu.unal.apolo.dominio.Pqrs;
import co.edu.unal.apolo.dominio.Prioridad;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a datos de Pqrs. Spring Data genera la implementación. */
public interface PqrsRepository extends JpaRepository<Pqrs, Long> {

    Optional<Pqrs> findByIdExterno(String idExterno);

    boolean existsByIdExterno(String idExterno);

    List<Pqrs> findByEstado(EstadoPqrs estado);

    List<Pqrs> findByPrioridad(Prioridad prioridad);

    List<Pqrs> findByResponsableId(Long usuarioId);

    /** PQRS sin resolver cuya fecha límite ya pasó respecto de la fecha dada (estado distinto de RESUELTA). */
    List<Pqrs> findByEstadoNotAndFechaLimiteBefore(EstadoPqrs estado, LocalDate fecha);

    /** Todas las PQRS con cliente, tipo de consulta y responsable cargados, ordenadas por fecha límite. */
    @Query("""
            select p from Pqrs p
            join fetch p.cliente
            join fetch p.tipoConsulta
            left join fetch p.responsable
            order by p.fechaLimite asc, p.id asc
            """)
    List<Pqrs> findAllConDetalle();

    /** Una PQRS con cliente, tipo de consulta y responsable cargados (se usa fuera de la transacción en las vistas). */
    @Query("""
            select p from Pqrs p
            join fetch p.cliente
            join fetch p.tipoConsulta
            left join fetch p.responsable
            where p.id = :id
            """)
    Optional<Pqrs> findByIdConDetalle(@Param("id") Long id);

    /** Las PQRS de una prioridad con el mismo detalle de findAllConDetalle, ordenadas por fecha límite. */
    @Query("""
            select p from Pqrs p
            join fetch p.cliente
            join fetch p.tipoConsulta
            left join fetch p.responsable
            where p.prioridad = :prioridad
            order by p.fechaLimite asc, p.id asc
            """)
    List<Pqrs> findByPrioridadConDetalle(@Param("prioridad") Prioridad prioridad);
}
