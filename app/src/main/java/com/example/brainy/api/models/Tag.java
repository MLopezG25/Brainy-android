package com.example.brainy.api.models;

import com.google.gson.annotations.SerializedName;

public class Tag {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    public Tag() {}

    public Tag(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
