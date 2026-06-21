package com.example.brainy.api;

import com.example.brainy.api.models.Category;
import com.example.brainy.api.models.ConnectionResponse;
import com.example.brainy.api.models.Entry;
import com.example.brainy.api.models.StatsResponse;
import com.example.brainy.api.models.Subcategory;
import com.example.brainy.api.models.Tag;
import com.example.brainy.api.models.TimelineYear;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/login/")
    Call<Map<String, Object>> login(@Body Map<String, String> credentials);

    @POST("api/register/")
    Call<Map<String, Object>> register(@Body Map<String, String> data);

    @POST("api/logout/")
    Call<Map<String, Object>> logout();

    @GET("api/me/")
    Call<Map<String, Object>> me();

    @GET("api/categories/")
    Call<List<Category>> getCategories();

    @GET("api/categories/{id}/")
    Call<Category> getCategory(@Path("id") int id);

    @GET("api/subcategories/")
    Call<List<Subcategory>> getSubcategories(@Query("category") Integer categoryId);

    @GET("api/subcategories/{id}/")
    Call<Subcategory> getSubcategory(@Path("id") int id);

    @GET("api/entries/")
    Call<List<Entry>> getEntries(
            @Query("search") String search,
            @Query("category") Integer categoryId,
            @Query("status") String status,
            @Query("ordering") String ordering,
            @Query("tags") String tags
    );

    @POST("api/entries/")
    Call<Entry> createEntry(@Body Map<String, Object> entryData);

    @GET("api/entries/{id}/")
    Call<Entry> getEntry(@Path("id") int id);

    @PUT("api/entries/{id}/")
    Call<Entry> updateEntry(@Path("id") int id, @Body Map<String, Object> entryData);

    @DELETE("api/entries/{id}/")
    Call<Void> deleteEntry(@Path("id") int id);

    @GET("api/tags/")
    Call<List<Tag>> getTags();

    @POST("api/tags/")
    Call<Tag> createTag(@Body Map<String, String> tagData);

    @GET("api/timeline/")
    Call<List<TimelineYear>> getTimeline(
            @Query("category") Integer categoryId,
            @Query("status") String status
    );

    @GET("api/stats/")
    Call<StatsResponse> getStats();

    @POST("api/import/")
    Call<Map<String, Object>> importEntries(@Body List<Map<String, Object>> entries);

    @POST("api/import-json/")
    Call<Map<String, Object>> importJson(@Body List<Map<String, Object>> entries);

    @POST("api/external/fetch-imdb/")
    Call<Map<String, Object>> fetchImdbData(@Body Map<String, String> body);

    @POST("api/external/fetch-discogs/")
    Call<Map<String, Object>> fetchDiscogsData(@Body Map<String, String> body);

    @POST("api/external/fetch-wikipedia/")
    Call<Map<String, Object>> fetchWikipediaData(@Body Map<String, String> body);

    @POST("api/external/fetch-books/")
    Call<Map<String, Object>> fetchBooksData(@Body Map<String, String> body);

    @POST("api/shares/")
    Call<Map<String, Object>> createShare(@Body Map<String, Object> body);

    @GET("api/shares/{token}/")
    Call<Map<String, Object>> getShare(@Path("token") String token);

    @POST("api/shares/import/")
    Call<Map<String, Object>> importShare(@Body Map<String, String> body);

    @POST("api/connections/generate/")
    Call<Map<String, Object>> generateConnections();

    @GET("api/connections/{entryId}/")
    Call<ConnectionResponse> getConnections(@Path("entryId") int entryId);

    @GET("api/random/")
    Call<Entry> getRandomEntry();

    @GET("api/stremio/{entryId}/")
    Call<Map<String, Object>> getStremioLink(@Path("entryId") int entryId);
}