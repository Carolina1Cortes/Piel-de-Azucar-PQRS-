package co.edu.unal.apolo.presentacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos del formulario que simula la llegada de un correo de una clienta. */
public class CorreoSimuladoFormulario {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 255, message = "El correo no puede superar 255 caracteres")
    private String correo;

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 300, message = "El asunto no puede superar 300 caracteres")
    private String asunto;

    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 4000, message = "El mensaje no puede superar 4000 caracteres")
    private String cuerpo;

    @NotNull(message = "Elige un tipo de consulta")
    private Long tipoConsultaId;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public void setCuerpo(String cuerpo) {
        this.cuerpo = cuerpo;
    }

    public Long getTipoConsultaId() {
        return tipoConsultaId;
    }

    public void setTipoConsultaId(Long tipoConsultaId) {
        this.tipoConsultaId = tipoConsultaId;
    }
}
