package co.edu.unal.apolo.servicios;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unal.apolo.dominio.Adjunto;
import co.edu.unal.apolo.dominio.CategoriaResolucion;
import co.edu.unal.apolo.dominio.DireccionMensaje;
import co.edu.unal.apolo.dominio.EstadoPqrs;
import co.edu.unal.apolo.dominio.Mensaje;
import co.edu.unal.apolo.dominio.PasoProtocolo;
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
import co.edu.unal.apolo.servicios.DetalleCaso.PasoVista;
import co.edu.unal.apolo.servicios.DetalleCaso.ResolucionVista;
import co.edu.unal.apolo.servicios.DetalleCaso.RegistroVista;

/**
 * Casos de uso sobre PQRS: bandeja, detalle de un caso, tomar un caso (CU-03), enviar una respuesta manual (CU-03),
 * completar los pasos del protocolo crítico (CU-03), resolver un caso (CU-03) y agregar notas internas (CU-03).
 * Todas las acciones del operador quedan en la bitácora y todo cambio de estado deja su registro (E5-12).
 * Las notas internas viven solo en la bitácora: nunca forman parte de la conversación con la persona cliente (E5-11).
 */
@Service
public class PqrsService {

    private static final int LARGO_MAXIMO_ASUNTO = 300;
    /** Igual al largo de {@code registro_bitacora.notas_internas}. */
    static final int LARGO_MAXIMO_NOTA = 2000;
    /** Igual al largo de {@code registro_bitacora.motivo}. */
    private static final int LARGO_MAXIMO_MOTIVO = 1000;

    private final PqrsRepository pqrsRepository;
    private final UsuarioRepository usuarioRepository;
    private final MensajeRepository mensajeRepository;
    private final BitacoraRepository bitacoraRepository;
    private final CanalMensajeria canal;
    private final DiasHabilesService diasHabiles;
    private final AdjuntoService adjuntoService;
    private final Clock reloj;

    public PqrsService(PqrsRepository pqrsRepository,
                       UsuarioRepository usuarioRepository,
                       MensajeRepository mensajeRepository,
                       BitacoraRepository bitacoraRepository,
                       CanalMensajeria canal,
                       DiasHabilesService diasHabiles,
                       AdjuntoService adjuntoService,
                       Clock reloj) {
        this.pqrsRepository = pqrsRepository;
        this.usuarioRepository = usuarioRepository;
        this.mensajeRepository = mensajeRepository;
        this.bitacoraRepository = bitacoraRepository;
        this.canal = canal;
        this.diasHabiles = diasHabiles;
        this.adjuntoService = adjuntoService;
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

        Map<Long, List<String>> adjuntosPorMensaje = adjuntoService.nombresPorMensaje(id);
        List<MensajeVista> mensajes = mensajeRepository.findByPqrsIdOrderByFechaAsc(id).stream()
                .map(m -> new MensajeVista(m.getFecha(), m.getDireccion(), m.getAsunto(), m.getCuerpo(),
                        m.isAutomatico(), m.getAutor() == null ? null : m.getAutor().getNombre(),
                        m.getId() == null ? List.of() : adjuntosPorMensaje.getOrDefault(m.getId(), List.of())))
                .toList();
        List<RegistroVista> bitacora = bitacoraRepository.findByPqrsIdOrderByFechaAsc(id).stream()
                .map(r -> new RegistroVista(r.getFecha(), r.getAccion(),
                        r.getUsuario() == null ? null : r.getUsuario().getNombre(), motivoVisible(r),
                        r.getNotasInternas()))
                .toList();

        Optional<String> motivoRechazo = motivoRechazoRespuesta(pqrs, usuario);
        boolean puedeTomar = !usuario.isBloqueado()
                && pqrs.getEstado() == EstadoPqrs.PENDIENTE
                && pqrs.getResponsable() == null;
        int diasRestantes = diasHabiles.diasHabilesRestantes(LocalDate.now(reloj), pqrs.getFechaLimite());

        boolean critico = pqrs.getPrioridad() == Prioridad.CRITICA;
        List<PasoVista> protocolo = critico ? protocolo(id) : List.of();
        boolean puedeCompletarPasos = critico && motivoRechazoPaso(pqrs, usuario).isEmpty();

        boolean protocoloIncompleto = critico && protocolo.stream().anyMatch(paso -> !paso.completado());
        Optional<String> motivoNoResolver = motivoRechazoResolver(pqrs, usuario, protocoloIncompleto);
        ResolucionVista resolucion = pqrs.getEstado() == EstadoPqrs.RESUELTA ? resolucionVista(pqrs) : null;

        return new DetalleCaso(pqrs, mensajes, bitacora, diasRestantes, esCriticoSinAsignar(pqrs),
                puedeTomar, motivoRechazo.isEmpty(), motivoRechazo.orElse(null), protocolo, puedeCompletarPasos,
                motivoNoResolver.isEmpty(), motivoNoResolver.orElse(null), resolucion,
                motivoRechazoNota(pqrs, usuario).orElse(null), adjuntoService.tamanoMaximoLegible(pqrs.getCanal()));
    }

    /**
     * El operador se autoasigna un caso pendiente: pasa a "En atención" (o "Atención inmediata" si es crítico),
     * queda como responsable (lo que desbloquea el envío en los casos críticos) y el cambio de estado
     * se registra en la bitácora.
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
        cambiarEstado(pqrs, usuario, pqrs.getPrioridad() == Prioridad.CRITICA
                ? EstadoPqrs.ATENCION_INMEDIATA
                : EstadoPqrs.EN_ATENCION, TipoAccion.CASO_TOMADO, null);
        return pqrs;
    }

    /**
     * Envía una respuesta manual a la persona cliente por el canal de mensajería y la deja en la conversación y en la bitácora.
     * Un caso crítico no admite respuesta mientras nadie lo haya tomado.
     */
    @Transactional
    public Mensaje enviarRespuesta(Long pqrsId, String correoUsuario, String asunto, String cuerpo) {
        return enviarRespuesta(pqrsId, correoUsuario, asunto, cuerpo, List.of());
    }

    /**
     * Igual que la anterior, pero con adjuntos JPG, PNG o PDF (CU-02-HU-02). Los adjuntos se validan contra el tamaño
     * máximo del canal del caso antes de guardar nada: si alguno no es válido, no se envía el mensaje.
     */
    @Transactional
    public Mensaje enviarRespuesta(Long pqrsId, String correoUsuario, String asunto, String cuerpo,
                                   List<ArchivoAdjunto> archivos) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new ReglaNegocioException("El mensaje no puede estar vacío");
        }
        Pqrs pqrs = buscarPorId(pqrsId);
        Usuario usuario = buscarUsuario(correoUsuario);
        motivoRechazoRespuesta(pqrs, usuario).ifPresent(motivo -> {
            throw new ReglaNegocioException(motivo);
        });
        adjuntoService.validar(archivos, pqrs.getCanal());

        Mensaje mensaje = new Mensaje();
        mensaje.setPqrs(pqrs);
        mensaje.setFecha(LocalDateTime.now(reloj));
        mensaje.setAsunto(asuntoDeRespuesta(pqrs, asunto));
        mensaje.setCuerpo(cuerpo.strip());
        mensaje.setDireccion(DireccionMensaje.SALIENTE);
        mensaje.setAutomatico(false);
        mensaje.setAutor(usuario);
        mensajeRepository.save(mensaje);
        List<Adjunto> adjuntos = adjuntoService.guardar(mensaje, archivos);

        canal.enviar(mensaje, adjuntos);
        registrar(pqrs, usuario, TipoAccion.RESPUESTA_ENVIADA, detalleAdjuntos(adjuntos), null);
        return mensaje;
    }

    /**
     * Agrega una nota interna al caso. Queda solo en la bitácora (visible para el equipo) y no se envía
     * ni aparece en la conversación con la persona cliente. No cambia el estado del caso.
     */
    @Transactional
    public void agregarNotaInterna(Long pqrsId, String correoUsuario, String nota) {
        Pqrs pqrs = buscarPorId(pqrsId);
        Usuario usuario = buscarUsuario(correoUsuario);
        motivoRechazoNota(pqrs, usuario).ifPresent(motivo -> {
            throw new ReglaNegocioException(motivo);
        });
        registrar(pqrs, usuario, TipoAccion.NOTA_INTERNA, null, nota);
    }

    /**
     * Marca como completado un paso del protocolo de un caso crítico. Se guarda como registro de bitácora
     * (motivo = nombre del paso); no se puede desmarcar.
     */
    @Transactional
    public void completarPaso(Long pqrsId, String correoUsuario, PasoProtocolo paso) {
        Pqrs pqrs = buscarPorId(pqrsId);
        Usuario usuario = buscarUsuario(correoUsuario);
        motivoRechazoPaso(pqrs, usuario).ifPresent(motivo -> {
            throw new ReglaNegocioException(motivo);
        });
        if (pasosCompletados(pqrsId).containsKey(paso.name())) {
            throw new ReglaNegocioException("Ese paso ya se completó");
        }
        registrar(pqrs, usuario, TipoAccion.PASO_PROTOCOLO_COMPLETADO, paso.name(), null);
    }

    private Optional<String> motivoRechazoNota(Pqrs pqrs, Usuario usuario) {
        if (usuario.isBloqueado()) {
            return Optional.of("El operador está bloqueado y no puede agregar notas");
        }
        if (pqrs.getEstado() == EstadoPqrs.RESUELTA) {
            return Optional.of("El caso ya fue resuelto");
        }
        Usuario responsable = pqrs.getResponsable();
        if (responsable == null) {
            return Optional.of("Toma el caso antes de agregar notas internas");
        }
        if (!responsable.getId().equals(usuario.getId()) && usuario.getRol() != Rol.ADMIN) {
            return Optional.of("El caso lo atiende otra persona");
        }
        return Optional.empty();
    }

    /**
     * Resuelve un caso: exige una categoría y una nota interna, lo pasa a "Resuelta", guarda quién y cuándo lo resolvió
     * y lo registra en la bitácora (motivo = categoría, notas internas = la nota). Un caso crítico solo se resuelve con
     * todos los pasos del protocolo completos. La nota es interna: no se envía a la persona cliente.
     * Un caso resuelto ya no admite respuestas.
     */
    @Transactional
    public Pqrs resolverCaso(Long pqrsId, String correoUsuario, CategoriaResolucion categoria, String nota) {
        if (categoria == null) {
            throw new ReglaNegocioException("Elige la categoría de la resolución");
        }
        if (nota == null || nota.isBlank()) {
            throw new ReglaNegocioException("Escribe la nota de resolución");
        }
        String notaLimpia = nota.strip();
        if (notaLimpia.length() > LARGO_MAXIMO_NOTA) {
            throw new ReglaNegocioException("La nota no puede superar " + LARGO_MAXIMO_NOTA + " caracteres");
        }
        Pqrs pqrs = buscarPorId(pqrsId);
        Usuario usuario = buscarUsuario(correoUsuario);
        boolean protocoloIncompleto = pqrs.getPrioridad() == Prioridad.CRITICA
                && pasosCompletados(pqrsId).size() < PasoProtocolo.values().length;
        motivoRechazoResolver(pqrs, usuario, protocoloIncompleto).ifPresent(motivo -> {
            throw new ReglaNegocioException(motivo);
        });

        pqrs.setCategoriaResolucion(categoria);
        pqrs.setDescripcionSolucion(notaLimpia);
        pqrs.setFechaResolucion(LocalDateTime.now(reloj));
        pqrs.setResueltaPor(usuario);
        cambiarEstado(pqrs, usuario, EstadoPqrs.RESUELTA, TipoAccion.CASO_RESUELTO, categoria.name(), notaLimpia);
        return pqrs;
    }

    private Optional<String> motivoRechazoResolver(Pqrs pqrs, Usuario usuario, boolean protocoloIncompleto) {
        if (usuario.isBloqueado()) {
            return Optional.of("El operador está bloqueado y no puede resolver casos");
        }
        if (pqrs.getEstado() == EstadoPqrs.RESUELTA) {
            return Optional.of("El caso ya fue resuelto");
        }
        Usuario responsable = pqrs.getResponsable();
        if (responsable == null) {
            return Optional.of("Toma el caso antes de resolverlo");
        }
        if (!responsable.getId().equals(usuario.getId()) && usuario.getRol() != Rol.ADMIN) {
            return Optional.of("El caso lo atiende otra persona");
        }
        if (protocoloIncompleto) {
            return Optional.of("Completa el protocolo crítico antes de resolver el caso");
        }
        return Optional.empty();
    }

    private ResolucionVista resolucionVista(Pqrs pqrs) {
        Usuario resueltaPor = pqrs.getResueltaPor();
        return new ResolucionVista(pqrs.getCategoriaResolucion(), pqrs.getDescripcionSolucion(),
                pqrs.getFechaResolucion(), resueltaPor == null ? null : resueltaPor.getNombre());
    }

    private Optional<String> motivoRechazoPaso(Pqrs pqrs, Usuario usuario) {
        if (usuario.isBloqueado()) {
            return Optional.of("El operador está bloqueado y no puede completar el protocolo");
        }
        if (pqrs.getPrioridad() != Prioridad.CRITICA) {
            return Optional.of("El protocolo solo aplica a casos críticos");
        }
        if (pqrs.getEstado() == EstadoPqrs.RESUELTA) {
            return Optional.of("El caso ya fue resuelto");
        }
        Usuario responsable = pqrs.getResponsable();
        if (responsable == null) {
            return Optional.of("Toma el caso antes de completar el protocolo");
        }
        if (!responsable.getId().equals(usuario.getId()) && usuario.getRol() != Rol.ADMIN) {
            return Optional.of("El caso lo atiende otra persona");
        }
        return Optional.empty();
    }

    private List<PasoVista> protocolo(Long pqrsId) {
        Map<String, RegistroBitacora> completados = pasosCompletados(pqrsId);
        return Arrays.stream(PasoProtocolo.values())
                .map(paso -> {
                    RegistroBitacora registro = completados.get(paso.name());
                    return registro == null
                            ? new PasoVista(paso, false, null, null)
                            : new PasoVista(paso, true, registro.getFecha(),
                                    registro.getUsuario() == null ? null : registro.getUsuario().getNombre());
                })
                .toList();
    }

    private Map<String, RegistroBitacora> pasosCompletados(Long pqrsId) {
        return bitacoraRepository.findByPqrsIdAndAccionOrderByFechaAsc(pqrsId, TipoAccion.PASO_PROTOCOLO_COMPLETADO)
                .stream()
                .filter(r -> r.getMotivo() != null)
                .collect(Collectors.toMap(RegistroBitacora::getMotivo, Function.identity(), (primero, otro) -> primero));
    }

    private String motivoVisible(RegistroBitacora registro) {
        if (registro.getAccion() == TipoAccion.PASO_PROTOCOLO_COMPLETADO && registro.getMotivo() != null) {
            return PasoProtocolo.valueOf(registro.getMotivo()).getEtiqueta();
        }
        if (registro.getAccion() == TipoAccion.CASO_RESUELTO && registro.getMotivo() != null) {
            return CategoriaResolucion.valueOf(registro.getMotivo()).getEtiqueta();
        }
        return registro.getMotivo();
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

    /** Lo que queda en la bitácora sobre los adjuntos de una respuesta (nulo si no tiene). */
    private String detalleAdjuntos(List<Adjunto> adjuntos) {
        if (adjuntos == null || adjuntos.isEmpty()) {
            return null;
        }
        String texto = "Adjuntos: " + adjuntos.stream().map(Adjunto::getNombreArchivo).collect(Collectors.joining(", "));
        return texto.length() <= LARGO_MAXIMO_MOTIVO ? texto : texto.substring(0, LARGO_MAXIMO_MOTIVO);
    }

    private String asuntoDeRespuesta(Pqrs pqrs, String asunto) {
        String texto = asunto == null || asunto.isBlank() ? "Re: " + pqrs.getAsunto() : asunto.strip();
        return texto.length() <= LARGO_MAXIMO_ASUNTO ? texto : texto.substring(0, LARGO_MAXIMO_ASUNTO);
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", correo));
    }

    /**
     * Único punto por el que cambia el estado de un caso: valida la nota (si la acción la exige), aplica el
     * nuevo estado y deja el registro en la bitácora con fecha, usuario, acción y el cambio "anterior → nuevo".
     */
    private void cambiarEstado(Pqrs pqrs, Usuario usuario, EstadoPqrs nuevo, TipoAccion accion, String nota) {
        String cambio = pqrs.getEstado().getEtiqueta() + " → " + nuevo.getEtiqueta();
        cambiarEstado(pqrs, usuario, nuevo, accion, cambio, nota);
    }

    /** Igual que el anterior, pero con el detalle (motivo) que se guarda en la bitácora decidido por quien llama. */
    private void cambiarEstado(Pqrs pqrs, Usuario usuario, EstadoPqrs nuevo, TipoAccion accion, String motivo,
                               String nota) {
        validarNota(accion, nota);
        pqrs.setEstado(nuevo);
        pqrsRepository.save(pqrs);
        registrar(pqrs, usuario, accion, motivo, nota);
    }

    /**
     * Guarda un registro de bitácora. Si la acción exige nota interna ({@link TipoAccion#exigeNotaInterna()})
     * y no hay, lanza {@link ReglaNegocioException} sin guardar nada. Visible para las pruebas del paquete.
     */
    void registrar(Pqrs pqrs, Usuario usuario, TipoAccion accion, String motivo, String nota) {
        validarNota(accion, nota);
        String notaLimpia = nota == null || nota.isBlank() ? null : nota.strip();
        RegistroBitacora registro = new RegistroBitacora();
        registro.setMotivo(motivo);
        registro.setNotasInternas(notaLimpia);
        registro.setFecha(LocalDateTime.now(reloj));
        registro.setAccion(accion);
        registro.setPqrs(pqrs);
        registro.setUsuario(usuario);
        bitacoraRepository.save(registro);
    }

    private void validarNota(TipoAccion accion, String nota) {
        boolean hayNota = nota != null && !nota.isBlank();
        if (accion.exigeNotaInterna() && !hayNota) {
            throw new ReglaNegocioException("La nota interna es obligatoria para esta acción");
        }
        if (hayNota && nota.strip().length() > LARGO_MAXIMO_NOTA) {
            throw new ReglaNegocioException("La nota interna no puede superar " + LARGO_MAXIMO_NOTA + " caracteres");
        }
    }
}
