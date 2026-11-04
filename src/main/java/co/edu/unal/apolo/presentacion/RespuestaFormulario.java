package co.edu.unal.apolo.presentacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos del formulario para responder a la clienta. Solo se valida el formato; las reglas viven en el servicio. */
public class RespuestaFormulario {

    @Size(max = 250, message = "El asunto no puede superar 250 caracteres")
    private String asunto;

    @NotBlank(message = "Escribe el mensaje antes de enviarlo")
    @Size(max = 4000, message = "El mensaje no puede superar 4000 caracteres")
    private String cuerpo;

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
}
