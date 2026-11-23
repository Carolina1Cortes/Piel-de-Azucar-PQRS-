package co.edu.unal.apolo.presentacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos del formulario de nota interna. Solo se valida el formato; las reglas viven en el servicio. */
public class NotaInternaFormulario {

    @NotBlank(message = "Escribe la nota antes de guardarla")
    @Size(max = 2000, message = "La nota no puede superar 2000 caracteres")
    private String nota;

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }
}
