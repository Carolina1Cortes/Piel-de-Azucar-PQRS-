package co.edu.unal.apolo.presentacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unal.apolo.dominio.Canal;
import co.edu.unal.apolo.dominio.CategoriaResolucion;
import co.edu.unal.apolo.dominio.Categoria;
import co.edu.unal.apolo.dominio.Cliente;
import co.edu.unal.apolo.dominio.DireccionMensaje;
import co.edu.unal.apolo.dominio.EstadoPqrs;
import co.edu.unal.apolo.dominio.PasoProtocolo;
import co.edu.unal.apolo.dominio.Pqrs;
import co.edu.unal.apolo.dominio.Prioridad;
import co.edu.unal.apolo.dominio.TipoAccion;
import co.edu.unal.apolo.dominio.TipoConsulta;
import co.edu.unal.apolo.servicios.ArchivoAdjunto;
import co.edu.unal.apolo.servicios.DetalleCaso;
import co.edu.unal.apolo.servicios.DetalleCaso.MensajeVista;
import co.edu.unal.apolo.servicios.DetalleCaso.PasoVista;
import co.edu.unal.apolo.servicios.DetalleCaso.ResolucionVista;
import co.edu.unal.apolo.servicios.DetalleCaso.RegistroVista;
import co.edu.unal.apolo.servicios.PqrsService;
import co.edu.unal.apolo.servicios.RecursoNoEncontradoException;
import co.edu.unal.apolo.servicios.ReglaNegocioException;

@WebMvcTest(CasoController.class)
@WithMockUser(username = "operador1@pieldeazucar.co")
class CasoControllerTest {

    private static final String CORREO = "operador1@pieldeazucar.co";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PqrsService pqrsService;

    @Test
    void unCasoCriticoSinAsignarMuestraElBannerYTieneElEnvioDeshabilitado() throws Exception {
        when(pqrsService.detalle(10L, CORREO)).thenReturn(
                detalle(true, true, false, "Caso crítico: toma el caso antes de responder"));

        mockMvc.perform(get("/pqrs/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("caso"))
                .andExpect(content().string(containsString("Automatización pausada")))
                .andExpect(content().string(containsString("Tomar caso")))
                .andExpect(content().string(containsString("toma el caso antes de responder")))
                .andExpect(content().string(containsString("disabled")))
                .andExpect(content().string(containsString("Valentina Ortiz")))
                .andExpect(content().string(containsString("me salió enrojecimiento")));
    }

    @Test
    void unCasoTomadoNoMuestraElBannerYHabilitaElEnvio() throws Exception {
        when(pqrsService.detalle(10L, CORREO)).thenReturn(detalle(false, false, true, null));

        mockMvc.perform(get("/pqrs/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Automatización pausada"))))
                .andExpect(content().string(not(containsString("Tomar caso"))))
                .andExpect(content().string(not(containsString("disabled"))))
                .andExpect(content().string(containsString("Enviar respuesta")));
    }

    @Test
    void unCasoInexistenteDevuelve404() throws Exception {
        when(pqrsService.detalle(eq(404L), any())).thenThrow(new RecursoNoEncontradoException("PQRS", 404L));

        mockMvc.perform(get("/pqrs/404"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("no-encontrado"));
    }

    @Test
    void tomarElCasoLlamaAlServicioConElOperadorDeLaSesionYVuelveAlCaso() throws Exception {
        mockMvc.perform(post("/pqrs/10/tomar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attributeExists("mensajeOk"));

        verify(pqrsService).tomarCaso(10L, CORREO);
    }

    @Test
    void siNoSePuedeTomarElCasoSeMuestraElMotivo() throws Exception {
        doThrow(new ReglaNegocioException("El caso ya tiene responsable")).when(pqrsService).tomarCaso(10L, CORREO);

        mockMvc.perform(post("/pqrs/10/tomar").with(csrf()))
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attribute("mensajeError", "El caso ya tiene responsable"));
    }

    @Test
    void tomarSinTokenCsrfEsRechazado() throws Exception {
        mockMvc.perform(post("/pqrs/10/tomar"))
                .andExpect(status().isForbidden());

        verify(pqrsService, never()).tomarCaso(any(), any());
    }

    @Test
    void responderEnviaElMensajeYVuelveAlCaso() throws Exception {
        mockMvc.perform(post("/pqrs/10/responder").with(csrf())
                        .param("asunto", "Sobre tu reacción")
                        .param("cuerpo", "Hola Valentina, lamentamos lo ocurrido."))
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attributeExists("mensajeOk"));

        verify(pqrsService).enviarRespuesta(10L, CORREO, "Sobre tu reacción", "Hola Valentina, lamentamos lo ocurrido.",
                List.of());
    }

    @Test
    void responderConElMensajeVacioVuelveAlFormularioConElError() throws Exception {
        when(pqrsService.detalle(10L, CORREO)).thenReturn(detalle(false, false, true, null));

        mockMvc.perform(post("/pqrs/10/responder").with(csrf()).param("cuerpo", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("caso"))
                .andExpect(model().attributeHasFieldErrors("formulario", "cuerpo"));

        verify(pqrsService, never()).enviarRespuesta(any(), any(), any(), any(), any());
    }

    @Test
    void siElServicioRechazaElEnvioSeMuestraElMotivo() throws Exception {
        doThrow(new ReglaNegocioException("Caso crítico: toma el caso antes de responder"))
                .when(pqrsService).enviarRespuesta(10L, CORREO, null, "Hola", List.of());

        mockMvc.perform(post("/pqrs/10/responder").with(csrf()).param("cuerpo", "Hola"))
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attribute("mensajeError", "Caso crítico: toma el caso antes de responder"));
    }

    @Test
    void completarUnPasoDelProtocoloLlamaAlServicioYVuelveAlCaso() throws Exception {
        mockMvc.perform(post("/pqrs/10/protocolo/SOLICITAR_LOTE").with(csrf()))
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attributeExists("mensajeOk"));

        verify(pqrsService).completarPaso(10L, CORREO, PasoProtocolo.SOLICITAR_LOTE);
    }

    @Test
    void siCompletarElPasoNoEstaPermitidoMuestraElMotivo() throws Exception {
        doThrow(new ReglaNegocioException("Ese paso ya se completó"))
                .when(pqrsService).completarPaso(10L, CORREO, PasoProtocolo.SUSPENDER_USO);

        mockMvc.perform(post("/pqrs/10/protocolo/SUSPENDER_USO").with(csrf()))
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attribute("mensajeError", "Ese paso ya se completó"));
    }

    @Test
    void unCasoCriticoMuestraLaSeccionDelProtocolo() throws Exception {
        DetalleCaso base = detalle(false, false, true, null);
        List<PasoVista> protocolo = List.of(
                new PasoVista(PasoProtocolo.SUSPENDER_USO, true, LocalDateTime.of(2026, 11, 4, 9, 30), "Operador Uno"),
                new PasoVista(PasoProtocolo.SOLICITAR_FOTOS, false, null, null));
        when(pqrsService.detalle(10L, CORREO)).thenReturn(new DetalleCaso(base.pqrs(), base.mensajes(), base.bitacora(),
                base.diasHabilesRestantes(), false, false, true, null, protocolo, true,
                false, "Toma el caso antes de resolverlo", null, null));

        mockMvc.perform(get("/pqrs/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Protocolo crítico")))
                .andExpect(content().string(containsString("✔ " + PasoProtocolo.SUSPENDER_USO.getEtiqueta()
                        + " · Operador Uno · 04/11/2026 09:30")))
                .andExpect(content().string(containsString(PasoProtocolo.SOLICITAR_FOTOS.getEtiqueta())))
                .andExpect(content().string(containsString("/pqrs/10/protocolo/SOLICITAR_FOTOS")))
                .andExpect(content().string(containsString("Marcar completado")))
                .andExpect(content().string(not(containsString("Toma el caso para completar los pasos."))));
    }

    @Test
    void resolverLlamaAlServicioConLosDatosDelFormularioYVuelveAlCaso() throws Exception {
        mockMvc.perform(post("/pqrs/10/resolver").with(csrf())
                        .param("categoria", "SOLUCIONADO")
                        .param("nota", "Se envió el cambio."))
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attributeExists("mensajeOk"));

        verify(pqrsService).resolverCaso(10L, CORREO, CategoriaResolucion.SOLUCIONADO, "Se envió el cambio.");
    }

    @Test
    void resolverSinCategoriaVuelveAlFormularioConElError() throws Exception {
        when(pqrsService.detalle(10L, CORREO)).thenReturn(detalleParaResolver(true, null, null));

        mockMvc.perform(post("/pqrs/10/resolver").with(csrf()).param("nota", "Listo"))
                .andExpect(status().isOk())
                .andExpect(view().name("caso"))
                .andExpect(content().string(containsString("Elige la categoría de la resolución")));

        verify(pqrsService, never()).resolverCaso(any(), any(), any(), any());
    }

    @Test
    void resolverSinNotaVuelveAlFormularioConElError() throws Exception {
        when(pqrsService.detalle(10L, CORREO)).thenReturn(detalleParaResolver(true, null, null));

        mockMvc.perform(post("/pqrs/10/resolver").with(csrf()).param("categoria", "NO_PROCEDE").param("nota", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("caso"))
                .andExpect(content().string(containsString("Escribe la nota de resolución")));

        verify(pqrsService, never()).resolverCaso(any(), any(), any(), any());
    }

    @Test
    void siElServicioRechazaResolverSeMuestraElMotivo() throws Exception {
        doThrow(new ReglaNegocioException("Completa el protocolo crítico antes de resolver el caso"))
                .when(pqrsService).resolverCaso(10L, CORREO, CategoriaResolucion.SOLUCIONADO, "Listo");

        mockMvc.perform(post("/pqrs/10/resolver").with(csrf()).param("categoria", "SOLUCIONADO").param("nota", "Listo"))
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attribute("mensajeError", "Completa el protocolo crítico antes de resolver el caso"));
    }

    @Test
    void resolverSinTokenCsrfEsRechazado() throws Exception {
        mockMvc.perform(post("/pqrs/10/resolver").param("categoria", "SOLUCIONADO").param("nota", "Listo"))
                .andExpect(status().isForbidden());

        verify(pqrsService, never()).resolverCaso(any(), any(), any(), any());
    }

    @Test
    void unCasoQuePuedeResolverseMuestraElFormularioConTodasLasCategorias() throws Exception {
        when(pqrsService.detalle(10L, CORREO)).thenReturn(detalleParaResolver(true, null, null));

        mockMvc.perform(get("/pqrs/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Resolver caso")))
                .andExpect(content().string(containsString("/pqrs/10/resolver")))
                .andExpect(content().string(containsString("Categoría de la resolución")))
                .andExpect(content().string(containsString("no la ve la persona cliente")))
                .andExpect(content().string(containsString(CategoriaResolucion.SOLUCIONADO.getEtiqueta())))
                .andExpect(content().string(containsString(CategoriaResolucion.SIN_RESPUESTA.getEtiqueta())))
                .andExpect(model().attributeExists("categoriasResolucion", "resolucionFormulario", "formulario"));
    }

    @Test
    void siNoSePuedeResolverSeExplicaElMotivo() throws Exception {
        when(pqrsService.detalle(10L, CORREO)).thenReturn(
                detalleParaResolver(false, "Completa el protocolo crítico antes de resolver el caso", null));

        mockMvc.perform(get("/pqrs/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Completa el protocolo crítico antes de resolver el caso")));
    }

    @Test
    void unCasoResueltoMuestraLaResolucionYYaNoElFormulario() throws Exception {
        ResolucionVista resolucion = new ResolucionVista(CategoriaResolucion.NO_PROCEDE, "Fuera de la política de cambios.",
                LocalDateTime.of(2026, 11, 4, 16, 45), "Operador Uno");
        when(pqrsService.detalle(10L, CORREO)).thenReturn(detalleParaResolver(false, "El caso ya fue resuelto", resolucion));

        mockMvc.perform(get("/pqrs/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Resolución")))
                .andExpect(content().string(containsString(CategoriaResolucion.NO_PROCEDE.getEtiqueta())))
                .andExpect(content().string(containsString("Fuera de la política de cambios.")))
                .andExpect(content().string(containsString("Operador Uno · 04/11/2026 16:45")))
                .andExpect(content().string(not(containsString("/pqrs/10/resolver"))));
    }

    // ---------- notas internas (E5-11) y bitácora (E5-12)

    @Test
    void agregarUnaNotaLlamaAlServicioConElOperadorDeLaSesionYVuelveAlCaso() throws Exception {
        mockMvc.perform(post("/pqrs/10/notas").with(csrf()).param("nota", "Llamar a la persona cliente."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attributeExists("mensajeOk"));

        verify(pqrsService).agregarNotaInterna(10L, CORREO, "Llamar a la persona cliente.");
    }

    @Test
    void agregarUnaNotaVaciaVuelveAlFormularioConElErrorYNoLlamaAlServicio() throws Exception {
        when(pqrsService.detalle(10L, CORREO)).thenReturn(detalle(false, false, true, null));

        mockMvc.perform(post("/pqrs/10/notas").with(csrf()).param("nota", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("caso"))
                .andExpect(model().attributeHasFieldErrors("notaFormulario", "nota"))
                .andExpect(content().string(containsString("Escribe la nota antes de guardarla")));

        verify(pqrsService, never()).agregarNotaInterna(any(), any(), any());
    }

    @Test
    void agregarUnaNotaDemasiadoLargaVuelveAlFormularioConElError() throws Exception {
        when(pqrsService.detalle(10L, CORREO)).thenReturn(detalle(false, false, true, null));

        mockMvc.perform(post("/pqrs/10/notas").with(csrf()).param("nota", "a".repeat(2001)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("notaFormulario", "nota"));

        verify(pqrsService, never()).agregarNotaInterna(any(), any(), any());
    }

    @Test
    void siElServicioRechazaLaNotaSeMuestraElMotivo() throws Exception {
        doThrow(new ReglaNegocioException("Toma el caso antes de agregar notas internas"))
                .when(pqrsService).agregarNotaInterna(10L, CORREO, "Nota");

        mockMvc.perform(post("/pqrs/10/notas").with(csrf()).param("nota", "Nota"))
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attribute("mensajeError", "Toma el caso antes de agregar notas internas"));
    }

    @Test
    void agregarUnaNotaSinTokenCsrfEsRechazado() throws Exception {
        mockMvc.perform(post("/pqrs/10/notas").param("nota", "Nota"))
                .andExpect(status().isForbidden());

        verify(pqrsService, never()).agregarNotaInterna(any(), any(), any());
    }

    @Test
    void elCasoMuestraElFormularioDeNotaHabilitadoYAvisaQueEsInterna() throws Exception {
        when(pqrsService.detalle(10L, CORREO)).thenReturn(detalle(false, false, true, null));

        mockMvc.perform(get("/pqrs/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Solo la ve el equipo")))
                .andExpect(content().string(containsString("/pqrs/10/notas")))
                .andExpect(content().string(containsString("Guardar nota")))
                .andExpect(content().string(not(containsString("disabled"))));
    }

    @Test
    void siNoSePuedeAgregarNotasElFormularioQuedaDeshabilitadoConElMotivo() throws Exception {
        when(pqrsService.detalle(10L, CORREO)).thenReturn(
                detalle(true, true, false, "Caso crítico: toma el caso antes de responder",
                        "Toma el caso antes de agregar notas internas"));

        mockMvc.perform(get("/pqrs/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Toma el caso antes de agregar notas internas")));
    }

    @Test
    void laBitacoraMuestraElCambioDeEstadoYLaNotaInterna() throws Exception {
        DetalleCaso base = detalle(false, false, true, null);
        List<RegistroVista> bitacora = List.of(
                new RegistroVista(LocalDateTime.of(2026, 11, 4, 9, 0), TipoAccion.CASO_TOMADO, "Operador Uno",
                        "Pendiente → Atención inmediata", null),
                new RegistroVista(LocalDateTime.of(2026, 11, 4, 9, 30), TipoAccion.NOTA_INTERNA, "Operador Uno",
                        null, "Llamar mañana <b>antes</b> de las 10"),
                new RegistroVista(LocalDateTime.of(2026, 11, 4, 9, 40), TipoAccion.RESPUESTA_ENVIADA, "Operador Uno",
                        null, null));
        when(pqrsService.detalle(10L, CORREO)).thenReturn(new DetalleCaso(base.pqrs(), base.mensajes(), bitacora,
                base.diasHabilesRestantes(), false, false, true, null, List.of(), false, false, null, null, null));

        mockMvc.perform(get("/pqrs/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("04/11/2026 09:00")))
                .andExpect(content().string(containsString("Pendiente → Atención inmediata")))
                .andExpect(content().string(containsString("Nota interna: Llamar mañana &lt;b&gt;antes&lt;/b&gt; de las 10")))
                .andExpect(content().string(not(containsString("<b>antes</b>"))));
    }

    private DetalleCaso detalle(boolean criticoSinAsignar, boolean puedeTomar, boolean puedeResponder, String motivo) {
        return detalle(criticoSinAsignar, puedeTomar, puedeResponder, motivo, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void responderConAdjuntosLosPasaAlServicio() throws Exception {
        byte[] pdf = {'%', 'P', 'D', 'F', '-', '1'};
        mockMvc.perform(multipart("/pqrs/10/responder")
                        .file(new MockMultipartFile("adjuntos", "guia_modo_de_uso.pdf", "application/pdf", pdf))
                        .file(new MockMultipartFile("adjuntos", "foto.png", "image/png", new byte[] {1, 2}))
                        .param("cuerpo", "Te comparto la guía de uso.")
                        .with(csrf()))
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attribute("mensajeOk", containsString("2 adjunto(s)")));

        ArgumentCaptor<List<ArchivoAdjunto>> archivos = ArgumentCaptor.forClass(List.class);
        verify(pqrsService).enviarRespuesta(eq(10L), eq(CORREO), any(), eq("Te comparto la guía de uso."),
                archivos.capture());
        assertThat(archivos.getValue()).extracting(ArchivoAdjunto::nombre)
                .containsExactly("guia_modo_de_uso.pdf", "foto.png");
        assertThat(archivos.getValue().get(0).contenido()).isEqualTo(pdf);
    }

    @Test
    void responderSinElegirArchivosIgnoraElCampoVacioDelNavegador() throws Exception {
        mockMvc.perform(multipart("/pqrs/10/responder")
                        .file(new MockMultipartFile("adjuntos", "", "application/octet-stream", new byte[0]))
                        .param("cuerpo", "Hola")
                        .with(csrf()))
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attribute("mensajeOk", "Respuesta enviada por el canal simulado."));

        verify(pqrsService).enviarRespuesta(10L, CORREO, null, "Hola", List.of());
    }

    @Test
    void siUnAdjuntoSuperaElMaximoDelCanalSeMuestraElMotivo() throws Exception {
        doThrow(new ReglaNegocioException("El archivo ficha.pdf pesa 12 MB y el máximo para Gmail es 10 MB"))
                .when(pqrsService).enviarRespuesta(eq(10L), eq(CORREO), any(), eq("Hola"), any());

        mockMvc.perform(multipart("/pqrs/10/responder")
                        .file(new MockMultipartFile("adjuntos", "ficha.pdf", "application/pdf", new byte[] {1}))
                        .param("cuerpo", "Hola")
                        .with(csrf()))
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attribute("mensajeError", "El archivo ficha.pdf pesa 12 MB y el máximo para Gmail es 10 MB"));
    }

    @Test
    void elFormularioDeRespuestaPermiteAdjuntarArchivosYMuestraLosAdjuntosDeLaConversacion() throws Exception {
        DetalleCaso base = detalle(false, false, true, null);
        MensajeVista conAdjunto = new MensajeVista(LocalDateTime.of(2026, 11, 3, 10, 0), DireccionMensaje.ENTRANTE,
                "Me salió enrojecimiento con el sérum", "Adjunto una foto.", false, null, List.of("foto_enrojecimiento.jpg"));
        when(pqrsService.detalle(10L, CORREO)).thenReturn(new DetalleCaso(base.pqrs(), List.of(conAdjunto),
                base.bitacora(), base.diasHabilesRestantes(), false, false, true, null, base.protocolo(), false, true,
                null, null, null, "10 MB"));

        mockMvc.perform(get("/pqrs/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("multipart/form-data")))
                .andExpect(content().string(containsString("type=\"file\"")))
                .andExpect(content().string(containsString("10 MB en total por mensaje")))
                .andExpect(content().string(containsString("foto_enrojecimiento.jpg")));
    }

    private DetalleCaso detalle(boolean criticoSinAsignar, boolean puedeTomar, boolean puedeResponder, String motivo,
                                String motivoNotaBloqueada) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Valentina Ortiz");
        cliente.setCorreo("valentina.ortiz@example.com");
        TipoConsulta tipo = new TipoConsulta();
        tipo.setCategoria(Categoria.REACCIONES);
        tipo.setDescripcion("Enrojecimiento, irritación o alergia");
        Pqrs pqrs = new Pqrs();
        pqrs.setId(10L);
        pqrs.setAsunto("Me salió enrojecimiento con el sérum");
        pqrs.setCanal(Canal.GMAIL);
        pqrs.setEstado(criticoSinAsignar ? EstadoPqrs.PENDIENTE : EstadoPqrs.EN_ATENCION);
        pqrs.setPrioridad(Prioridad.CRITICA);
        pqrs.setFechaLimite(LocalDate.of(2026, 11, 9));
        pqrs.setCliente(cliente);
        pqrs.setTipoConsulta(tipo);
        MensajeVista mensaje = new MensajeVista(LocalDateTime.of(2026, 11, 3, 10, 0), DireccionMensaje.ENTRANTE,
                "Me salió enrojecimiento con el sérum", "Hola, me salió enrojecimiento y ardor.", false, null);
        return new DetalleCaso(pqrs, List.of(mensaje), List.of(), 4, criticoSinAsignar, puedeTomar, puedeResponder, motivo,
                List.of(), false, puedeResponder, puedeResponder ? null : "Toma el caso antes de resolverlo", null,
                motivoNotaBloqueada);
    }

    private DetalleCaso detalleParaResolver(boolean puedeResolver, String motivoNoResolver, ResolucionVista resolucion) {
        DetalleCaso base = detalle(false, false, true, null);
        return new DetalleCaso(base.pqrs(), base.mensajes(), base.bitacora(), base.diasHabilesRestantes(), false, false,
                true, null, base.protocolo(), false, puedeResolver, motivoNoResolver, resolucion, null);
    }
}
