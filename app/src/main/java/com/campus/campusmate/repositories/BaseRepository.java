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
}
