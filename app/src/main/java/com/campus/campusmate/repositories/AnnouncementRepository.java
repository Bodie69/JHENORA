package com.campus.campusmate.repositories;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.AuditLog;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        enqueue(dataApi.insertAnnouncement(announcement, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                logAudit(announcement.getAuthorId(), announcement.getAuthorName(),
                        AuditLog.ACTION_CREATE, AuditLog.TARGET_ANNOUNCEMENT, id,
                        "Announcement created: " + announcement.getTitle());
                listener.onSuccess(id);
            }

            @Override
            public void onError(String message) {
                listener.onError("Failed to create announcement");
            }
        });
    }

    public void updateAnnouncement(Announcement announcement, OnCompleteListener listener) {
        announcement.setUpdatedAt(System.currentTimeMillis());
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + announcement.getId());
        Map<String, Object> fields = buildAnnouncementPatch(announcement);
        enqueue(dataApi.patchAnnouncement(filter, fields, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                listener.onSuccess(announcement.getId());
            }

            @Override
            public void onError(String message) {
                listener.onError("Update failed");
            }
        });
    }

    public void deleteAnnouncement(String id, String userId, String userName, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        enqueue(dataApi.deleteAnnouncement(filter), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                logAudit(userId, userName, AuditLog.ACTION_DELETE,
                        AuditLog.TARGET_ANNOUNCEMENT, id, "Announcement deleted");
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Delete failed");
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
        enqueue(dataApi.patchAnnouncement(filter, fields, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                logAudit(adminId, adminName, AuditLog.ACTION_APPROVE,
                        AuditLog.TARGET_ANNOUNCEMENT, id, "Announcement approved");
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Approval failed");
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
        enqueue(dataApi.patchAnnouncement(filter, fields, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                logAudit(adminId, adminName, AuditLog.ACTION_REJECT,
                        AuditLog.TARGET_ANNOUNCEMENT, id, "Announcement rejected: " + reason);
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Rejection failed");
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
        enqueue(dataApi.getAnnouncements(filter, null), new OnResult<List<Announcement>>() {
            @Override
            public void onSuccess(List<Announcement> body) {
                if (body != null) {
                    Collections.sort(body, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                }
                liveData.postValue(body);
            }

            @Override
            public void onError(String message) {
                liveData.postValue(null);
            }
        });
    }

    public void observePendingAnnouncements(MutableLiveData<List<Announcement>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("status", "eq." + Announcement.STATUS_PENDING);
        filter.put("order", "created_at.asc");
        enqueue(dataApi.getAnnouncements(filter, null), new OnResult<List<Announcement>>() {
            @Override
            public void onSuccess(List<Announcement> body) {
                liveData.postValue(body);
            }

            @Override
            public void onError(String message) {
                liveData.postValue(null);
            }
        });
    }

    public void observeUserAnnouncements(String userId, MutableLiveData<List<Announcement>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("author_id", "eq." + userId);
        filter.put("order", "created_at.desc");
        enqueue(dataApi.getAnnouncements(filter, null), new OnResult<List<Announcement>>() {
            @Override
            public void onSuccess(List<Announcement> body) {
                liveData.postValue(body);
            }

            @Override
            public void onError(String message) {
                liveData.postValue(null);
            }
        });
    }

    public void observeAllAnnouncements(MutableLiveData<List<Announcement>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("order", "created_at.desc");
        enqueue(dataApi.getAnnouncements(filter, null), new OnResult<List<Announcement>>() {
            @Override
            public void onSuccess(List<Announcement> body) {
                liveData.postValue(body);
            }

            @Override
            public void onError(String message) {
                liveData.postValue(null);
            }
        });
    }

    public void fetchAnnouncementById(String id, OnAnnouncementFetchedListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        enqueue(dataApi.getAnnouncements(filter, null), new OnResult<List<Announcement>>() {
            @Override
            public void onSuccess(List<Announcement> body) {
                if (body != null && !body.isEmpty()) {
                    listener.onFetched(body.get(0));
                } else {
                    listener.onFetched(null);
                }
            }

            @Override
            public void onError(String message) {
                listener.onError(message);
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
