package co.edu.unal.apolo.presentacion;

import co.edu.unal.apolo.dominio.CategoriaResolucion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos del formulario para resolver un caso. Solo se valida el formato; las reglas viven en el servicio. */
public class ResolucionFormulario {

    @NotNull(message = "Elige la categoría de la resolución")
    private CategoriaResolucion categoria;

    @NotBlank(message = "Escribe la nota de resolución")
    @Size(max = 2000, message = "La nota no puede superar 2000 caracteres")
    private String nota;

    public CategoriaResolucion getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaResolucion categoria) {
        this.categoria = categoria;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }
}
