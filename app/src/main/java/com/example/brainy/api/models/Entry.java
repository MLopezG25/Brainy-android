package com.example.brainy.api.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class Entry {

    @SerializedName("id")
    private int id;

    @SerializedName("user")
    private int userId;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("category")
    private int categoryId;

    @SerializedName("category_name")
    private String categoryName;

    @SerializedName("subcategory")
    private int subcategoryId;

    @SerializedName("subcategory_name")
    private String subcategoryName;

    @SerializedName("tags")
    private List<Tag> tags;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("completed_date")
    private String completedDate;

    @SerializedName("field_values")
    private List<Map<String, Object>> fieldValues;

    public Entry() {}

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getSubcategoryId() {
        return subcategoryId;
    }

    public void setSubcategoryId(int subcategoryId) {
        this.subcategoryId = subcategoryId;
    }

    public String getSubcategoryName() {
        return subcategoryName;
    }

    public void setSubcategoryName(String subcategoryName) {
        this.subcategoryName = subcategoryName;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Map<String, Object>> getFieldValues() {
        return fieldValues;
    }

    public void setFieldValues(List<Map<String, Object>> fieldValues) {
        this.fieldValues = fieldValues;
    }

    public String getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(String completedDate) {
        this.completedDate = completedDate;
    }

    public String getStatusDisplay() {
        switch (status) {
            case "pendiente":
                return "Pendiente";
            case "en_progreso":
                return "En progreso";
            case "completado":
                return "Completado";
            case "abandonado":
                return "Abandonado";
            default:
                return status;
        }
    }
}
