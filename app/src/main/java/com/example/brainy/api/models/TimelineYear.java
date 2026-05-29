package com.example.brainy.api.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TimelineYear {

    @SerializedName("year")
    private String year;

    @SerializedName("entries")
    private List<Entry> entries;

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }
}
