package co.edu.unal.apolo.servicios;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.unal.apolo.dominio.Adjunto;
import co.edu.unal.apolo.dominio.Canal;
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
    @Mock private AdjuntoService adjuntoService;

    private PqrsService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneId.of("UTC"));
        servicio = new PqrsService(pqrsRepository, usuarioRepository, mensajeRepository, bitacoraRepository,
                canal, diasHabiles, adjuntoService, reloj);
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
    void tomarCasoCriticoLoPasaAAtencionInmediataYRegistraEnBitacora() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.PENDIENTE, null);
        when(pqrsRepository.findByIdConDetalle(2L)).thenReturn(Optional.of(critica));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        servicio.tomarCaso(2L, CORREO_OPERADOR);

        assertThat(critica.getResponsable()).isSameAs(operador);
        assertThat(critica.getEstado()).isEqualTo(EstadoPqrs.ATENCION_INMEDIATA);
        ArgumentCaptor<RegistroBitacora> captor = ArgumentCaptor.forClass(RegistroBitacora.class);
        verify(bitacoraRepository).save(captor.capture());
        assertThat(captor.getValue().getAccion()).isEqualTo(TipoAccion.CASO_TOMADO);
        assertThat(captor.getValue().getUsuario()).isSameAs(operador);
        assertThat(captor.getValue().getPqrs()).isSameAs(critica);
        assertThat(captor.getValue().getFecha()).isEqualTo(LocalDateTime.of(2026, 11, 4, 15, 0));
    }

    @Test
void tomarCasoNoCriticoLoPasaAEnAtencion() {
    Usuario operador = usuario(2L, Rol.OPERADOR, false);
    Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.PENDIENTE, null);
    when(pqrsRepository.findByIdConDetalle(4L)).thenReturn(Optional.of(caso));
    when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

    servicio.tomarCaso(4L, CORREO_OPERADOR);

    assertThat(caso.getEstado()).isEqualTo(EstadoPqrs.EN_ATENCION);
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
        verify(canal).enviar(enviado, List.of());

        ArgumentCaptor<RegistroBitacora> registro = ArgumentCaptor.forClass(RegistroBitacora.class);
        verify(bitacoraRepository).save(registro.capture());
        assertThat(registro.getValue().getAccion()).isEqualTo(TipoAccion.RESPUESTA_ENVIADA);
        assertThat(registro.getValue().getUsuario()).isSameAs(operador);
    }

    @Test
    void enviaLosAdjuntosValidadosContraElCanalDelCasoYLosDejaEnLaBitacora() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.MEDIA, EstadoPqrs.EN_ATENCION, operador);
        caso.setCanal(Canal.GMAIL);
        when(pqrsRepository.findByIdConDetalle(4L)).thenReturn(Optional.of(caso));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));
        List<ArchivoAdjunto> archivos = List.of(new ArchivoAdjunto("guia_modo_de_uso.pdf", new byte[] {'%', 'P', 'D', 'F', '-'}));
        Adjunto guia = new Adjunto();
        guia.setNombreArchivo("guia_modo_de_uso.pdf");
        when(adjuntoService.guardar(any(Mensaje.class), any())).thenReturn(List.of(guia));

        Mensaje enviado = servicio.enviarRespuesta(4L, CORREO_OPERADOR, "Modo de uso", "Te comparto la guía.", archivos);

        verify(adjuntoService).validar(archivos, Canal.GMAIL);
        verify(adjuntoService).guardar(enviado, archivos);
        verify(canal).enviar(enviado, List.of(guia));
        ArgumentCaptor<RegistroBitacora> registro = ArgumentCaptor.forClass(RegistroBitacora.class);
        verify(bitacoraRepository).save(registro.capture());
        assertThat(registro.getValue().getAccion()).isEqualTo(TipoAccion.RESPUESTA_ENVIADA);
        assertThat(registro.getValue().getMotivo()).isEqualTo("Adjuntos: guia_modo_de_uso.pdf");
    }

    @Test
    void siUnAdjuntoNoEsValidoNoSeGuardaNiSeEnviaElMensaje() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.MEDIA, EstadoPqrs.EN_ATENCION, operador);
        caso.setCanal(Canal.GMAIL);
        when(pqrsRepository.findByIdConDetalle(4L)).thenReturn(Optional.of(caso));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));
        List<ArchivoAdjunto> archivos = List.of(new ArchivoAdjunto("ficha.pdf", new byte[] {'%', 'P', 'D', 'F', '-'}));
        doThrow(new ReglaNegocioException("El archivo ficha.pdf pesa 12 MB y el máximo para Gmail es 10 MB"))
                .when(adjuntoService).validar(archivos, Canal.GMAIL);

        assertThatThrownBy(() -> servicio.enviarRespuesta(4L, CORREO_OPERADOR, null, "Hola", archivos))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("máximo para Gmail");
        verify(mensajeRepository, never()).save(any());
        verify(adjuntoService, never()).guardar(any(), any());
        verifyNoInteractions(canal);
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void elDetalleMuestraLosAdjuntosDeCadaMensajeYElMaximoDelCanal() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);
        caso.setCanal(Canal.GMAIL);
        Mensaje entrante = new Mensaje();
        entrante.setId(20L);
        entrante.setPqrs(caso);
        entrante.setFecha(LocalDateTime.of(2026, 11, 3, 10, 0));
        entrante.setCuerpo("Adjunto una foto.");
        entrante.setDireccion(DireccionMensaje.ENTRANTE);
        when(pqrsRepository.findByIdConDetalle(4L)).thenReturn(Optional.of(caso));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));
        when(mensajeRepository.findByPqrsIdOrderByFechaAsc(4L)).thenReturn(List.of(entrante));
        when(adjuntoService.nombresPorMensaje(4L)).thenReturn(java.util.Map.of(20L, List.of("foto.jpg")));
        when(adjuntoService.tamanoMaximoLegible(Canal.GMAIL)).thenReturn("10 MB");

        DetalleCaso detalle = servicio.detalle(4L, CORREO_OPERADOR);

        assertThat(detalle.mensajes()).singleElement()
                .extracting(DetalleCaso.MensajeVista::adjuntos)
                .isEqualTo(List.of("foto.jpg"));
        assertThat(detalle.tamanoMaximoAdjuntos()).isEqualTo("10 MB");
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

        verify(canal).enviar(any(Mensaje.class), anyList());
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

    // ---------- protocolo crítico

    @Test
    void completarPasoGuardaElRegistroConElNombreDelPaso() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.EN_ATENCION, operador);
        prepararPaso(critica, operador);

        servicio.completarPaso(2L, CORREO_OPERADOR, PasoProtocolo.SOLICITAR_FOTOS);

        ArgumentCaptor<RegistroBitacora> captor = ArgumentCaptor.forClass(RegistroBitacora.class);
        verify(bitacoraRepository).save(captor.capture());
        assertThat(captor.getValue().getAccion()).isEqualTo(TipoAccion.PASO_PROTOCOLO_COMPLETADO);
        assertThat(captor.getValue().getMotivo()).isEqualTo("SOLICITAR_FOTOS");
        assertThat(captor.getValue().getUsuario()).isSameAs(operador);
        assertThat(captor.getValue().getPqrs()).isSameAs(critica);
    }

    @Test
    void rechazaCompletarUnPasoSiElCasoNoSeHaTomado() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.PENDIENTE, null);
        when(pqrsRepository.findByIdConDetalle(2L)).thenReturn(Optional.of(critica));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        assertThatThrownBy(() -> servicio.completarPaso(2L, CORREO_OPERADOR, PasoProtocolo.SUSPENDER_USO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Toma el caso antes de completar el protocolo");
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void rechazaCompletarUnPasoQueYaSeCompleto() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.EN_ATENCION, operador);
        prepararPaso(critica, operador, registroPaso(PasoProtocolo.SUSPENDER_USO, operador));

        assertThatThrownBy(() -> servicio.completarPaso(2L, CORREO_OPERADOR, PasoProtocolo.SUSPENDER_USO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Ese paso ya se completó");
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void rechazaCompletarUnPasoEnUnCasoNoCritico() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);
        when(pqrsRepository.findByIdConDetalle(4L)).thenReturn(Optional.of(caso));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        assertThatThrownBy(() -> servicio.completarPaso(4L, CORREO_OPERADOR, PasoProtocolo.SUSPENDER_USO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("críticos");
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void unOperadorNoPuedeCompletarPasosDeUnCasoQueAtiendeOtraPersona() {
        Usuario otro = usuario(3L, Rol.OPERADOR, false);
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.EN_ATENCION, otro);
        when(pqrsRepository.findByIdConDetalle(2L)).thenReturn(Optional.of(critica));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        assertThatThrownBy(() -> servicio.completarPaso(2L, CORREO_OPERADOR, PasoProtocolo.SUSPENDER_USO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El caso lo atiende otra persona");
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void elDetalleMarcaLosPasosCompletadosYMuestraSuEtiquetaEnLaBitacora() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.EN_ATENCION, operador);
        prepararDetalle(critica, operador);
        RegistroBitacora paso = registroPaso(PasoProtocolo.SOLICITAR_LOTE, operador);
        when(bitacoraRepository.findByPqrsIdAndAccionOrderByFechaAsc(2L, TipoAccion.PASO_PROTOCOLO_COMPLETADO))
                .thenReturn(List.of(paso));
        when(bitacoraRepository.findByPqrsIdOrderByFechaAsc(2L)).thenReturn(List.of(paso));

        DetalleCaso detalle = servicio.detalle(2L, CORREO_OPERADOR);

        assertThat(detalle.puedeCompletarPasos()).isTrue();
        assertThat(detalle.protocolo()).extracting(DetalleCaso.PasoVista::paso)
                .containsExactly(PasoProtocolo.values());
        assertThat(detalle.protocolo()).extracting(DetalleCaso.PasoVista::completado)
                .containsExactly(false, false, true, false);
        assertThat(detalle.protocolo().get(2).usuario()).isEqualTo("Operador Uno");
        assertThat(detalle.protocolo().get(2).fecha()).isEqualTo(LocalDateTime.of(2026, 11, 4, 10, 0));
        assertThat(detalle.bitacora().get(0).motivo()).isEqualTo(PasoProtocolo.SOLICITAR_LOTE.getEtiqueta());
    }

    @Test
    void elDetalleDeUnCasoNoCriticoNoTieneProtocolo() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);
        prepararDetalle(caso, operador);

        DetalleCaso detalle = servicio.detalle(4L, CORREO_OPERADOR);

        assertThat(detalle.protocolo()).isEmpty();
        assertThat(detalle.puedeCompletarPasos()).isFalse();
    }

    @Test
    void unOperadorBloqueadoNoPuedeCompletarPasos() {
        Usuario bloqueado = usuario(2L, Rol.OPERADOR, true);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.ATENCION_INMEDIATA, bloqueado);
        when(pqrsRepository.findByIdConDetalle(2L)).thenReturn(Optional.of(critica));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(bloqueado));

        assertThatThrownBy(() -> servicio.completarPaso(2L, CORREO_OPERADOR, PasoProtocolo.SUSPENDER_USO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El operador está bloqueado y no puede completar el protocolo");
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void noSeCompletanPasosDeUnCasoResuelto() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs resuelta = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.RESUELTA, operador);
        when(pqrsRepository.findByIdConDetalle(2L)).thenReturn(Optional.of(resuelta));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        assertThatThrownBy(() -> servicio.completarPaso(2L, CORREO_OPERADOR, PasoProtocolo.SUSPENDER_USO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El caso ya fue resuelto");
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void unAdminSiPuedeCompletarPasosDeUnCasoQueAtiendeUnOperador() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Usuario admin = usuario(1L, Rol.ADMIN, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.ATENCION_INMEDIATA, operador);
        prepararPaso(critica, admin);

        servicio.completarPaso(2L, CORREO_OPERADOR, PasoProtocolo.SOLICITAR_FOTOS);

        ArgumentCaptor<RegistroBitacora> captor = ArgumentCaptor.forClass(RegistroBitacora.class);
        verify(bitacoraRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isSameAs(admin);
    }

    // ---------- resolver casos

    @Test
    void resolverUnCasoLoPasaAResueltaYGuardaLaResolucionYLaBitacora() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(5L, Prioridad.MEDIA, EstadoPqrs.EN_ATENCION, operador);
        prepararResolver(caso, operador);

        Pqrs resuelto = servicio.resolverCaso(5L, CORREO_OPERADOR, CategoriaResolucion.SOLUCIONADO, "  Se envió el cambio.  ");

        assertThat(resuelto.getEstado()).isEqualTo(EstadoPqrs.RESUELTA);
        assertThat(resuelto.getCategoriaResolucion()).isEqualTo(CategoriaResolucion.SOLUCIONADO);
        assertThat(resuelto.getDescripcionSolucion()).isEqualTo("Se envió el cambio.");
        assertThat(resuelto.getFechaResolucion()).isEqualTo(LocalDateTime.of(2026, 11, 4, 15, 0));
        assertThat(resuelto.getResueltaPor()).isSameAs(operador);
        verify(pqrsRepository).save(caso);
        ArgumentCaptor<RegistroBitacora> captor = ArgumentCaptor.forClass(RegistroBitacora.class);
        verify(bitacoraRepository).save(captor.capture());
        assertThat(captor.getValue().getAccion()).isEqualTo(TipoAccion.CASO_RESUELTO);
        assertThat(captor.getValue().getMotivo()).isEqualTo("SOLUCIONADO");
        assertThat(captor.getValue().getNotasInternas()).isEqualTo("Se envió el cambio.");
        assertThat(captor.getValue().getUsuario()).isSameAs(operador);
        assertThat(captor.getValue().getPqrs()).isSameAs(caso);
        verifyNoInteractions(canal);
    }

    @Test
    void resolverUnCasoCriticoConElProtocoloCompletoLoDejaResuelto() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.ATENCION_INMEDIATA, operador);
        prepararPaso(critica, operador, todosLosPasos(operador));

        Pqrs resuelto = servicio.resolverCaso(2L, CORREO_OPERADOR, CategoriaResolucion.COMPENSACION_APLICADA, "Se repuso el producto.");

        assertThat(resuelto.getEstado()).isEqualTo(EstadoPqrs.RESUELTA);
        assertThat(resuelto.getCategoriaResolucion()).isEqualTo(CategoriaResolucion.COMPENSACION_APLICADA);
    }

    @Test
    void rechazaResolverUnCasoCriticoConPasosDelProtocoloPendientes() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.ATENCION_INMEDIATA, operador);
        prepararPaso(critica, operador, registroPaso(PasoProtocolo.SUSPENDER_USO, operador));

        assertThatThrownBy(() -> servicio.resolverCaso(2L, CORREO_OPERADOR, CategoriaResolucion.SOLUCIONADO, "Listo"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Completa el protocolo crítico antes de resolver el caso");
        assertThat(critica.getEstado()).isEqualTo(EstadoPqrs.ATENCION_INMEDIATA);
        verify(pqrsRepository, never()).save(any());
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void rechazaResolverSinCategoria() {
        assertThatThrownBy(() -> servicio.resolverCaso(5L, CORREO_OPERADOR, null, "Listo"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Elige la categoría de la resolución");
        verifyNoInteractions(pqrsRepository, bitacoraRepository);
    }

    @Test
    void rechazaResolverSinNotaOConNotaEnBlanco() {
        assertThatThrownBy(() -> servicio.resolverCaso(5L, CORREO_OPERADOR, CategoriaResolucion.SOLUCIONADO, "   "))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Escribe la nota de resolución");
        assertThatThrownBy(() -> servicio.resolverCaso(5L, CORREO_OPERADOR, CategoriaResolucion.SOLUCIONADO, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Escribe la nota de resolución");
        verifyNoInteractions(pqrsRepository, bitacoraRepository);
    }

    @Test
    void rechazaUnaNotaMasLargaQueLaColumna() {
        String nota = "x".repeat(2001);

        assertThatThrownBy(() -> servicio.resolverCaso(5L, CORREO_OPERADOR, CategoriaResolucion.SOLUCIONADO, nota))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("2000");
        verifyNoInteractions(pqrsRepository, bitacoraRepository);
    }

    @Test
    void rechazaResolverUnCasoQueNadieHaTomado() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(5L, Prioridad.MEDIA, EstadoPqrs.PENDIENTE, null);
        prepararResolver(caso, operador);

        assertThatThrownBy(() -> servicio.resolverCaso(5L, CORREO_OPERADOR, CategoriaResolucion.SOLUCIONADO, "Listo"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Toma el caso antes de resolverlo");
        verify(pqrsRepository, never()).save(any());
    }

    @Test
    void rechazaResolverUnCasoQueYaEstaResuelto() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(5L, Prioridad.MEDIA, EstadoPqrs.RESUELTA, operador);
        prepararResolver(caso, operador);

        assertThatThrownBy(() -> servicio.resolverCaso(5L, CORREO_OPERADOR, CategoriaResolucion.SOLUCIONADO, "Listo"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El caso ya fue resuelto");
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void unOperadorNoPuedeResolverUnCasoQueAtiendeOtraPersona() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Usuario otro = usuario(3L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(5L, Prioridad.MEDIA, EstadoPqrs.EN_ATENCION, otro);
        prepararResolver(caso, operador);

        assertThatThrownBy(() -> servicio.resolverCaso(5L, CORREO_OPERADOR, CategoriaResolucion.SOLUCIONADO, "Listo"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El caso lo atiende otra persona");
        verify(pqrsRepository, never()).save(any());
    }

    @Test
    void unAdminSiPuedeResolverUnCasoQueAtiendeUnOperador() {
        Usuario admin = usuario(1L, Rol.ADMIN, false);
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(5L, Prioridad.MEDIA, EstadoPqrs.EN_ATENCION, operador);
        prepararResolver(caso, admin);

        servicio.resolverCaso(5L, CORREO_OPERADOR, CategoriaResolucion.NO_PROCEDE, "Fuera de política.");

        assertThat(caso.getEstado()).isEqualTo(EstadoPqrs.RESUELTA);
        assertThat(caso.getResueltaPor()).isSameAs(admin);
    }

    @Test
    void unOperadorBloqueadoNoPuedeResolver() {
        Usuario bloqueado = usuario(2L, Rol.OPERADOR, true);
        Pqrs caso = pqrs(5L, Prioridad.MEDIA, EstadoPqrs.EN_ATENCION, bloqueado);
        prepararResolver(caso, bloqueado);

        assertThatThrownBy(() -> servicio.resolverCaso(5L, CORREO_OPERADOR, CategoriaResolucion.SOLUCIONADO, "Listo"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("bloqueado");
        verify(pqrsRepository, never()).save(any());
    }

    @Test
    void resolverUnCasoInexistenteLanzaRecursoNoEncontrado() {
        when(pqrsRepository.findByIdConDetalle(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.resolverCaso(99L, CORREO_OPERADOR, CategoriaResolucion.SOLUCIONADO, "Listo"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void despuesDeResolverUnCasoYaNoAdmiteRespuestas() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(5L, Prioridad.MEDIA, EstadoPqrs.EN_ATENCION, operador);
        prepararResolver(caso, operador);
        servicio.resolverCaso(5L, CORREO_OPERADOR, CategoriaResolucion.SOLUCIONADO, "Listo");

        assertThatThrownBy(() -> servicio.enviarRespuesta(5L, CORREO_OPERADOR, null, "Hola otra vez"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El caso ya fue resuelto");
        verifyNoInteractions(canal);
    }

    @Test
    void elDetalleDeUnCasoTomadoPermiteResolver() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(5L, Prioridad.MEDIA, EstadoPqrs.EN_ATENCION, operador);
        prepararDetalle(caso, operador);

        DetalleCaso detalle = servicio.detalle(5L, CORREO_OPERADOR);

        assertThat(detalle.puedeResolver()).isTrue();
        assertThat(detalle.motivoResolverBloqueado()).isNull();
        assertThat(detalle.resolucion()).isNull();
    }

    @Test
    void elDetalleDeUnCasoCriticoConPasosPendientesNoPermiteResolver() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.ATENCION_INMEDIATA, operador);
        prepararDetalle(critica, operador);

        DetalleCaso detalle = servicio.detalle(2L, CORREO_OPERADOR);

        assertThat(detalle.puedeResolver()).isFalse();
        assertThat(detalle.motivoResolverBloqueado()).isEqualTo("Completa el protocolo crítico antes de resolver el caso");
    }

    @Test
    void elDetalleDeUnCasoCriticoConElProtocoloCompletoPermiteResolver() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.ATENCION_INMEDIATA, operador);
        prepararDetalle(critica, operador);
        when(bitacoraRepository.findByPqrsIdAndAccionOrderByFechaAsc(2L, TipoAccion.PASO_PROTOCOLO_COMPLETADO))
                .thenReturn(List.of(todosLosPasos(operador)));

        DetalleCaso detalle = servicio.detalle(2L, CORREO_OPERADOR);

        assertThat(detalle.puedeResolver()).isTrue();
    }

    @Test
    void elDetalleDeUnCasoSinResponsableExplicaQueHayQueTomarloParaResolver() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(5L, Prioridad.MEDIA, EstadoPqrs.PENDIENTE, null);
        prepararDetalle(caso, operador);

        DetalleCaso detalle = servicio.detalle(5L, CORREO_OPERADOR);

        assertThat(detalle.puedeResolver()).isFalse();
        assertThat(detalle.motivoResolverBloqueado()).isEqualTo("Toma el caso antes de resolverlo");
    }

    @Test
    void elDetalleDeUnCasoResueltoMuestraLaResolucionYNoOfreceResolver() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(5L, Prioridad.MEDIA, EstadoPqrs.RESUELTA, operador);
        caso.setCategoriaResolucion(CategoriaResolucion.INFORMACION_BRINDADA);
        caso.setDescripcionSolucion("Se explicó el modo de uso.");
        caso.setFechaResolucion(LocalDateTime.of(2026, 11, 3, 16, 30));
        caso.setResueltaPor(operador);
        prepararDetalle(caso, operador);

        DetalleCaso detalle = servicio.detalle(5L, CORREO_OPERADOR);

        assertThat(detalle.puedeResolver()).isFalse();
        assertThat(detalle.resolucion()).isNotNull();
        assertThat(detalle.resolucion().categoria()).isEqualTo(CategoriaResolucion.INFORMACION_BRINDADA);
        assertThat(detalle.resolucion().nota()).isEqualTo("Se explicó el modo de uso.");
        assertThat(detalle.resolucion().fecha()).isEqualTo(LocalDateTime.of(2026, 11, 3, 16, 30));
        assertThat(detalle.resolucion().resueltaPor()).isEqualTo("Operador Uno");
    }

    @Test
    void elDetalleDeUnCasoResueltoAntesDeLasCategoriasNoFalla() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(5L, Prioridad.BAJA, EstadoPqrs.RESUELTA, null);
        caso.setDescripcionSolucion("Respuesta automática enviada.");
        prepararDetalle(caso, operador);

        DetalleCaso detalle = servicio.detalle(5L, CORREO_OPERADOR);

        assertThat(detalle.resolucion().categoria()).isNull();
        assertThat(detalle.resolucion().resueltaPor()).isNull();
        assertThat(detalle.resolucion().nota()).isEqualTo("Respuesta automática enviada.");
    }

    @Test
    void laBitacoraMuestraLaCategoriaDeLaResolucionYSuNotaInterna() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(5L, Prioridad.MEDIA, EstadoPqrs.RESUELTA, operador);
        prepararDetalle(caso, operador);
        RegistroBitacora registro = new RegistroBitacora();
        registro.setFecha(LocalDateTime.of(2026, 11, 4, 9, 0));
        registro.setAccion(TipoAccion.CASO_RESUELTO);
        registro.setMotivo(CategoriaResolucion.NO_PROCEDE.name());
        registro.setNotasInternas("Fuera de política.");
        registro.setUsuario(operador);
        when(bitacoraRepository.findByPqrsIdOrderByFechaAsc(5L)).thenReturn(List.of(registro));

        DetalleCaso detalle = servicio.detalle(5L, CORREO_OPERADOR);

        assertThat(detalle.bitacora().get(0).motivo()).isEqualTo(CategoriaResolucion.NO_PROCEDE.getEtiqueta());
        assertThat(detalle.bitacora().get(0).notas()).isEqualTo("Fuera de política.");
    }

    // ---------- E5-12: todo cambio de estado deja su registro

    @Test
    void tomarUnCasoCriticoRegistraElCambioDeEstadoConFechaUsuarioYAccion() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.PENDIENTE, null);
        when(pqrsRepository.findByIdConDetalle(2L)).thenReturn(Optional.of(critica));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        servicio.tomarCaso(2L, CORREO_OPERADOR);

        ArgumentCaptor<RegistroBitacora> captor = ArgumentCaptor.forClass(RegistroBitacora.class);
        verify(bitacoraRepository).save(captor.capture());
        RegistroBitacora registro = captor.getValue();
        assertThat(registro.getAccion()).isEqualTo(TipoAccion.CASO_TOMADO);
        assertThat(registro.getUsuario()).isSameAs(operador);
        assertThat(registro.getFecha()).isEqualTo(LocalDateTime.of(2026, 11, 4, 15, 0));
        assertThat(registro.getMotivo()).isEqualTo("Pendiente → Atención inmediata");
        assertThat(registro.getNotasInternas()).isNull();
    }

    @Test
    void tomarUnCasoNoCriticoRegistraElCambioAEnAtencion() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.PENDIENTE, null);
        when(pqrsRepository.findByIdConDetalle(4L)).thenReturn(Optional.of(caso));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        servicio.tomarCaso(4L, CORREO_OPERADOR);

        ArgumentCaptor<RegistroBitacora> captor = ArgumentCaptor.forClass(RegistroBitacora.class);
        verify(bitacoraRepository).save(captor.capture());
        assertThat(captor.getValue().getMotivo()).isEqualTo("Pendiente → En atención");
    }

    @Test
    void tomarUnCasoQueYaNoEstaPendienteNoCambiaElEstadoNiDejaRegistro() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, null);
        when(pqrsRepository.findByIdConDetalle(4L)).thenReturn(Optional.of(caso));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(operador));

        assertThatThrownBy(() -> servicio.tomarCaso(4L, CORREO_OPERADOR)).isInstanceOf(ReglaNegocioException.class);

        assertThat(caso.getEstado()).isEqualTo(EstadoPqrs.EN_ATENCION);
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void responderCompletarUnPasoYAgregarUnaNotaNoCambianElEstadoDelCaso() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs critica = pqrs(2L, Prioridad.CRITICA, EstadoPqrs.ATENCION_INMEDIATA, operador);
        prepararPaso(critica, operador);

        servicio.enviarRespuesta(2L, CORREO_OPERADOR, "Hola", "Texto");
        servicio.completarPaso(2L, CORREO_OPERADOR, PasoProtocolo.SOLICITAR_LOTE);
        servicio.agregarNotaInterna(2L, CORREO_OPERADOR, "Nota");

        assertThat(critica.getEstado()).isEqualTo(EstadoPqrs.ATENCION_INMEDIATA);
        verify(pqrsRepository, never()).save(any());
    }

    // ---------- E5-11: notas internas

    @Test
    void agregarUnaNotaInternaLaGuardaSoloEnLaBitacoraYNoLaEnviaALaPersonaCliente() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);
        prepararNota(caso, operador);

        servicio.agregarNotaInterna(4L, CORREO_OPERADOR, "  Llamar a la persona cliente mañana.  ");

        ArgumentCaptor<RegistroBitacora> captor = ArgumentCaptor.forClass(RegistroBitacora.class);
        verify(bitacoraRepository).save(captor.capture());
        RegistroBitacora registro = captor.getValue();
        assertThat(registro.getAccion()).isEqualTo(TipoAccion.NOTA_INTERNA);
        assertThat(registro.getNotasInternas()).isEqualTo("Llamar a la persona cliente mañana.");
        assertThat(registro.getMotivo()).isNull();
        assertThat(registro.getUsuario()).isSameAs(operador);
        assertThat(registro.getPqrs()).isSameAs(caso);
        assertThat(registro.getFecha()).isEqualTo(LocalDateTime.of(2026, 11, 4, 15, 0));
        assertThat(caso.getEstado()).isEqualTo(EstadoPqrs.EN_ATENCION);
        verifyNoInteractions(canal, mensajeRepository);
    }

    @Test
    void rechazaUnaNotaInternaVaciaOSoloConEspacios() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);
        prepararNota(caso, operador);

        assertThatThrownBy(() -> servicio.agregarNotaInterna(4L, CORREO_OPERADOR, "   "))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La nota interna es obligatoria para esta acción");
        assertThatThrownBy(() -> servicio.agregarNotaInterna(4L, CORREO_OPERADOR, null))
                .isInstanceOf(ReglaNegocioException.class);
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void aceptaUnaNotaDeExactamenteElLargoMaximoYRechazaUnaMasLarga() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);
        prepararNota(caso, operador);

        servicio.agregarNotaInterna(4L, CORREO_OPERADOR, "a".repeat(PqrsService.LARGO_MAXIMO_NOTA));
        verify(bitacoraRepository).save(any(RegistroBitacora.class));

        assertThatThrownBy(() -> servicio.agregarNotaInterna(4L, CORREO_OPERADOR, "a".repeat(PqrsService.LARGO_MAXIMO_NOTA + 1)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("2000");
    }

    @Test
    void rechazaAgregarUnaNotaSiElOperadorEstaBloqueado() {
        Usuario bloqueado = usuario(3L, Rol.OPERADOR, true);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, bloqueado);
        prepararNota(caso, bloqueado);

        assertThatThrownBy(() -> servicio.agregarNotaInterna(4L, CORREO_OPERADOR, "Nota"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("bloqueado");
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void rechazaAgregarUnaNotaSiElCasoNoSeHaTomado() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.PENDIENTE, null);
        prepararNota(caso, operador);

        assertThatThrownBy(() -> servicio.agregarNotaInterna(4L, CORREO_OPERADOR, "Nota"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Toma el caso antes de agregar notas internas");
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void rechazaAgregarUnaNotaSiElCasoYaFueResuelto() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(3L, Prioridad.MEDIA, EstadoPqrs.RESUELTA, operador);
        prepararNota(caso, operador);

        assertThatThrownBy(() -> servicio.agregarNotaInterna(3L, CORREO_OPERADOR, "Nota"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El caso ya fue resuelto");
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void unOperadorNoPuedeAgregarNotasAUnCasoQueAtiendeOtraPersona() {
        Usuario otro = usuario(3L, Rol.OPERADOR, false);
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, otro);
        prepararNota(caso, operador);

        assertThatThrownBy(() -> servicio.agregarNotaInterna(4L, CORREO_OPERADOR, "Nota"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El caso lo atiende otra persona");
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void unAdminSiPuedeAgregarNotasAUnCasoQueAtiendeUnOperador() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Usuario admin = usuario(1L, Rol.ADMIN, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);
        prepararNota(caso, admin);

        servicio.agregarNotaInterna(4L, CORREO_OPERADOR, "Revisado por administración");

        ArgumentCaptor<RegistroBitacora> captor = ArgumentCaptor.forClass(RegistroBitacora.class);
        verify(bitacoraRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isSameAs(admin);
    }

    @Test
    void lasAccionesQueExigenNotaNoSeRegistranSinNotaYNoDejanRastro() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);

        for (TipoAccion accion : new TipoAccion[] {TipoAccion.CASO_RESUELTO, TipoAccion.ESCALADA}) {
            assertThatThrownBy(() -> servicio.registrar(caso, operador, accion, null, null))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La nota interna es obligatoria para esta acción");
            assertThatThrownBy(() -> servicio.registrar(caso, operador, accion, "Motivo", "  "))
                    .isInstanceOf(ReglaNegocioException.class);
        }
        verify(bitacoraRepository, never()).save(any());
    }

    @Test
    void lasAccionesQueExigenNotaSeRegistranCuandoTraenNota() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);

        servicio.registrar(caso, operador, TipoAccion.CASO_RESUELTO, "En atención → Resuelta", " Se reenvió el pedido. ");

        ArgumentCaptor<RegistroBitacora> captor = ArgumentCaptor.forClass(RegistroBitacora.class);
        verify(bitacoraRepository).save(captor.capture());
        assertThat(captor.getValue().getAccion()).isEqualTo(TipoAccion.CASO_RESUELTO);
        assertThat(captor.getValue().getNotasInternas()).isEqualTo("Se reenvió el pedido.");
        assertThat(captor.getValue().getMotivo()).isEqualTo("En atención → Resuelta");
    }

    @Test
    void lasAccionesQueNoExigenNotaSeRegistranSinNota() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);

        servicio.registrar(caso, operador, TipoAccion.RESPUESTA_ENVIADA, null, null);

        ArgumentCaptor<RegistroBitacora> captor = ArgumentCaptor.forClass(RegistroBitacora.class);
        verify(bitacoraRepository).save(captor.capture());
        assertThat(captor.getValue().getNotasInternas()).isNull();
    }

    @Test
    void elDetalleMuestraLaNotaEnLaBitacoraPeroNoEnLaConversacion() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs caso = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);
        prepararDetalle(caso, operador);
        RegistroBitacora nota = new RegistroBitacora();
        nota.setFecha(LocalDateTime.of(2026, 11, 4, 11, 0));
        nota.setAccion(TipoAccion.NOTA_INTERNA);
        nota.setNotasInternas("Confirmar el lote con bodega.");
        nota.setUsuario(operador);
        when(bitacoraRepository.findByPqrsIdOrderByFechaAsc(4L)).thenReturn(List.of(nota));

        DetalleCaso detalle = servicio.detalle(4L, CORREO_OPERADOR);

        assertThat(detalle.bitacora()).extracting(DetalleCaso.RegistroVista::notas).containsExactly("Confirmar el lote con bodega.");
        assertThat(detalle.mensajes()).extracting(DetalleCaso.MensajeVista::cuerpo)
                .doesNotContain("Confirmar el lote con bodega.");
    }

    @Test
    void elDetalleIndicaSiElOperadorPuedeAgregarNotas() {
        Usuario operador = usuario(2L, Rol.OPERADOR, false);
        Pqrs tomado = pqrs(4L, Prioridad.ALTA, EstadoPqrs.EN_ATENCION, operador);
        prepararDetalle(tomado, operador);

        DetalleCaso conPermiso = servicio.detalle(4L, CORREO_OPERADOR);

        assertThat(conPermiso.puedeAgregarNota()).isTrue();
        assertThat(conPermiso.motivoNotaBloqueada()).isNull();

        Pqrs sinTomar = pqrs(5L, Prioridad.ALTA, EstadoPqrs.PENDIENTE, null);
        prepararDetalle(sinTomar, operador);

        DetalleCaso sinPermiso = servicio.detalle(5L, CORREO_OPERADOR);

        assertThat(sinPermiso.puedeAgregarNota()).isFalse();
        assertThat(sinPermiso.motivoNotaBloqueada()).isEqualTo("Toma el caso antes de agregar notas internas");
    }

    // ---------- ayudas

    private void prepararNota(Pqrs pqrs, Usuario usuario) {
        when(pqrsRepository.findByIdConDetalle(pqrs.getId())).thenReturn(Optional.of(pqrs));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(usuario));
    }

    private void prepararPaso(Pqrs pqrs, Usuario usuario, RegistroBitacora... completados) {
        when(pqrsRepository.findByIdConDetalle(pqrs.getId())).thenReturn(Optional.of(pqrs));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(usuario));
        when(bitacoraRepository.findByPqrsIdAndAccionOrderByFechaAsc(pqrs.getId(), TipoAccion.PASO_PROTOCOLO_COMPLETADO))
                .thenReturn(List.of(completados));
    }

    private void prepararResolver(Pqrs pqrs, Usuario usuario) {
        when(pqrsRepository.findByIdConDetalle(pqrs.getId())).thenReturn(Optional.of(pqrs));
        when(usuarioRepository.findByCorreo(CORREO_OPERADOR)).thenReturn(Optional.of(usuario));
    }

    private RegistroBitacora[] todosLosPasos(Usuario usuario) {
        PasoProtocolo[] pasos = PasoProtocolo.values();
        RegistroBitacora[] registros = new RegistroBitacora[pasos.length];
        for (int i = 0; i < pasos.length; i++) {
            registros[i] = registroPaso(pasos[i], usuario);
        }
        return registros;
    }

    private RegistroBitacora registroPaso(PasoProtocolo paso, Usuario usuario) {
        RegistroBitacora registro = new RegistroBitacora();
        registro.setFecha(LocalDateTime.of(2026, 11, 4, 10, 0));
        registro.setAccion(TipoAccion.PASO_PROTOCOLO_COMPLETADO);
        registro.setMotivo(paso.name());
        registro.setUsuario(usuario);
        return registro;
    }

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
