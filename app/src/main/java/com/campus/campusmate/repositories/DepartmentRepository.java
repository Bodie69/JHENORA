package com.campus.campusmate.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.models.AuditLog;
import com.campus.campusmate.models.Department;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

/**
 * Reads the {@code departments} table and lets admins manage department branding (logos).
 * Writes are admin-only at the database layer (see supabase/add_department_logos.sql).
 * <p>
 * Also keeps an app-wide department → logo URL lookup so posts, profiles and the live display
 * (which only carry the department's name) can show its logo. The lookup is persisted to
 * SharedPreferences so logos are available immediately on startup and while offline; logo
 * URLs are public storage links, so nothing sensitive is stored.
 */
public class DepartmentRepository extends BaseRepository {

    private static final String PREFS = "campusmate_department_logos";
    private static final String KEY_LOGOS = "logos_by_department";

    private static DepartmentRepository instance;

    private SharedPreferences prefs;
    /** Upper-cased department name and code → logo URL. Replaced wholesale, never mutated. */
    private volatile Map<String, String> logoByDepartment = Collections.emptyMap();
    /** Emits whenever a fetch changes the logo lookup, so already-drawn labels can rebind. */
    private final MutableLiveData<Map<String, String>> logoUpdates = new MutableLiveData<>();

    private DepartmentRepository() {}

    public static synchronized DepartmentRepository getInstance() {
        if (instance == null) instance = new DepartmentRepository();
        return instance;
    }

    @Override
    public void init(Context context) {
        super.init(context);
        if (prefs == null) {
            prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            logoByDepartment = readCachedLogos();
        }
    }

    /**
     * Loads all departments ordered by code. The table is a handful of rows, so no paging.
     * Posts {@code null} on failure so observers can distinguish an error from an empty list.
     */
    public void observeDepartments(MutableLiveData<List<Department>> liveData) {
        fetchDepartments(new OnResult<List<Department>>() {
            @Override
            public void onSuccess(List<Department> body) {
                liveData.postValue(body);
            }

            @Override
            public void onError(String message) {
                liveData.postValue(null);
            }
        });
    }

    /** Re-reads departments in the background just to refresh the logo lookup. */
    public void refreshLogoCache() {
        fetchDepartments(new OnResult<List<Department>>() {
            @Override
            public void onSuccess(List<Department> body) {}

            @Override
            public void onError(String message) {
                Timber.w("Department logo refresh failed: %s", message);
            }
        });
    }

    /**
     * Fires after a department fetch changes the logo lookup (e.g. the first fetch after
     * install, or an admin uploading a new logo). Not fired for the cached copy read at startup.
     */
    public LiveData<Map<String, String>> getLogoUpdates() {
        return logoUpdates;
    }

    /**
     * Logo URL for a department, matched case-insensitively against its name or code (posts
     * and users store the name, e.g. "BSIT"). Returns {@code null} when it has no logo.
     */
    @Nullable
    public String getLogoUrl(@Nullable String department) {
        if (department == null || department.isEmpty()) return null;
        return logoByDepartment.get(department.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Sets (or clears, when {@code logoUrl} is {@code null}) a department's logo and records
     * the change in the audit log. Callers re-read departments afterwards (via
     * {@link #observeDepartments}), which also refreshes the logo lookup.
     */
    public void updateLogo(String departmentId, String logoUrl, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + departmentId);
        Map<String, Object> fields = new HashMap<>();
        // Empty string rather than null: the shared Gson instance drops null map values, which
        // would turn a "remove logo" PATCH into a no-op.
        fields.put("logo_url", logoUrl != null ? logoUrl : "");
        enqueue(dataApi.patchDepartment(filter, fields, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                logAudit(session.getUserId(), session.getUserName(), AuditLog.ACTION_UPDATE,
                        AuditLog.TARGET_DEPARTMENT, departmentId,
                        logoUrl != null ? "Department logo updated" : "Department logo removed");
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Failed to update department logo");
            }
        });
    }

    /** Fetches every department, updating the logo lookup before handing the rows on. */
    private void fetchDepartments(OnResult<List<Department>> result) {
        Map<String, String> filter = new HashMap<>();
        filter.put("order", "code.asc");
        enqueue(dataApi.getDepartments(filter), new OnResult<List<Department>>() {
            @Override
            public void onSuccess(List<Department> body) {
                if (body != null) cacheLogos(body);
                result.onSuccess(body);
            }

            @Override
            public void onError(String message) {
                result.onError(message);
            }
        });
    }

    private void cacheLogos(List<Department> departments) {
        Map<String, String> logos = new HashMap<>();
        for (Department d : departments) {
            String url = d.getLogoUrl();
            if (url == null || url.isEmpty()) continue;
            if (d.getName() != null) logos.put(d.getName().trim().toUpperCase(Locale.ROOT), url);
            if (d.getCode() != null) logos.put(d.getCode().trim().toUpperCase(Locale.ROOT), url);
        }
        if (logos.equals(logoByDepartment)) return;
        logoByDepartment = Collections.unmodifiableMap(logos);
        logoUpdates.postValue(logoByDepartment);
        if (prefs != null) {
            prefs.edit().putString(KEY_LOGOS, new JSONObject(logos).toString()).apply();
        }
    }

    private Map<String, String> readCachedLogos() {
        String json = prefs.getString(KEY_LOGOS, null);
        if (json == null) return Collections.emptyMap();
        try {
            JSONObject object = new JSONObject(json);
            Map<String, String> logos = new HashMap<>();
            for (Iterator<String> keys = object.keys(); keys.hasNext(); ) {
                String key = keys.next();
                logos.put(key, object.getString(key));
            }
            return Collections.unmodifiableMap(logos);
        } catch (JSONException e) {
            Timber.w(e, "Discarding unreadable department logo cache");
            return Collections.emptyMap();
        }
    }

    public interface OnSimpleListener {
        void onSuccess();
        void onError(String message);
    }
}
