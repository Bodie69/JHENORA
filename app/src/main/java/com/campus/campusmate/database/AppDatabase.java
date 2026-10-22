package com.campus.campusmate.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * The app's local Room database. It only holds a disposable copy of server data, so a schema
 * change simply drops and rebuilds it; the next sync refills it.
 */
@Database(entities = {CachedPost.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String NAME = "jhenora_cache.db";

    private static volatile AppDatabase instance;

    public abstract CachedPostDao cachedPostDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, NAME)
                            .fallbackToDestructiveMigration(true)
                            .build();
                }
            }
        }
        return instance;
    }
}
