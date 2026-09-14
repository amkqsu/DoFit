package com.dofit.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(primaryKeys = ["habitId", "day"])
data class HabitCheck(val habitId: Long, val day: String, val done: Boolean)

@Entity
data class HealthEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val value: Double,
    val unit: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface DoFitDao {
    @Query("SELECT * FROM Habit ORDER BY createdAt DESC") fun habits(): Flow<List<Habit>>
    @Query("SELECT * FROM HabitCheck WHERE day=:day") fun checks(day: String): Flow<List<HabitCheck>>
    @Insert suspend fun addHabit(habit: Habit): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun setCheck(check: HabitCheck)
    @Query("DELETE FROM Habit WHERE id=:id") suspend fun deleteHabit(id: Long)
    @Query("DELETE FROM HabitCheck WHERE habitId=:id") suspend fun deleteChecks(id: Long)
    @Insert suspend fun addHealth(entry: HealthEntry)
    @Query("SELECT * FROM HealthEntry ORDER BY createdAt DESC LIMIT 50") fun health(): Flow<List<HealthEntry>>
    @Query("SELECT * FROM Habit") suspend fun allHabits(): List<Habit>
}

@Database(entities = [Habit::class, HabitCheck::class, HealthEntry::class], version = 1, exportSchema = false)
abstract class DoFitDatabase : RoomDatabase() {
    abstract fun dao(): DoFitDao
    companion object {
        @Volatile private var instance: DoFitDatabase? = null
        fun get(context: Context): DoFitDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, DoFitDatabase::class.java, "dofit.db").build().also { instance = it }
        }
    }
}
