package co.edu.unal.apolo.servicios;

import co.edu.unal.apolo.dominio.Pqrs;

/** Fila de la bandeja: la PQRS y los días hábiles que le quedan (negativos si ya venció). */
public record ResumenPqrs(Pqrs pqrs, int diasHabilesRestantes) {

    public boolean vencida() {
        return diasHabilesRestantes < 0;
    }

    public int diasAbsolutos() {
        return Math.abs(diasHabilesRestantes);
    }
}
