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
    version = 7,
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "homease_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
