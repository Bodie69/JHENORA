package com.campus.campusmate.repositories;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.api.SupabaseApi;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.AuditLog;
import com.campus.campusmate.utils.SupabaseManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository class responsible for managing Announcement data.
 * Handles fetching, creating, updating, approving, and rejecting announcements via Supabase API.
 */
public class AnnouncementRepository extends BaseRepository {

    private static AnnouncementRepository instance;

    private AnnouncementRepository() {}

    public static synchronized AnnouncementRepository getInstance() {
        if (instance == null) instance = new AnnouncementRepository();
        return instance;
    }

    @Override
    public void init(Context context) {
        super.init(context);
    }

    // --- Create / Update / Delete ---

    public void createAnnouncement(Announcement announcement, OnCompleteListener listener) {
        String id = UUID.randomUUID().toString();
        announcement.setId(id);
        dataApi.insertAnnouncement(announcement, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    logAudit(announcement.getAuthorId(), announcement.getAuthorName(),
                            AuditLog.ACTION_CREATE, AuditLog.TARGET_ANNOUNCEMENT, id,
                            "Announcement created: " + announcement.getTitle());
                    listener.onSuccess(id);
                } else {
                    listener.onError("Failed to create announcement");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void updateAnnouncement(Announcement announcement, OnCompleteListener listener) {
        announcement.setUpdatedAt(System.currentTimeMillis());
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + announcement.getId());
        Map<String, Object> fields = buildAnnouncementPatch(announcement);
        dataApi.patchAnnouncement(filter, fields, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) listener.onSuccess(announcement.getId());
                else listener.onError("Update failed");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void deleteAnnouncement(String id, String userId, String userName, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        dataApi.deleteAnnouncement(filter).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    logAudit(userId, userName, AuditLog.ACTION_DELETE,
                            AuditLog.TARGET_ANNOUNCEMENT, id, "Announcement deleted");
                    listener.onSuccess();
                } else {
                    listener.onError("Delete failed");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void approveAnnouncement(String id, String adminId, String adminName, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", Announcement.STATUS_APPROVED);
        fields.put("approved_by", adminName);
        fields.put("approved_at", System.currentTimeMillis());
        fields.put("updated_at", System.currentTimeMillis());
        dataApi.patchAnnouncement(filter, fields, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    logAudit(adminId, adminName, AuditLog.ACTION_APPROVE,
                            AuditLog.TARGET_ANNOUNCEMENT, id, "Announcement approved");
                    listener.onSuccess();
                } else {
                    listener.onError("Approval failed");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void rejectAnnouncement(String id, String adminId, String adminName,
                                   String reason, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", Announcement.STATUS_REJECTED);
        fields.put("rejection_reason", reason);
        fields.put("updated_at", System.currentTimeMillis());
        dataApi.patchAnnouncement(filter, fields, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    logAudit(adminId, adminName, AuditLog.ACTION_REJECT,
                            AuditLog.TARGET_ANNOUNCEMENT, id, "Announcement rejected: " + reason);
                    listener.onSuccess();
                } else {
                    listener.onError("Rejection failed");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    // --- Queries ---

    public void observeApprovedAnnouncements(String userDept, MutableLiveData<List<Announcement>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("status", "eq." + Announcement.STATUS_APPROVED);
        if (userDept != null && !userDept.isEmpty()) {
            filter.put("or", "(visibility.eq.general,department.eq." + userDept + ")");
        } else {
            filter.put("visibility", "eq.general");
        }
        dataApi.getAnnouncements(filter, null).enqueue(new Callback<List<Announcement>>() {
            @Override
            public void onResponse(Call<List<Announcement>> call, Response<List<Announcement>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Announcement> list = response.body();
                    Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    liveData.postValue(list);
                } else {
                    liveData.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<Announcement>> call, Throwable t) {
                liveData.postValue(null);
            }
        });
    }

    public void observePendingAnnouncements(MutableLiveData<List<Announcement>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("status", "eq." + Announcement.STATUS_PENDING);
        filter.put("order", "created_at.asc");
        dataApi.getAnnouncements(filter, null).enqueue(new Callback<List<Announcement>>() {
            @Override
            public void onResponse(Call<List<Announcement>> call, Response<List<Announcement>> response) {
                liveData.postValue(response.isSuccessful() ? response.body() : null);
            }

            @Override
            public void onFailure(Call<List<Announcement>> call, Throwable t) {
                liveData.postValue(null);
            }
        });
    }

    public void observeUserAnnouncements(String userId, MutableLiveData<List<Announcement>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("author_id", "eq." + userId);
        filter.put("order", "created_at.desc");
        dataApi.getAnnouncements(filter, null).enqueue(new Callback<List<Announcement>>() {
            @Override
            public void onResponse(Call<List<Announcement>> call, Response<List<Announcement>> response) {
                liveData.postValue(response.isSuccessful() ? response.body() : null);
            }

            @Override
            public void onFailure(Call<List<Announcement>> call, Throwable t) {
                liveData.postValue(null);
            }
        });
    }

    public void observeAllAnnouncements(MutableLiveData<List<Announcement>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("order", "created_at.desc");
        dataApi.getAnnouncements(filter, null).enqueue(new Callback<List<Announcement>>() {
            @Override
            public void onResponse(Call<List<Announcement>> call, Response<List<Announcement>> response) {
                liveData.postValue(response.isSuccessful() ? response.body() : null);
            }

            @Override
            public void onFailure(Call<List<Announcement>> call, Throwable t) {
                liveData.postValue(null);
            }
        });
    }

    public void fetchAnnouncementById(String id, OnAnnouncementFetchedListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        dataApi.getAnnouncements(filter, null).enqueue(new Callback<List<Announcement>>() {
            @Override
            public void onResponse(Call<List<Announcement>> call, Response<List<Announcement>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    listener.onFetched(response.body().get(0));
                } else {
                    listener.onFetched(null);
                }
            }

            @Override
            public void onFailure(Call<List<Announcement>> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }


    private Map<String, Object> buildAnnouncementPatch(Announcement a) {
        Map<String, Object> m = new HashMap<>();
        m.put("title", a.getTitle());
        m.put("what", a.getWhat());
        m.put("who", a.getWho());
        m.put("when_info", a.getWhen());
        m.put("where_info", a.getWhere());
        m.put("why", a.getWhy());
        m.put("additional_info", a.getAdditionalInfo());
        m.put("image_url", a.getImageUrl());
        m.put("visibility", a.getVisibility());
        m.put("department", a.getDepartment());
        m.put("priority", a.getPriority());
        m.put("category", a.getCategory());
        m.put("status", a.getStatus());
        m.put("updated_at", a.getUpdatedAt());
        return m;
    }

    public interface OnCompleteListener {
        void onSuccess(String id);
        void onError(String message);
    }

    public interface OnSimpleListener {
        void onSuccess();
        void onError(String message);
    }

    public interface OnAnnouncementFetchedListener {
        void onFetched(Announcement announcement);
        void onError(String message);
    }
}
