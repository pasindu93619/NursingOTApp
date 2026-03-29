package com.pasindu.nursingotapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pasindu.nursingotapp.data.local.dao.ClaimPeriodDao
import com.pasindu.nursingotapp.data.local.dao.DailyEntryDao
import com.pasindu.nursingotapp.data.local.dao.ProfileDao
import com.pasindu.nursingotapp.data.local.entity.ClaimPeriodEntity
import com.pasindu.nursingotapp.data.local.entity.DailyEntryEntity
import com.pasindu.nursingotapp.data.local.entity.ProfileEntity

@Database(
    entities = [
        ProfileEntity::class,
        ClaimPeriodEntity::class,
        DailyEntryEntity::class
    ],
    version = 2, // <--- VERSION INCREASED TO 2 HERE
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao

    abstract fun claimPeriodDao(): ClaimPeriodDao

    abstract fun dailyEntryDao(): DailyEntryDao
}