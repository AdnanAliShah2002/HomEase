package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        ServiceRequestEntity::class,
        JobOfferEntity::class,
        JobRatingEntity::class,
        ServiceCategoryEntity::class,
        ProviderLocationEntity::class,
        JobMessageEntity::class,
        CallLogEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun serviceRequestDao(): ServiceRequestDao
    abstract fun jobOfferDao(): JobOfferDao
    abstract fun jobRatingDao(): JobRatingDao
    abstract fun serviceCategoryDao(): ServiceCategoryDao
    abstract fun providerLocationDao(): ProviderLocationDao
    abstract fun jobMessageDao(): JobMessageDao
    abstract fun callLogDao(): CallLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE service_requests ADD COLUMN cancelledBy TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE service_requests ADD COLUMN cancellationReason TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE service_requests ADD COLUMN cancelledAt INTEGER DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "homease_database"
                )
                .addMigrations(MIGRATION_10_11)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
