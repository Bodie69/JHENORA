package com.campus.campusmate.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

/**
 * One approved announcement or event saved for offline reading.
 * <p>
 * The post itself is stored as JSON in {@link #json} rather than as one column per field, so
 * adding a field to {@code Announcement}/{@code Event} never needs a database migration. Only
 * the audience columns ({@link #visibility}, {@link #department}) are broken out, because the
 * cache is queried by them.
 */
@Entity(tableName = "cached_posts", primaryKeys = {"kind", "id"})
public class CachedPost {

    public static final String KIND_ANNOUNCEMENT = "announcement";
    public static final String KIND_EVENT = "event";

    /** {@link #KIND_ANNOUNCEMENT} or {@link #KIND_EVENT}. */
    @NonNull
    public String kind;

    @NonNull
    public String id;

    public String visibility;

    public String department;

    /** The full post, serialized with Gson. */
    @NonNull
    @ColumnInfo(name = "json")
    public String json;

    public CachedPost(@NonNull String kind, @NonNull String id, String visibility,
                      String department, @NonNull String json) {
        this.kind = kind;
        this.id = id;
        this.visibility = visibility;
        this.department = department;
        this.json = json;
    }
}
