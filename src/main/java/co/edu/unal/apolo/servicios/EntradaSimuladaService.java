package co.edu.unal.apolo.servicios;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unal.apolo.dominio.Canal;
import co.edu.unal.apolo.dominio.Cliente;
import co.edu.unal.apolo.dominio.DireccionMensaje;
import co.edu.unal.apolo.dominio.EstadoPqrs;
import co.edu.unal.apolo.dominio.Mensaje;
import co.edu.unal.apolo.dominio.Pqrs;
import co.edu.unal.apolo.dominio.TipoConsulta;
import co.edu.unal.apolo.persistencia.ClienteRepository;
import co.edu.unal.apolo.persistencia.MensajeRepository;
import co.edu.unal.apolo.persistencia.PqrsRepository;
import co.edu.unal.apolo.persistencia.TipoConsultaRepository;

/**
 * Entrada simulada de correos (CU-01 sin internet): crea una PQRS nueva con su clienta, su mensaje entrante,
 * canal Gmail, la prioridad por defecto del tipo de consulta y la fecha límite en días hábiles.
 */
@Service
public class EntradaSimuladaService {

    private final ClienteRepository clienteRepository;
    private final TipoConsultaRepository tipoConsultaRepository;
    private final PqrsRepository pqrsRepository;
    private final MensajeRepository mensajeRepository;
    private final DiasHabilesService diasHabiles;
    private final Clock reloj;

    public EntradaSimuladaService(ClienteRepository clienteRepository,
                                  TipoConsultaRepository tipoConsultaRepository,
                                  PqrsRepository pqrsRepository,
                                  MensajeRepository mensajeRepository,
                                  DiasHabilesService diasHabiles,
                                  Clock reloj) {
        this.clienteRepository = clienteRepository;
        this.tipoConsultaRepository = tipoConsultaRepository;
        this.pqrsRepository = pqrsRepository;
        this.mensajeRepository = mensajeRepository;
        this.diasHabiles = diasHabiles;
        this.reloj = reloj;
    }

    /** Tipos de consulta disponibles para clasificar el correo simulado. */
    @Transactional(readOnly = true)
    public List<TipoConsulta> listarTiposConsulta() {
        return tipoConsultaRepository.findAll(Sort.by("id"));
    }

    @Transactional
    public Pqrs registrarCorreoEntrante(String nombreClienta, String correoClienta, String asunto, String cuerpo,
                                        Long tipoConsultaId) {
        exigirTexto(nombreClienta, "El nombre de la clienta es obligatorio");
        exigirTexto(correoClienta, "El correo de la clienta es obligatorio");
        exigirTexto(asunto, "El asunto es obligatorio");
        exigirTexto(cuerpo, "El mensaje es obligatorio");
        if (tipoConsultaId == null) {
            throw new ReglaNegocioException("Debe elegir un tipo de consulta");
        }
        TipoConsulta tipo = tipoConsultaRepository.findById(tipoConsultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de consulta", tipoConsultaId));

        String correo = correoClienta.strip().toLowerCase();
        Cliente cliente = clienteRepository.findByCorreo(correo).orElseGet(() -> {
            Cliente nueva = new Cliente();
            nueva.setNombre(nombreClienta.strip());
            nueva.setCorreo(correo);
            clienteRepository.save(nueva);
            return nueva;
        });

        LocalDateTime ahora = LocalDateTime.now(reloj);
        Pqrs pqrs = new Pqrs();
        pqrs.setIdExterno("simulado-" + UUID.randomUUID());
        pqrs.setCanal(Canal.GMAIL);
        pqrs.setFechaLlegada(ahora);
        pqrs.setAsunto(asunto.strip());
        pqrs.setEstado(EstadoPqrs.PENDIENTE);
        pqrs.setPrioridad(tipo.getPrioridadPorDefecto());
        pqrs.setFechaLimite(diasHabiles.calcularFechaLimite(ahora.toLocalDate()));
        pqrs.setCliente(cliente);
        pqrs.setTipoConsulta(tipo);
        pqrsRepository.save(pqrs);

        Mensaje entrante = new Mensaje();
        entrante.setPqrs(pqrs);
        entrante.setFecha(ahora);
        entrante.setAsunto(pqrs.getAsunto());
        entrante.setCuerpo(cuerpo.strip());
        entrante.setDireccion(DireccionMensaje.ENTRANTE);
        entrante.setAutomatico(false);
        mensajeRepository.save(entrante);

        return pqrs;
    }

    private void exigirTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException(mensaje);
        }
    }
}
