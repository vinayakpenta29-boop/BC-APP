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
        version = 7,   // 🔥 UPDATED VERSION (6 → 7)
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

    // 🔹 Migration 2 → 3 : paidAmount
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE bc_table " +
                    "ADD COLUMN paidAmount TEXT"
            );
        }
    };

    // 🔹 Migration 3 → 4 : payments
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE bc_table " +
                    "ADD COLUMN payments TEXT"
            );
        }
    };

    // 🔹 Migration 4 → 5 : paidBcAmount
    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE bc_table " +
                    "ADD COLUMN paidBcAmount TEXT"
            );
        }
    };

    // 🔹 Migration 5 → 6 : Receive Amount
    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE bc_table " +
                    "ADD COLUMN receiveAmounts TEXT"
            );
            database.execSQL(
                    "ALTER TABLE bc_table " +
                    "ADD COLUMN isReceiveAmountFixed INTEGER NOT NULL DEFAULT 1"
            );
        }
    };

    // 🔥 NEW MIGRATION 6 → 7 : Weekly Support
    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE bc_table " +
                    "ADD COLUMN isWeekly INTEGER NOT NULL DEFAULT 0"
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
                                    MIGRATION_4_5,
                                    MIGRATION_5_6,
                                    MIGRATION_6_7   // 🔥 ADDED
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
