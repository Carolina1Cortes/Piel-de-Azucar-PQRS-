package co.edu.unal.apolo.servicios;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

import co.edu.unal.apolo.configuracion.AdjuntosPropiedades;
import co.edu.unal.apolo.dominio.Adjunto;
import co.edu.unal.apolo.dominio.Canal;
import co.edu.unal.apolo.dominio.Mensaje;
import co.edu.unal.apolo.integracion.AlmacenArchivos;
import co.edu.unal.apolo.persistencia.AdjuntoRepository;

@ExtendWith(MockitoExtension.class)
class AdjuntoServiceTest {

    private static final byte[] FIRMA_JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] FIRMA_PDF = {'%', 'P', 'D', 'F', '-'};

    @Mock private AdjuntoRepository adjuntoRepository;
    @Mock private AlmacenArchivos almacen;

    private AdjuntoService servicio;

    @BeforeEach
    void preparar() {
        AdjuntosPropiedades propiedades = new AdjuntosPropiedades(Map.of("gmail", DataSize.ofKilobytes(100)));
        servicio = new AdjuntoService(adjuntoRepository, almacen, propiedades);
    }

    // ---------- validar

    @Test
    void aceptaJpgPngYPdfDentroDelLimiteDelCanal() {
        List<ArchivoAdjunto> archivos = List.of(
                archivo("foto.jpg", FIRMA_JPG, 10),
                archivo("captura.PNG", FIRMA_PNG, 10),
                archivo("guia_modo_de_uso.pdf", FIRMA_PDF, 10));

        assertThatCode(() -> servicio.validar(archivos, Canal.GMAIL)).doesNotThrowAnyException();
    }

    @Test
    void sinAdjuntosNoValidaNada() {
        assertThatCode(() -> servicio.validar(null, Canal.GMAIL)).doesNotThrowAnyException();
        assertThatCode(() -> servicio.validar(List.of(), Canal.GMAIL)).doesNotThrowAnyException();
    }

    @Test
    void rechazaUnTipoDeArchivoNoPermitido() {
        List<ArchivoAdjunto> archivos = List.of(archivo("contrato.docx", "PK".getBytes(), 10));

        assertThatThrownBy(() -> servicio.validar(archivos, Canal.GMAIL))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("JPG, PNG o PDF")
                .hasMessageContaining("contrato.docx");
    }

    @Test
    void rechazaUnArchivoConExtensionPermitidaPeroContenidoDeOtroTipo() {
        List<ArchivoAdjunto> archivos = List.of(archivo("guia.pdf", "MZ".getBytes(), 10));

        assertThatThrownBy(() -> servicio.validar(archivos, Canal.GMAIL))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("guia.pdf");
    }

    @Test
    void rechazaUnArchivoVacio() {
        List<ArchivoAdjunto> archivos = List.of(new ArchivoAdjunto("foto.jpg", new byte[0]));

        assertThatThrownBy(() -> servicio.validar(archivos, Canal.GMAIL))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void rechazaUnArchivoQueSuperaElTamanoMaximoDelCanal() {
        List<ArchivoAdjunto> archivos = List.of(archivo("ficha_tecnica.pdf", FIRMA_PDF, 101 * 1024));

        assertThatThrownBy(() -> servicio.validar(archivos, Canal.GMAIL))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ficha_tecnica.pdf")
                .hasMessageContaining("Gmail")
                .hasMessageContaining("100 KB");
    }

    @Test
    void rechazaAdjuntosQueJuntosSuperanElTamanoMaximoDelCanal() {
        List<ArchivoAdjunto> archivos = List.of(
                archivo("parte1.pdf", FIRMA_PDF, 60 * 1024),
                archivo("parte2.pdf", FIRMA_PDF, 60 * 1024));

        assertThatThrownBy(() -> servicio.validar(archivos, Canal.GMAIL))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("suman");
    }

    @Test
    void aceptaUnArchivoExactamenteDelTamanoMaximo() {
        List<ArchivoAdjunto> archivos = List.of(archivo("guia.pdf", FIRMA_PDF, 100 * 1024));

        assertThatCode(() -> servicio.validar(archivos, Canal.GMAIL)).doesNotThrowAnyException();
    }

    @Test
    void rechazaMasDeCincoAdjuntos() {
        ArchivoAdjunto foto = archivo("foto.jpg", FIRMA_JPG, 10);
        List<ArchivoAdjunto> archivos = List.of(foto, foto, foto, foto, foto, foto);

        assertThatThrownBy(() -> servicio.validar(archivos, Canal.GMAIL))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("5");
    }

    @Test
    void usaElTamanoPorDefectoSiElCanalNoTieneConfiguracion() {
        AdjuntoService sinConfiguracion = new AdjuntoService(adjuntoRepository, almacen, new AdjuntosPropiedades(null));
        List<ArchivoAdjunto> archivos = List.of(archivo("guia.pdf", FIRMA_PDF, 200 * 1024));

        assertThatCode(() -> sinConfiguracion.validar(archivos, Canal.GMAIL)).doesNotThrowAnyException();
        assertThat(sinConfiguracion.tamanoMaximoLegible(Canal.GMAIL)).isEqualTo("5 MB");
        assertThat(sinConfiguracion.tamanoMaximoLegible(null)).isEqualTo("5 MB");
    }

    @Test
    void muestraElTamanoMaximoDelCanalEnUnidadesLegibles() {
        assertThat(servicio.tamanoMaximoLegible(Canal.GMAIL)).isEqualTo("100 KB");
        assertThat(AdjuntoService.legible(DataSize.ofMegabytes(10).toBytes())).isEqualTo("10 MB");
        assertThat(AdjuntoService.legible(300)).isEqualTo("1 KB");
        assertThat(AdjuntoService.legible(1536 * 1024)).startsWith("1").endsWith("5 MB");
    }

    // ---------- guardar

    @Test
    void guardaCadaArchivoConNombreUnicoYLoLigaAlMensaje() {
        Mensaje mensaje = new Mensaje();
        when(almacen.guardar(eq(AdjuntoService.CARPETA_MENSAJES), anyString(), any()))
                .thenAnswer(invocacion -> AdjuntoService.CARPETA_MENSAJES + "/" + invocacion.getArgument(1));
        when(adjuntoRepository.save(any(Adjunto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        List<Adjunto> adjuntos = servicio.guardar(mensaje, List.of(
                archivo("guia_modo_de_uso.pdf", FIRMA_PDF, 10),
                archivo("foto.jpeg", FIRMA_JPG, 10)));

        assertThat(adjuntos).hasSize(2);
        Adjunto guia = adjuntos.get(0);
        assertThat(guia.getNombreArchivo()).isEqualTo("guia_modo_de_uso.pdf");
        assertThat(guia.getTipoMime()).isEqualTo("application/pdf");
        assertThat(guia.getMensaje()).isSameAs(mensaje);
        assertThat(guia.getPlantilla()).isNull();
        assertThat(guia.getRuta()).startsWith("adjuntos/mensajes/").endsWith(".pdf")
                .doesNotContain("guia_modo_de_uso");
        assertThat(adjuntos.get(1).getTipoMime()).isEqualTo("image/jpeg");
        assertThat(adjuntos.get(1).getRuta()).endsWith(".jpg").isNotEqualTo(guia.getRuta());
    }

    @Test
    void guardaSoloElNombreSinLaRutaQueEnvianAlgunosNavegadores() {
        when(almacen.guardar(anyString(), anyString(), any())).thenReturn("adjuntos/mensajes/x.pdf");
        when(adjuntoRepository.save(any(Adjunto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        List<Adjunto> adjuntos = servicio.guardar(new Mensaje(),
                List.of(archivo("C:\\Users\\agente\\Documentos\\guia.pdf", FIRMA_PDF, 10)));

        assertThat(adjuntos.get(0).getNombreArchivo()).isEqualTo("guia.pdf");
    }

    @Test
    void recortaLosNombresMuyLargosConservandoLaExtension() {
        String largo = "a".repeat(300) + ".pdf";

        String limpio = AdjuntoService.nombreLimpio(largo);

        assertThat(limpio).hasSize(255).endsWith(".pdf");
        assertThat(AdjuntoService.nombreLimpio(null)).isEqualTo("sin_nombre");
        assertThat(AdjuntoService.nombreLimpio("carpeta/")).isEqualTo("sin_nombre");
    }

    @Test
    void sinAdjuntosNoGuardaNada() {
        assertThat(servicio.guardar(new Mensaje(), List.of())).isEmpty();
        assertThat(servicio.guardar(new Mensaje(), null)).isEmpty();
        verify(adjuntoRepository, never()).save(any());
    }

    @Test
    void siNoSePuedeEscribirElArchivoLanzaReglaDeNegocio() {
        when(almacen.guardar(anyString(), anyString(), any()))
                .thenThrow(new UncheckedIOException(new IOException("disco lleno")));

        assertThatThrownBy(() -> servicio.guardar(new Mensaje(), List.of(archivo("guia.pdf", FIRMA_PDF, 10))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("guia.pdf");
        verify(adjuntoRepository, never()).save(any());
    }

    @Test
    void guardarRechazaUnArchivoNoPermitidoAunqueNoSeHayaValidado() {
        assertThatThrownBy(() -> servicio.guardar(new Mensaje(), List.of(archivo("virus.exe", "MZ".getBytes(), 10))))
                .isInstanceOf(ReglaNegocioException.class);
        verify(almacen, never()).guardar(anyString(), anyString(), any());
    }

    // ---------- consultar

    @Test
    void agrupaLosNombresDeLosAdjuntosPorMensaje() {
        Mensaje primero = mensaje(1L);
        Mensaje segundo = mensaje(2L);
        when(adjuntoRepository.findByMensajePqrsId(7L)).thenReturn(List.of(
                adjunto("foto.jpg", primero), adjunto("guia.pdf", segundo), adjunto("ficha.pdf", segundo)));

        Map<Long, List<String>> nombres = servicio.nombresPorMensaje(7L);

        assertThat(nombres).containsOnlyKeys(1L, 2L);
        assertThat(nombres.get(1L)).containsExactly("foto.jpg");
        assertThat(nombres.get(2L)).containsExactly("guia.pdf", "ficha.pdf");
    }

    @Test
    void verificaLaFirmaAunqueLaExtensionVengaEnMayusculas() {
        ArgumentCaptor<byte[]> contenido = ArgumentCaptor.forClass(byte[].class);
        when(almacen.guardar(anyString(), anyString(), contenido.capture())).thenReturn("adjuntos/mensajes/x.png");
        when(adjuntoRepository.save(any(Adjunto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        ArchivoAdjunto png = archivo("CAPTURA.PNG", FIRMA_PNG, 20);

        servicio.guardar(new Mensaje(), List.of(png));

        assertThat(contenido.getValue()).isEqualTo(png.contenido());
    }

    // ---------- utilidades

    private static ArchivoAdjunto archivo(String nombre, byte[] firma, int tamano) {
        byte[] contenido = Arrays.copyOf(firma, Math.max(tamano, firma.length));
        return new ArchivoAdjunto(nombre, contenido);
    }

    private static Mensaje mensaje(Long id) {
        Mensaje mensaje = new Mensaje();
        mensaje.setId(id);
        return mensaje;
    }

    private static Adjunto adjunto(String nombre, Mensaje mensaje) {
        Adjunto adjunto = new Adjunto();
        adjunto.setNombreArchivo(nombre);
        adjunto.setMensaje(mensaje);
        return adjunto;
    }
}
