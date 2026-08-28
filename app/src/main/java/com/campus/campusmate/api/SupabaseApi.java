package com.campus.campusmate.api;

import com.campus.campusmate.api.dto.CreateAccountResponse;
import com.campus.campusmate.api.dto.CreateDisplayAccountRequest;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.AuditLog;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.models.Organization;
import com.campus.campusmate.models.User;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

public interface SupabaseApi {

    // Announcements
    @GET("rest/v1/announcements")
    Call<List<Announcement>> getAnnouncements(
            @QueryMap Map<String, String> filters,
            @Header("Range") String range);

    @POST("rest/v1/announcements")
    Call<Void> insertAnnouncement(
            @Body Announcement announcement,
            @Header("Prefer") String prefer);

    @PATCH("rest/v1/announcements")
    Call<Void> patchAnnouncement(
            @QueryMap Map<String, String> filters,
            @Body Map<String, Object> fields,
            @Header("Prefer") String prefer);

    @DELETE("rest/v1/announcements")
    Call<Void> deleteAnnouncement(@QueryMap Map<String, String> filters);

    // Events
    @GET("rest/v1/events")
    Call<List<Event>> getEvents(
            @QueryMap Map<String, String> filters,
            @Header("Range") String range);

    @POST("rest/v1/events")
    Call<Void> insertEvent(
            @Body Event event,
            @Header("Prefer") String prefer);

    @PATCH("rest/v1/events")
    Call<Void> patchEvent(
            @QueryMap Map<String, String> filters,
            @Body Map<String, Object> fields,
            @Header("Prefer") String prefer);

    @DELETE("rest/v1/events")
    Call<Void> deleteEvent(@QueryMap Map<String, String> filters);

    // Users
    @GET("rest/v1/users")
    Call<List<User>> getUsers(@QueryMap Map<String, String> filters);

    @POST("rest/v1/users")
    Call<Void> insertUser(
            @Body User user,
            @Header("Prefer") String prefer);

    @POST("rest/v1/users")
    Call<Void> insertUserMap(
            @Body Map<String, Object> userData,
            @Header("Prefer") String prefer);

    @PATCH("rest/v1/users")
    Call<Void> patchUser(
            @QueryMap Map<String, String> filters,
            @Body Map<String, Object> fields,
            @Header("Prefer") String prefer);

    @PATCH("rest/v1/users")
    Call<Void> patchUserMap(
            @QueryMap Map<String, String> filters,
            @Body Map<String, Object> fields,
            @Header("Prefer") String prefer);

    // Organizations
    @GET("rest/v1/organizations")
    Call<List<Organization>> getOrganizations(@QueryMap Map<String, String> filters);

    @POST("rest/v1/organizations")
    Call<Void> insertOrganization(
            @Body Organization organization,
            @Header("Prefer") String prefer);

    @PATCH("rest/v1/organizations")
    Call<Void> patchOrganization(
            @QueryMap Map<String, String> filters,
            @Body Map<String, Object> fields,
            @Header("Prefer") String prefer);

    @DELETE("rest/v1/organizations")
    Call<Void> deleteOrganization(@QueryMap Map<String, String> filters);

    // Audit logs
    @POST("rest/v1/audit_logs")
    Call<Void> insertAuditLog(
            @Body AuditLog log,
            @Header("Prefer") String prefer);

    // Edge Functions
    @POST("functions/v1/create-display-account")
    Call<CreateAccountResponse> createDisplayAccount(@Body CreateDisplayAccountRequest request);
}
