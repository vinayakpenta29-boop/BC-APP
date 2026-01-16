package com.example.bcapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {BcEntity.class},
        version = 5, // ✅ INCREASE VERSION
        exportSchema = false
)
@TypeConverters({Converters.class})
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

    // 🔹 Migration 3 → 4 : payments (List<PaymentEntry>)
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE bc_table " +
                    "ADD COLUMN payments TEXT"
            );
        }
    };

    // 🔹 Migration 4 → 5 : paidBcAmount (Paid BC per member)
    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE bc_table " +
                    "ADD COLUMN paidBcAmount TEXT"
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
                            .addMigrations(
                                    MIGRATION_1_2,
                                    MIGRATION_2_3,
                                    MIGRATION_3_4,
                                    MIGRATION_4_5
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
