package com.example.bcapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {BcEntity.class},
        version = 3,                  // ⬅️ INCREASE VERSION
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract BcDao bcDao();

    private static volatile AppDatabase INSTANCE;

    // 🔹 Migration 1 → 2 : afterTakenAmount
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE bc_table " +
                    "ADD COLUMN afterTakenAmount REAL NOT NULL DEFAULT 0.0"
            );
        }
    };

    // 🔹 Migration 2 → 3 : paidAmount (Map<String, Double>)
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE bc_table " +
                    "ADD COLUMN paidAmount TEXT"
            );
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "bc_database"
                            )
                            // ⬅️ ADD ALL MIGRATIONS IN ORDER
                            .addMigrations(
                                    MIGRATION_1_2,
                                    MIGRATION_2_3
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
