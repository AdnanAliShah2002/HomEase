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
    version = 12,
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

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_requests_status ON service_requests(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_requests_customerPhone ON service_requests(customerPhone)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_requests_selectedProviderPhone ON service_requests(selectedProviderPhone)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_requests_remoteId ON service_requests(remoteId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_job_offers_requestId ON job_offers(requestId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_job_offers_providerPhone ON job_offers(providerPhone)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_job_offers_status ON job_offers(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_job_messages_jobId ON job_messages(jobId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_job_messages_createdAtEpochMs ON job_messages(createdAtEpochMs)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_call_logs_jobId ON call_logs(jobId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "homease_database"
                )
                .addMigrations(MIGRATION_10_11, MIGRATION_11_12)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
