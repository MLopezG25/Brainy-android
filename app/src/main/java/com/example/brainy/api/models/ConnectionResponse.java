package com.example.brainy.api.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class ConnectionResponse {

    @SerializedName("entry_id")
    private int entryId;

    @SerializedName("total")
    private int total;

    @SerializedName("connections")
    private List<Connection> connections;

    public int getEntryId() { return entryId; }
    public int getTotal() { return total; }
    public List<Connection> getConnections() { return connections; }

    public static class Connection {

        @SerializedName("id")
        private int id;

        @SerializedName("entry_from_id")
        private int entryFromId;

        @SerializedName("relation_type")
        private String relationType;

        @SerializedName("label")
        private String label;

        @SerializedName("direction")
        private String direction;

        @SerializedName("related_entry")
        private RelatedEntry relatedEntry;

        public int getId() { return id; }
        public String getRelationType() { return relationType; }
        public String getLabel() { return label; }
        public String getDirection() { return direction; }
        public RelatedEntry getRelatedEntry() { return relatedEntry; }
    }

    public static class RelatedEntry {
        @SerializedName("id")
        private int id;

        @SerializedName("title")
        private String title;

        @SerializedName("category_name")
        private String categoryName;

        @SerializedName("image_url")
        private String imageUrl;

        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getCategoryName() { return categoryName; }
        public String getImageUrl() { return imageUrl; }
    }
}