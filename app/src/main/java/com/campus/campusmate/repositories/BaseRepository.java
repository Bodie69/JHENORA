package com.campus.campusmate.repositories;

import android.content.Context;
import android.text.TextUtils;

import com.campus.campusmate.api.SupabaseApi;
import com.campus.campusmate.api.SupabaseAuthApi;
import com.campus.campusmate.models.AuditLog;
import com.campus.campusmate.models.FeedFilter;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.utils.SupabaseManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import timber.log.Timber;

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
                    result.onError(extractErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                result.onError(t.getMessage());
            }
        });
    }

    /**
     * Pulls the human-readable reason out of a failed response body instead of just its HTTP
     * code, so callers (and the user-facing error message) reflect what Supabase actually
     * rejected — e.g. a password-policy violation — rather than a generic "Request failed: 400"
     * that could mean anything.
     * <p>
     * Supabase Auth (GoTrue) error bodies use {@code msg}/{@code error_description}/{@code error};
     * PostgREST (the data API) uses {@code message}. Falls back to the HTTP code if the body is
     * missing, empty, or not JSON in one of those shapes.
     */
    private static String extractErrorMessage(Response<?> response) {
        String fallback = "Request failed: " + response.code();
        if (response.errorBody() == null) return fallback;
        try {
            String body = response.errorBody().string();
            if (body.isEmpty()) return fallback;
            JSONObject json = new JSONObject(body);
            for (String key : new String[]{"msg", "error_description", "message", "error"}) {
                if (json.isNull(key)) continue;
                String value = json.optString(key, "");
                if (!value.isEmpty()) return value;
            }
            return fallback;
        } catch (Exception e) {
            Timber.w(e, "Could not parse error body for %s", fallback);
            return fallback;
        }
    }

    /**
     * Adds a feed's scope and search (plus any caller-specific {@code extra} conditions) to a
     * PostgREST filter map. Everything is ANDed through a single {@code and=(...)} parameter,
     * which also absorbs an existing audience {@code or=(...)}: otherwise the scope's
     * {@code visibility}/{@code department} conditions would overwrite the audience rule's keys.
     *
     * @param searchColumns columns matched case-insensitively against {@link FeedFilter#query}
     */
    protected static void applyFeedFilter(Map<String, String> filter, FeedFilter feed,
                                          List<String> extra, String... searchColumns) {
        List<String> conditions = new ArrayList<>();
        String audience = filter.remove("or");
        if (audience != null) conditions.add("or" + audience);

        if (FeedFilter.SCOPE_GENERAL.equals(feed.scope)) {
            conditions.add("visibility.eq." + Constants.VISIBILITY_GENERAL);
        } else if (feed.isDepartmentScope()) {
            // General posts also carry their author's department, so match on scope as well.
            conditions.add("visibility.eq.department");
            conditions.add("department.eq." + quoteFilterValue(feed.scope));
        }

        String search = sanitizeSearch(feed.query);
        if (!search.isEmpty() && searchColumns.length > 0) {
            String pattern = quoteFilterValue("*" + search + "*");
            List<String> anyColumn = new ArrayList<>();
            for (String column : searchColumns) anyColumn.add(column + ".ilike." + pattern);
            conditions.add("or(" + TextUtils.join(",", anyColumn) + ")");
        }

        conditions.addAll(extra);
        if (!conditions.isEmpty()) filter.put("and", "(" + TextUtils.join(",", conditions) + ")");
    }

    /**
     * Strips characters that would change the meaning of a quoted ilike pattern: quote and
     * backslash (string syntax) and the {@code *}/{@code %} wildcards, so typed text is matched
     * literally. Runs of whitespace collapse to one space.
     */
    private static String sanitizeSearch(String query) {
        return query.replaceAll("[\"\\\\*%]", " ").replaceAll("\\s+", " ").trim();
    }

    /** Double-quotes a value so PostgREST's reserved characters ({@code ,.:()} and spaces) are literal. */
    private static String quoteFilterValue(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    protected interface OnResult<T> {
        void onSuccess(T body);
        void onError(String message);
    }

    /** Callback for a single infinite-scroll page fetch (see {@code fetch*Page} repository methods). */
    public interface OnPageListener<T> {
        void onLoaded(List<T> page);
        void onError(String message);
    }

    /**
     * Fetches a single Range-header'd page of {@code T} via {@code fetcher}.
     * Implemented per-repository since each table's Retrofit call has a different signature
     * (different filter/response types), but the Range string itself is always "offset-end".
     */
    protected interface PageFetcher<T> {
        void fetch(String range, OnResult<List<T>> callback);
    }

    /**
     * Loads an entire table/query result by walking it {@link Constants#BULK_FETCH_CHUNK_SIZE}
     * rows at a time via {@code fetcher}, instead of one unbounded request.
     * <p>
     * This exists so screens that legitimately need "the whole list" (dashboard stat counts,
     * the live-display rotation, client-side search/filter chips) don't do it by asking
     * PostgREST for every row in a single response — which gets slower and heavier as the
     * table grows, and can silently truncate once a table exceeds PostgREST's row cap. The
     * public contract (one {@link OnResult} callback with the complete, combined list) is
     * unchanged, so callers don't need to know it happened in chunks.
     * <p>
     * For UI-driven infinite scroll (only fetch more as the user actually scrolls), use the
     * repository's dedicated page-fetch method instead — this helper always walks to the end.
     */
    protected <T> void fetchAllPages(PageFetcher<T> fetcher, OnResult<List<T>> onComplete) {
        fetchNextChunk(fetcher, 0, new ArrayList<>(), onComplete);
    }

    private <T> void fetchNextChunk(PageFetcher<T> fetcher, int offset, List<T> accumulated,
                                     OnResult<List<T>> onComplete) {
        int chunkSize = Constants.BULK_FETCH_CHUNK_SIZE;
        String range = offset + "-" + (offset + chunkSize - 1);
        fetcher.fetch(range, new OnResult<List<T>>() {
            @Override
            public void onSuccess(List<T> page) {
                if (page != null) accumulated.addAll(page);
                if (page == null || page.size() < chunkSize) {
                    onComplete.onSuccess(accumulated);
                } else {
                    fetchNextChunk(fetcher, offset + chunkSize, accumulated, onComplete);
                }
            }

            @Override
            public void onError(String message) {
                // A later chunk failing shouldn't discard the rows already fetched.
                if (accumulated.isEmpty()) {
                    onComplete.onError(message);
                } else {
                    onComplete.onSuccess(accumulated);
                }
            }
        });
    }
}
