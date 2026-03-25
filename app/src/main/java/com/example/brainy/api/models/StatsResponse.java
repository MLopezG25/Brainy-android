package com.example.brainy.api.models;

import com.google.gson.annotations.SerializedName;

public class StatsResponse {

    @SerializedName("total_entries")
    private int totalEntries;

    @SerializedName("pendiente")
    private int pendiente;

    @SerializedName("en_progreso")
    private int enProgreso;

    @SerializedName("completado")
    private int completado;

    @SerializedName("abandonado")
    private int abandonado;

    @SerializedName("categories")
    private int categories;

    @SerializedName("subcategories")
    private int subcategories;

    public StatsResponse() {}

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    public int getPendiente() {
        return pendiente;
    }

    public void setPendiente(int pendiente) {
        this.pendiente = pendiente;
    }

    public int getEnProgreso() {
        return enProgreso;
    }

    public void setEnProgreso(int enProgreso) {
        this.enProgreso = enProgreso;
    }

    public int getCompletado() {
        return completado;
    }

    public void setCompletado(int completado) {
        this.completado = completado;
    }

    public int getAbandonado() {
        return abandonado;
    }

    public void setAbandonado(int abandonado) {
        this.abandonado = abandonado;
    }

    public int getCategories() {
        return categories;
    }

    public void setCategories(int categories) {
        this.categories = categories;
    }

    public int getSubcategories() {
        return subcategories;
    }

    public void setSubcategories(int subcategories) {
        this.subcategories = subcategories;
    }
}
