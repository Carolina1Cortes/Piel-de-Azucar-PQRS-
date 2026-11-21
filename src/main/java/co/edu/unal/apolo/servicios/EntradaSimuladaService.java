package co.edu.unal.apolo.servicios;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import co.edu.unal.apolo.integracion.CanalMensajeria;
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

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ClienteRepository clienteRepository;
    private final TipoConsultaRepository tipoConsultaRepository;
    private final PqrsRepository pqrsRepository;
    private final MensajeRepository mensajeRepository;
    private final DiasHabilesService diasHabiles;
    private final Clock reloj;
    private final CanalMensajeria canal;

    public EntradaSimuladaService(ClienteRepository clienteRepository,
                                  TipoConsultaRepository tipoConsultaRepository,
                                  PqrsRepository pqrsRepository,
                                  MensajeRepository mensajeRepository,
                                  DiasHabilesService diasHabiles,
                                  Clock reloj,
                                  CanalMensajeria canal) {
        this.clienteRepository = clienteRepository;
        this.tipoConsultaRepository = tipoConsultaRepository;
        this.pqrsRepository = pqrsRepository;
        this.mensajeRepository = mensajeRepository;
        this.diasHabiles = diasHabiles;
        this.reloj = reloj;
        this.canal = canal;
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
        pqrs.setRadicado(generarRadicado(ahora.getYear(), pqrs.getId()));

        Mensaje entrante = new Mensaje();
        entrante.setPqrs(pqrs);
        entrante.setFecha(ahora);
        entrante.setAsunto(pqrs.getAsunto());
        entrante.setCuerpo(cuerpo.strip());
        entrante.setDireccion(DireccionMensaje.ENTRANTE);
        entrante.setAutomatico(false);
        mensajeRepository.save(entrante);

        // Acuse de recibo, no respuesta de contenido: se envía también en casos críticos sin asignar.
        Mensaje confirmacion = new Mensaje();
        confirmacion.setPqrs(pqrs);
        confirmacion.setFecha(ahora);
        confirmacion.setAsunto("Recibimos tu solicitud - Radicado " + pqrs.getRadicado());
        confirmacion.setCuerpo("Hola " + cliente.getNombre() + ", recibimos tu solicitud con radicado "
                + pqrs.getRadicado() + ". La atenderemos a más tardar el "
                + pqrs.getFechaLimite().format(FORMATO_FECHA) + ". Guarda este número para cualquier consulta.");
        confirmacion.setDireccion(DireccionMensaje.SALIENTE);
        confirmacion.setAutomatico(true);
        confirmacion.setAutor(null);
        mensajeRepository.save(confirmacion);
        canal.enviar(confirmacion);

        return pqrs;
    }

    /** Radicado visible para la clienta: PQRS-AAAA-NNNNNN (año de llegada e id con ceros a la izquierda). */
    static String generarRadicado(int anio, long id) {
        return String.format("PQRS-%d-%06d", anio, id);
    }

    private void exigirTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException(mensaje);
        }
    }
}
