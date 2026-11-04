package co.edu.unal.apolo.servicios;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unal.apolo.dominio.DireccionMensaje;
import co.edu.unal.apolo.dominio.EstadoPqrs;
import co.edu.unal.apolo.dominio.Mensaje;
import co.edu.unal.apolo.dominio.Pqrs;
import co.edu.unal.apolo.dominio.Prioridad;
import co.edu.unal.apolo.dominio.RegistroBitacora;
import co.edu.unal.apolo.dominio.Rol;
import co.edu.unal.apolo.dominio.TipoAccion;
import co.edu.unal.apolo.dominio.Usuario;
import co.edu.unal.apolo.integracion.CanalMensajeria;
import co.edu.unal.apolo.persistencia.BitacoraRepository;
import co.edu.unal.apolo.persistencia.MensajeRepository;
import co.edu.unal.apolo.persistencia.PqrsRepository;
import co.edu.unal.apolo.persistencia.UsuarioRepository;
import co.edu.unal.apolo.servicios.DetalleCaso.MensajeVista;
import co.edu.unal.apolo.servicios.DetalleCaso.RegistroVista;

/**
 * Casos de uso sobre PQRS: bandeja, detalle de un caso, tomar un caso (CU-03) y enviar una respuesta manual (CU-03).
 * Todas las acciones del operador quedan en la bitácora.
 */
@Service
public class PqrsService {

    private static final int LARGO_MAXIMO_ASUNTO = 300;

    private final PqrsRepository pqrsRepository;
    private final UsuarioRepository usuarioRepository;
    private final MensajeRepository mensajeRepository;
    private final BitacoraRepository bitacoraRepository;
    private final CanalMensajeria canal;
    private final DiasHabilesService diasHabiles;
    private final Clock reloj;

    public PqrsService(PqrsRepository pqrsRepository,
                       UsuarioRepository usuarioRepository,
                       MensajeRepository mensajeRepository,
                       BitacoraRepository bitacoraRepository,
                       CanalMensajeria canal,
                       DiasHabilesService diasHabiles,
                       Clock reloj) {
        this.pqrsRepository = pqrsRepository;
        this.usuarioRepository = usuarioRepository;
        this.mensajeRepository = mensajeRepository;
        this.bitacoraRepository = bitacoraRepository;
        this.canal = canal;
        this.diasHabiles = diasHabiles;
        this.reloj = reloj;
    }

    /** Bandeja: todas las PQRS o solo las críticas, ordenadas por fecha límite, con sus días hábiles restantes. */
    @Transactional(readOnly = true)
    public List<ResumenPqrs> listar(boolean soloCriticas) {
        List<Pqrs> casos = soloCriticas
                ? pqrsRepository.findByPrioridadConDetalle(Prioridad.CRITICA)
                : pqrsRepository.findAllConDetalle();
        LocalDate hoy = LocalDate.now(reloj);
        return casos.stream()
                .map(caso -> new ResumenPqrs(caso, diasHabiles.diasHabilesRestantes(hoy, caso.getFechaLimite())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Pqrs buscarPorId(Long id) {
        return pqrsRepository.findByIdConDetalle(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("PQRS", id));
    }

    /** Pantalla de un caso: conversación, bitácora y lo que el operador puede hacer sobre él. */
    @Transactional(readOnly = true)
    public DetalleCaso detalle(Long id, String correoUsuario) {
        Pqrs pqrs = buscarPorId(id);
        Usuario usuario = buscarUsuario(correoUsuario);

        List<MensajeVista> mensajes = mensajeRepository.findByPqrsIdOrderByFechaAsc(id).stream()
                .map(m -> new MensajeVista(m.getFecha(), m.getDireccion(), m.getAsunto(), m.getCuerpo(),
                        m.isAutomatico(), m.getAutor() == null ? null : m.getAutor().getNombre()))
                .toList();
        List<RegistroVista> bitacora = bitacoraRepository.findByPqrsIdOrderByFechaAsc(id).stream()
                .map(r -> new RegistroVista(r.getFecha(), r.getAccion(),
                        r.getUsuario() == null ? null : r.getUsuario().getNombre(), r.getMotivo(), r.getNotasInternas()))
                .toList();

        Optional<String> motivoRechazo = motivoRechazoRespuesta(pqrs, usuario);
        boolean puedeTomar = !usuario.isBloqueado()
                && pqrs.getEstado() == EstadoPqrs.PENDIENTE
                && pqrs.getResponsable() == null;
        int diasRestantes = diasHabiles.diasHabilesRestantes(LocalDate.now(reloj), pqrs.getFechaLimite());

        return new DetalleCaso(pqrs, mensajes, bitacora, diasRestantes, esCriticoSinAsignar(pqrs),
                puedeTomar, motivoRechazo.isEmpty(), motivoRechazo.orElse(null));
    }

    /**
     * El operador se autoasigna un caso pendiente: pasa a "En atención", queda como responsable
     * (lo que desbloquea el envío en los casos críticos) y se registra en la bitácora.
     */
    @Transactional
    public Pqrs tomarCaso(Long pqrsId, String correoUsuario) {
        Pqrs pqrs = buscarPorId(pqrsId);
        Usuario usuario = buscarUsuario(correoUsuario);
        if (usuario.isBloqueado()) {
            throw new ReglaNegocioException("El operador está bloqueado y no puede tomar casos");
        }
        if (pqrs.getEstado() != EstadoPqrs.PENDIENTE || pqrs.getResponsable() != null) {
            throw new ReglaNegocioException("Solo se puede tomar un caso pendiente que nadie atiende");
        }
        pqrs.setResponsable(usuario);
        pqrs.setEstado(EstadoPqrs.EN_ATENCION);
        pqrsRepository.save(pqrs);
        registrar(pqrs, usuario, TipoAccion.CASO_TOMADO);
        return pqrs;
    }

    /**
     * Envía una respuesta manual a la clienta por el canal de mensajería y la deja en la conversación y en la bitácora.
     * Un caso crítico no admite respuesta mientras nadie lo haya tomado.
     */
    @Transactional
    public Mensaje enviarRespuesta(Long pqrsId, String correoUsuario, String asunto, String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new ReglaNegocioException("El mensaje no puede estar vacío");
        }
        Pqrs pqrs = buscarPorId(pqrsId);
        Usuario usuario = buscarUsuario(correoUsuario);
        motivoRechazoRespuesta(pqrs, usuario).ifPresent(motivo -> {
            throw new ReglaNegocioException(motivo);
        });

        Mensaje mensaje = new Mensaje();
        mensaje.setPqrs(pqrs);
        mensaje.setFecha(LocalDateTime.now(reloj));
        mensaje.setAsunto(asuntoDeRespuesta(pqrs, asunto));
        mensaje.setCuerpo(cuerpo.strip());
        mensaje.setDireccion(DireccionMensaje.SALIENTE);
        mensaje.setAutomatico(false);
        mensaje.setAutor(usuario);
        mensajeRepository.save(mensaje);

        canal.enviar(mensaje);
        registrar(pqrs, usuario, TipoAccion.RESPUESTA_ENVIADA);
        return mensaje;
    }

    private Optional<String> motivoRechazoRespuesta(Pqrs pqrs, Usuario usuario) {
        if (usuario.isBloqueado()) {
            return Optional.of("El operador está bloqueado y no puede responder");
        }
        if (pqrs.getEstado() == EstadoPqrs.RESUELTA) {
            return Optional.of("El caso ya fue resuelto");
        }
        if (esCriticoSinAsignar(pqrs)) {
            return Optional.of("Caso crítico: toma el caso antes de responder");
        }
        Usuario responsable = pqrs.getResponsable();
        boolean esDelOperador = responsable == null || responsable.getId().equals(usuario.getId());
        if (!esDelOperador && usuario.getRol() != Rol.ADMIN) {
            return Optional.of("El caso lo atiende otra persona");
        }
        return Optional.empty();
    }

    private boolean esCriticoSinAsignar(Pqrs pqrs) {
        return pqrs.getPrioridad() == Prioridad.CRITICA
                && pqrs.getResponsable() == null
                && pqrs.getEstado() != EstadoPqrs.RESUELTA;
    }

    private String asuntoDeRespuesta(Pqrs pqrs, String asunto) {
        String texto = asunto == null || asunto.isBlank() ? "Re: " + pqrs.getAsunto() : asunto.strip();
        return texto.length() <= LARGO_MAXIMO_ASUNTO ? texto : texto.substring(0, LARGO_MAXIMO_ASUNTO);
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", correo));
    }

    private void registrar(Pqrs pqrs, Usuario usuario, TipoAccion accion) {
        RegistroBitacora registro = new RegistroBitacora();
        registro.setFecha(LocalDateTime.now(reloj));
        registro.setAccion(accion);
        registro.setPqrs(pqrs);
        registro.setUsuario(usuario);
        bitacoraRepository.save(registro);
    }
}
