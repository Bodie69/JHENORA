package com.campus.campusmate.repositories;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.api.SupabaseApi;
import com.campus.campusmate.models.AuditLog;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.utils.SupabaseManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventRepository extends BaseRepository {

    private static EventRepository instance;

    private EventRepository() {}

    public static synchronized EventRepository getInstance() {
        if (instance == null) instance = new EventRepository();
        return instance;
    }

    @Override
    public void init(Context context) {
        super.init(context);
    }

    public void createEvent(Event event, OnCompleteListener listener) {
        String id = UUID.randomUUID().toString();
        event.setId(id);
        dataApi.insertEvent(event, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    logAudit(event.getAuthorId(), event.getAuthorName(),
                            AuditLog.ACTION_CREATE, AuditLog.TARGET_EVENT,
                            id, "Event created: " + event.getTitle());
                    listener.onSuccess(id);
                } else {
                    listener.onError("Failed to create event");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void updateEvent(Event event, OnCompleteListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + event.getId());
        Map<String, Object> fields = buildEventPatch(event);
        dataApi.patchEvent(filter, fields, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) listener.onSuccess(event.getId());
                else listener.onError("Update failed");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void deleteEvent(String id, String userId, String userName, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        dataApi.deleteEvent(filter).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    logAudit(userId, userName, AuditLog.ACTION_DELETE,
                            AuditLog.TARGET_EVENT, id, "Event deleted");
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

    public void approveEvent(String id, String adminId, String adminName, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", Event.STATUS_APPROVED);
        fields.put("approved_by", adminName);
        fields.put("approved_at", System.currentTimeMillis());
        fields.put("updated_at", System.currentTimeMillis());
        dataApi.patchEvent(filter, fields, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    logAudit(adminId, adminName, AuditLog.ACTION_APPROVE,
                            AuditLog.TARGET_EVENT, id, "Event approved");
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

    public void rejectEvent(String id, String adminId, String adminName,
                            String reason, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", Event.STATUS_REJECTED);
        fields.put("rejection_reason", reason);
        fields.put("updated_at", System.currentTimeMillis());
        dataApi.patchEvent(filter, fields, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    logAudit(adminId, adminName, AuditLog.ACTION_REJECT,
                            AuditLog.TARGET_EVENT, id, "Event rejected: " + reason);
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

    public void observeApprovedEvents(String userDept, MutableLiveData<List<Event>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("status", "eq." + Event.STATUS_APPROVED);
        if (userDept != null && !userDept.isEmpty()) {
            filter.put("or", "(visibility.eq.general,department.eq." + userDept + ")");
        } else {
            filter.put("visibility", "eq.general");
        }
        filter.put("order", "start_date.asc");
        dataApi.getEvents(filter, null).enqueue(new Callback<List<Event>>() {
            @Override
            public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                liveData.postValue(response.isSuccessful() ? response.body() : null);
            }

            @Override
            public void onFailure(Call<List<Event>> call, Throwable t) {
                liveData.postValue(null);
            }
        });
    }

    public void observePendingEvents(MutableLiveData<List<Event>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("status", "eq." + Event.STATUS_PENDING);
        filter.put("order", "created_at.asc");
        dataApi.getEvents(filter, null).enqueue(new Callback<List<Event>>() {
            @Override
            public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                liveData.postValue(response.isSuccessful() ? response.body() : null);
            }

            @Override
            public void onFailure(Call<List<Event>> call, Throwable t) {
                liveData.postValue(null);
            }
        });
    }

    public void observeUserEvents(String userId, MutableLiveData<List<Event>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("author_id", "eq." + userId);
        filter.put("order", "created_at.desc");
        dataApi.getEvents(filter, null).enqueue(new Callback<List<Event>>() {
            @Override
            public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                liveData.postValue(response.isSuccessful() ? response.body() : null);
            }

            @Override
            public void onFailure(Call<List<Event>> call, Throwable t) {
                liveData.postValue(null);
            }
        });
    }

    public void fetchEventById(String id, OnEventFetchedListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        dataApi.getEvents(filter, null).enqueue(new Callback<List<Event>>() {
            @Override
            public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    listener.onFetched(response.body().get(0));
                } else {
                    listener.onFetched(null);
                }
            }

            @Override
            public void onFailure(Call<List<Event>> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void observeAllEvents(MutableLiveData<List<Event>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("order", "created_at.desc");
        dataApi.getEvents(filter, null).enqueue(new Callback<List<Event>>() {
            @Override
            public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                liveData.postValue(response.isSuccessful() ? response.body() : null);
            }

            @Override
            public void onFailure(Call<List<Event>> call, Throwable t) {
                liveData.postValue(null);
            }
        });
    }


    private Map<String, Object> buildEventPatch(Event e) {
        Map<String, Object> m = new HashMap<>();
        m.put("title", e.getTitle());
        m.put("what", e.getWhat());
        m.put("who", e.getWho());
        m.put("when_info", e.getWhen());
        m.put("where_info", e.getWhere());
        m.put("why", e.getWhy());
        m.put("registration_link", e.getRegistrationLink());
        m.put("image_url", e.getImageUrl());
        m.put("visibility", e.getVisibility());
        m.put("department", e.getDepartment());
        m.put("status", e.getStatus());
        m.put("start_date", e.getStartDate());
        m.put("end_date", e.getEndDate());
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

    public interface OnEventFetchedListener {
        void onFetched(Event event);
        void onError(String message);
    }
}
