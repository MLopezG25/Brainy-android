package com.example.brainy.utils;

import com.example.brainy.R;

/**
 * Centraliza toda la lógica relacionada con los estados de entrada:
 * pendiente, en_progreso, completado, abandonado.
 */
public final class StatusUtils {

    private StatusUtils() {}

    public static final String PENDIENTE = "pendiente";
    public static final String EN_PROGRESO = "en_progreso";
    public static final String COMPLETADO = "completado";
    public static final String ABANDONADO = "abandonado";

    /** Convierte texto de UI (spinner) a clave interna. */
    public static String toKey(String display) {
        switch (display) {
            case "Pendiente": return PENDIENTE;
            case "En progreso": return EN_PROGRESO;
            case "Completado": return COMPLETADO;
            case "Abandonado": return ABANDONADO;
            default: return null;
        }
    }

    /** Convierte clave interna a texto para UI. */
    public static String toDisplay(String key) {
        if (key == null) return "Desconocido";
        switch (key) {
            case PENDIENTE: return "Pendiente";
            case EN_PROGRESO: return "En progreso";
            case COMPLETADO: return "Completado";
            case ABANDONADO: return "Abandonado";
            default: return key;
        }
    }

    /** Icono asociado a cada estado. */
    public static int getIconRes(String key) {
        switch (key != null ? key : "") {
            case PENDIENTE: return R.drawable.ic_status_pending;
            case EN_PROGRESO: return R.drawable.ic_status_progress;
            case COMPLETADO: return R.drawable.ic_status_completed;
            case ABANDONADO: return R.drawable.ic_status_abandoned;
            default: return R.drawable.ic_status_pending;
        }
    }

    /** Lista de etiquetas usadas en spinners de filtro. */
    public static String[] getFilterLabels() {
        return new String[]{"Estado", "Pendiente", "En progreso", "Completado", "Abandonado"};
    }

    /** Lista de etiquetas usadas en dropdowns de formulario (sin "Estado" neutro). */
    public static String[] getFormLabels() {
        return new String[]{"Pendiente", "En progreso", "Completado", "Abandonado"};
    }
}