package com.campus.campusmate.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.campus.campusmate.utils.Constants;

import java.util.List;

/** Room access to {@link CachedPost}. Every method blocks, so call them off the main thread. */
@Dao
public abstract class CachedPostDao {

    /**
     * Posts of {@code kind} that a user of {@code department} may see: every General post plus
     * their own department's. A null department matches General posts only, the same audience
     * rule the repositories send to Supabase.
     */
    @Query("SELECT * FROM cached_posts WHERE kind = :kind AND (visibility = '"
            + Constants.VISIBILITY_GENERAL + "' OR department = :department)")
    public abstract List<CachedPost> getVisibleTo(String kind, String department);

    /** Every cached post of {@code kind}, for the campus-wide Live Display. */
    @Query("SELECT * FROM cached_posts WHERE kind = :kind")
    public abstract List<CachedPost> getAll(String kind);

    @Query("SELECT * FROM cached_posts WHERE kind = :kind AND id = :id")
    public abstract CachedPost find(String kind, String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void upsert(List<CachedPost> posts);

    @Query("DELETE FROM cached_posts WHERE kind = :kind AND id = :id")
    public abstract void delete(String kind, String id);

    @Query("DELETE FROM cached_posts WHERE kind = :kind")
    public abstract void deleteKind(String kind);

    @Query("DELETE FROM cached_posts")
    public abstract void deleteAll();

    /** Makes {@code posts} the complete cached set of {@code kind}, dropping everything else. */
    @Transaction
    public void replaceKind(String kind, List<CachedPost> posts) {
        deleteKind(kind);
        upsert(posts);
    }
}
