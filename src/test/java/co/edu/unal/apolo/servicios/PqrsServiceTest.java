package co.edu.unal.apolo.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class PqrsServiceTest {

    private static final String CORREO_OPERADOR = "operador1@pieldeazucar.co";
    private static final LocalDate HOY = LocalDate.of(2026, 11, 4);

    @Mock private PqrsRepository pqrsRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MensajeRepository mensajeRepository;
    @Mock private BitacoraRepository bitacoraRepository;
    @Mock private CanalMensajeria canal;
    @Mock private DiasHabilesService diasHabiles;

    private PqrsService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneId.of("UTC"));
        servicio = new PqrsService(pqrsRepository, usuarioRepository, mensajeRepository, bitacoraRepository,
                canal, diasHabiles, reloj);
    }

    // ---------- consultas

    @Test
    void buscarPorIdDevuelveLaPqrsExistente() {
        Pqrs pqrs = pqrs(7L, Prioridad.MEDIA, EstadoPqrs.PENDIENTE, null);
        when(pqrsRepository.findByIdConDetalle(7L)).thenReturn(Optional.of(pqrs));

        assertThat(servicio.buscarPorId(7L)).isSameAs(pqrs);
    }

    @Test
    void buscarPorIdLanzaExcepcionSiNoExiste() {
        when(pqrsRepository.findByIdConDetalle(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void listarTodasCalculaLosDiasHabilesRestantesDeCadaCaso() {
        Pqrs pqrs = pqrs(1L, Prioridad.MEDIA, EstadoPqrs.PENDIENTE, null);
        when(pqrsRepository.findAllConDetalle()).thenReturn(List.of(pqrs));
        when(diasHabiles.diasHabilesRestantes(HOY, pqrs.getFechaLimite())).thenReturn(4);

        List<ResumenPqrs> filas = servicio.listar(false);

        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).pqrs()).isSameAs(pqrs);
        assertThat(filas.get(0).diasHabilesRestantes()).isEqualTo(4);
        assertThat(filas.get(0).vencida()).isFalse();
    }

    @Test
    void listarSoloCriticasUsaElFiltroDePrioridad() {
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.PENDIENTE, null);
        when(pqrsRepository.findByPrioridadConDetalle(Prioridad.CRITICA)).thenReturn(List.of(critica));
        when(diasHabiles.diasHabilesRestantes(HOY, critica.getFechaLimite())).thenReturn(-3);

        List<ResumenPqrs> filas = servicio.listar(true);

        assertThat(filas).extracting(ResumenPqrs::pqrs).containsExactly(critica);
        assertThat(filas.get(0).vencida()).isTrue();
        assertThat(filas.get(0).diasAbsolutos()).isEqualTo(3);
        verify(pqrsRepository, never()).findAllConDetalle();
    }

    @Test
    void elDetalleDeUnCasoCriticoSinAsignarPausaLaAutomatizacionYBloqueaElEnvio() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.PENDIENTE, null);
        prepararDetalle(critica, operador);

        DetalleCaso detalle = servicio.detalle(2L, CORREO_OPERADOR);

        assertThat(detalle.criticoSinAsignar()).isTrue();
        assertThat(detalle.puedeTomar()).isTrue();
        assertThat(detalle.puedeResponder()).isFalse();
        assertThat(detalle.motivoRespuestaBloqueada()).contains("toma el caso");
        assertThat(detalle.diasHabilesRestantes()).isEqualTo(4);
        assertThat(detalle.mensajes()).hasSize(1);
        assertThat(detalle.mensajes().get(0).direccion()).isEqualTo(DireccionMensaje.ENTRANTE);
        assertThat(detalle.mensajes().get(0).autor()).isNull();
    }

    @Test
    void elDetalleDeUnCasoCriticoTomadoPorElOperadorHabilitaElEnvio() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.EN_ATENCION, operador);
        prepararDetalle(critica, operador);

        DetalleCaso detalle = servicio.detalle(2L, CORREO_OPERADOR);

        assertThat(detalle.criticoSinAsignar()).isFalse();
        assertThat(detalle.puedeTomar()).isFalse();
        assertThat(detalle.puedeResponder()).isTrue();
        assertThat(detalle.motivoRespuestaBloqueada()).isNull();
    }

    @Test
    void elDetalleMuestraLaBitacoraConElNombreDelUsuario() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);
        prepararDetalle(caso, operador);
        RegistroBitacora registro = new RegistroBitacora();
        registro.setFecha(LocalDateTime.of(2026, 11, 4, 9, 0));
        registro.setAccion(TipoAccion.CASO_TOMADO);
        registro.setUsuario(operador);
        when(bitacoraRepository.findByPqrsIdOrderByFechaAsc(4L)).thenReturn(List.of(registro));

        DetalleCaso detalle = servicio.detalle(4L, CORREO_OPERADOR);

        assertThat(detalle.bitacora()).hasSize(1);
        assertThat(detalle.bitacora().get(0).accion()).isEqualTo(TipoAccion.CASO_TOMADO);
        assertThat(detalle.bitacora().get(0).usuario()).isEqualTo("Operador Uno");
    }

    // ---------- tomar caso

    @Test
    void tomarCasoAsignaAlOperadorLoPasaAEnAtencionYRegistraEnBitacora() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.PENDIENTE, null);
        when(pqrsRepository.findByIdConDetalle(2L)).thenReturn(Optional.of(critica));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        servicio.tomarCaso(2L, CORREO_OPERADOR);

        assertThat(critica.getResponsable()).isSameAs(operador);
        assertThat(critica.getEstado()).isEqualTo(EstadoPqrs.EN_ATENCION);
        ArgumentCaptor<RegistroBitacora> captor = ArgumentCaptor.forClass(RegistroBitacora.class);
        verify(bitacoraRepository).save(captor.capture());
        assertThat(captor.getValue().getAccion()).isEqualTo(TipoAccion.CASO_TOMADO);
        assertThat(captor.getValue().getUsuario()).isSameAs(operador);
        assertThat(captor.getValue().getPqrs()).isSameAs(critica);
        assertThat(captor.getValue().getFecha()).isEqualTo(LocalDateTime.of(2026, 11, 4, 15, 0));
    }

    @Test
    void rechazaTomarElCasoSiElOperadorEstaBloqueado() {
        Usuario bloqueado = usuario(3L, Rol.OPERADOR, true);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.PENDIENTE, null);
        when(pqrsRepository.findByIdConDetalle(2L)).thenReturn(Optional.of(critica));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(bloqueado));

        assertThatThrownBy(() -> servicio.tomarCaso(2L, CORREO_OPERADOR))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("bloqueado");
        assertThat(critica.getResponsable()).isNull();
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void rechazaTomarUnCasoQueYaTieneResponsable() {
        Usuario otro = usuario(1L, Rol.ADMIN, false);
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs tomado = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, otro);
        when(pqrsRepository.findByIdConDetalle(4L)).thenReturn(Optional.of(tomado));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        assertThatThrownBy(() -> servicio.tomarCaso(4L, CORREO_OPERADOR))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(tomado.getResponsable()).isSameAs(otro);
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void rechazaTomarUnCasoQueYaNoEstaPendiente() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs resuelta = pqrs(3L, Prioridad.BAJA, EstadoPqrs.RESUELTA, null);
        when(pqrsRepository.findByIdConDetalle(3L)).thenReturn(Optional.of(resuelta));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        assertThatThrownBy(() -> servicio.tomarCaso(3L, CORREO_OPERADOR))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void tomarCasoFallaSiElUsuarioNoExiste() {
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.PENDIENTE, null);
        when(pqrsRepository.findByIdConDetalle(2L)).thenReturn(Optional.of(critica));
        when(usuarioRepository.findByCorreo("nadie@pieldeazucar.co")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.tomarCaso(2L, "nadie@pieldeazucar.co"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------- enviar respuesta

    @Test
    void rechazaElEnvioSiElCasoCriticoNoSeHaTomado() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.PENDIENTE, null);
        when(pqrsRepository.findByIdConDetalle(2L)).thenReturn(Optional.of(critica));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        assertThatThrownBy(() -> servicio.enviarRespuesta(2L, CORREO_OPERADOR, "Asunto", "Hola"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("crítico");
        verifyNoInteractions(canal);
        verify(mensajeRepository, never()).save(any());
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void rechazaElEnvioDeUnMensajeVacio() {
        assertThatThrownBy(() -> servicio.enviarRespuesta(2L, CORREO_OPERADOR, "Asunto", "   "))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.enviarRespuesta(2L, CORREO_OPERADOR, "Asunto", null))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(canal);
    }

    @Test
    void enviaLaRespuestaPorElCanalLaGuardaYLaRegistraEnBitacora() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.EN_ATENCION, operador);
        when(pqrsRepository.findByIdConDetalle(2L)).thenReturn(Optional.of(critica));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        Mensaje enviado = servicio.enviarRespuesta(2L, CORREO_OPERADOR, "Sobre tu reacción", "  Hola Valentina, lamentamos lo ocurrido.  ");

        ArgumentCaptor<Mensaje> guardado = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeRepository).save(guardado.capture());
        assertThat(guardado.getValue()).isSameAs(enviado);
        assertThat(enviado.getDireccion()).isEqualTo(DireccionMensaje.SALIENTE);
        assertThat(enviado.isAutomatico()).isFalse();
        assertThat(enviado.getAutor()).isSameAs(operador);
        assertThat(enviado.getPqrs()).isSameAs(critica);
        assertThat(enviado.getAsunto()).isEqualTo("Sobre tu reacción");
        assertThat(enviado.getCuerpo()).isEqualTo("Hola Valentina, lamentamos lo ocurrido.");
        verify(canal).enviar(enviado);

        ArgumentCaptor<RegistroBitacora> registro = ArgumentCaptor.forClass(RegistroBitacora.class);
        verify(bitacoraRepository).save(registro.capture());
        assertThat(registro.getValue().getAccion()).isEqualTo(TipoAccion.RESPUESTA_ENVIADA);
        assertThat(registro.getValue().getUsuario()).isSameAs(operador);
    }

    @Test
    void sinAsuntoUsaElDelCasoConPrefijoRe() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);
        caso.setAsunto("Quiero devolver mi crema");
        when(pqrsRepository.findByIdConDetalle(4L)).thenReturn(Optional.of(caso));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        Mensaje enviado = servicio.enviarRespuesta(4L, CORREO_OPERADOR, " ", "Claro, te ayudamos.");

        assertThat(enviado.getAsunto()).isEqualTo("Re: Quiero devolver mi crema");
    }

    @Test
    void elAsuntoNuncaSuperaElLargoDeLaColumna() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);
        caso.setAsunto("x".repeat(300));
        when(pqrsRepository.findByIdConDetalle(4L)).thenReturn(Optional.of(caso));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        Mensaje enviado = servicio.enviarRespuesta(4L, CORREO_OPERADOR, null, "Hola");

        assertThat(enviado.getAsunto()).hasSize(300);
    }

    @Test
    void unOperadorNoPuedeResponderUnCasoQueAtiendeOtraPersona() {
        Usuario duenio = usuario(5L, Rol.OPERADOR, false);
        Usuario intruso = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, duenio);
        when(pqrsRepository.findByIdConDetalle(4L)).thenReturn(Optional.of(caso));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(intruso));

        assertThatThrownBy(() -> servicio.enviarRespuesta(4L, CORREO_OPERADOR, "A", "Hola"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("otra persona");
        verifyNoInteractions(canal);
    }

    @Test
    void unAdminSiPuedeResponderUnCasoQueAtiendeUnOperador() {
        Usuario operador = usuario(5L, Rol.OPERADOR, false);
        Usuario admin = usuario(1L, Rol.ADMIN, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);
        when(pqrsRepository.findByIdConDetalle(4L)).thenReturn(Optional.of(caso));
        when(usuarioRepository.findByCorreo("paula@pieldeazucar.co")).thenReturn(Optional.of(admin));

        servicio.enviarRespuesta(4L, "paula@pieldeazucar.co", "A", "Hola");

        verify(canal).enviar(any(Mensaje.class));
    }

    @Test
    void unOperadorBloqueadoNoPuedeResponder() {
        Usuario bloqueado = usuario(3L, Rol.OPERADOR, true);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, bloqueado);
        when(pqrsRepository.findByIdConDetalle(4L)).thenReturn(Optional.of(caso));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(bloqueado));

        assertThatThrownBy(() -> servicio.enviarRespuesta(4L, CORREO_OPERADOR, "A", "Hola"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("bloqueado");
    }

    @Test
    void noSeRespondeUnCasoResuelto() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs resuelta = pqrs(3L, Prioridad.BAJA, EstadoPqrs.RESUELTA, null);
        when(pqrsRepository.findByIdConDetalle(3L)).thenReturn(Optional.of(resuelta));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        assertThatThrownBy(() -> servicio.enviarRespuesta(3L, CORREO_OPERADOR, "A", "Hola"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("resuelto");
    }

    // ---------- ayudas

    private void prepararDetalle(Pqrs pqrs, Usuario usuario) {
        Mensaje entrante = new Mensaje();
        entrante.setPqrs(pqrs);
        entrante.setFecha(LocalDateTime.of(2026, 11, 3, 10, 0));
        entrante.setAsunto(pqrs.getAsunto());
        entrante.setCuerpo("Hola, necesito ayuda.");
        entrante.setDireccion(DireccionMensaje.ENTRANTE);
        when(pqrsRepository.findByIdConDetalle(pqrs.getId())).thenReturn(Optional.of(pqrs));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(usuario));
        when(mensajeRepository.findByPqrsIdOrderByFechaAsc(pqrs.getId())).thenReturn(List.of(entrante));
        when(diasHabiles.diasHabilesRestantes(HOY, pqrs.getFechaLimite())).thenReturn(4);
    }

    private Pqrs pqrs(Long id, Prioridad prioridad, EstadoPqrs estado, Usuario responsable) {
        Pqrs pqrs = new Pqrs();
        pqrs.setId(id);
        pqrs.setAsunto("Asunto del caso " + id);
        pqrs.setPrioridad(prioridad);
        pqrs.setEstado(estado);
        pqrs.setResponsable(responsable);
        pqrs.setFechaLimite(LocalDate.of(2026, 11, 10));
        return pqrs;
    }

    private Usuario usuario(Long id, Rol rol, boolean bloqueado) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Operador Uno");
        usuario.setCorreo(CORREO_OPERADOR);
        usuario.setRol(rol);
        usuario.setBloqueado(bloqueado);
        return usuario;
    }
}
