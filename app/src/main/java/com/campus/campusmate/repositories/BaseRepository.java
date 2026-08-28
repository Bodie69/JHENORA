package com.campus.campusmate.repositories;

import android.content.Context;

import com.campus.campusmate.api.SupabaseApi;
import com.campus.campusmate.api.SupabaseAuthApi;
import com.campus.campusmate.models.AuditLog;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.utils.SupabaseManager;

import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Base Repository class to centralize common logic like API access and audit logging.
 */
public abstract class BaseRepository {

    protected SupabaseAuthApi authApi;
    protected SupabaseApi dataApi;
    protected SessionManager session;

    public void init(Context context) {
        SupabaseManager manager = SupabaseManager.getInstance(context);
        authApi = manager.authApi();
        dataApi = manager.dataApi();
        session = new SessionManager(context);
    }

    /**
     * Logs an action to the audit_logs table.
     */
    protected void logAudit(String userId, String userEmail, String action, String targetType, String targetId, String details) {
        if (dataApi == null) return;

        AuditLog log = new AuditLog(userId, userEmail, action, targetType, targetId, details);
        log.setId(UUID.randomUUID().toString());
        dataApi.insertAuditLog(log, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Background logging, no listener needed usually
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Fail silently for audit logs
            }
        });
    }

    /**
     * Enqueues a Retrofit call and routes its outcome through {@link OnResult}, collapsing
     * the repeated isSuccessful/body-null branching every repository was hand-rolling per call.
     */
    protected <T> void enqueue(Call<T> call, OnResult<T> result) {
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful()) {
                    result.onSuccess(response.body());
                } else {
                    result.onError("Request failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                result.onError(t.getMessage());
            }
        });
    }

    protected interface OnResult<T> {
        void onSuccess(T body);
        void onError(String message);
    }
}
